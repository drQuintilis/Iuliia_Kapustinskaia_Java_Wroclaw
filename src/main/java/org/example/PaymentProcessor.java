package org.example;

import org.example.jsonClasses.Order;
import org.example.jsonClasses.PaymentMethod;
import java.util.stream.Collectors;

import java.util.*;

public class PaymentProcessor {
    private PaymentProcessor() {}

    /**
     * Processes the payments for the given orders using the available payment methods.
     *
     * @param orders  the list of orders to be paid
     * @param methods the list of available payment methods
     * @param summary the payment summary
     */
    public static void payByMethod(List<Order> orders, List<PaymentMethod> methods, PaymentSummary summary) {
        methods.sort(
                Comparator.comparingInt(PaymentMethod::getDiscount)
                        .reversed()
        );

        orders.sort(
                Comparator.comparingDouble(Order::getValue)
        );

        // 1) Phase: full payment by methods >10%
        paymentReorder(orders, methods, summary);

        // 2) Phase: distribute PUNKTY among remaining orders
        PaymentMethod punkty = findMethod(methods, "PUNKTY");
        PaymentMethod lowest = methods.getLast();
        distributePunktyWithFallback(orders, methods, punkty, lowest, summary);

        // 3) Phase: fallback payment for orders that lost PUNKTY slot
        payFallbackOrders(orders, methods, summary);

        // report unpaid orders
//        reportUnpaid(orders, summary);


    }

    /**
     * Reorders the payments for the given orders using the available payment methods.
     *
     * @param orders  the list of orders to be paid
     * @param methods the list of available payment methods
     * @param summary the payment summary
     */
    private static void paymentReorder(List<Order> orders, List<PaymentMethod> methods, PaymentSummary summary) {
        List<Order> unpaid = new ArrayList<>(orders);
        for (PaymentMethod pm : methods) {
            if (pm.getDiscount() <= 10) break;
            Iterator<Order> it = unpaid.iterator();
            while (it.hasNext()) {
                Order o = it.next();
                if (!supports(o, pm)) continue;
                double cost = o.getValue() * (1 - pm.getDiscount() / 100.0);
                if (summary.getRemainingLimit(pm.getId()) >= cost) {
//                    System.out.printf("Order %s paid by %s: %.2f -> %.2f, left=%.2f%n",
//                            o.getId(), pm.getId(), o.getValue(), cost, summary.getRemainingLimit(pm.getId()));
                    PaymentSummary.PaymentEntry entry = new PaymentSummary.PaymentEntry(pm.getId(), cost);
                    summary.putPaymentEntries(o.getId(), List.of(entry));
                    it.remove();
                }
            }
        }
    }

    /**
     * Filters the unpaid orders from the given list of orders.
     *
     * @param orders  the list of orders to be filtered
     * @param summary the payment summary
     * @return a list of unpaid orders
     */
    public static List<Order> filterUnpaidOrders(List<Order> orders, PaymentSummary summary) {
        return orders.stream()
                .filter(o -> summary.getPaymentsForOrder(o.getId()).isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Filters the orders that have mixed payment methods.
     *
     * @param orders        the list of orders to be filtered
     * @param summary       the payment summary
     * @param punktyMethodId the ID of the PUNKTY payment method
     * @return a list of orders with mixed payment methods
     */
    public static List<Order> filterMixedPaymentOrders(
            List<Order> orders,
            PaymentSummary summary,
            String punktyMethodId) {
        // Filter orders that have both PUNKTY and other payment methods
        return orders.stream()
                .filter(o -> {
                    List<PaymentSummary.PaymentEntry> payments =
                            summary.getPaymentsForOrder(o.getId());
                    boolean hasPunkty = payments.stream()
                            .anyMatch(e -> e.getMethodId().equals(punktyMethodId));
                    boolean hasOther  = payments.stream()
                            .anyMatch(e -> !e.getMethodId().equals(punktyMethodId));
                    return hasPunkty && hasOther;
                })
                .collect(Collectors.toList());
    }

    /**
     * Filters the orders that are not fully paid with PUNKTY.
     *
     * @param orders        the list of orders to be filtered
     * @param summary       the payment summary
     * @param punktyMethodId the ID of the PUNKTY payment method
     * @return a list of orders that are not fully paid with PUNKTY
     */
    public static List<Order> filterNotFullyPunktyPaidOrders(
            List<Order> orders,
            PaymentSummary summary,
            String punktyMethodId) {
        // Filter orders that are not fully paid with PUNKTY
        return orders.stream()
                .filter(o -> {
                    List<PaymentSummary.PaymentEntry> payments =
                            summary.getPaymentsForOrder(o.getId());
                    return !( !payments.isEmpty()
                            && payments.stream().allMatch(e -> e.getMethodId().equals(punktyMethodId)) );
                })
                .collect(Collectors.toList());
    }


    /**
     * Distributes PUNKTY among orders that couldn't be fully paid.
     * It first tries to split 10% of the order value for each order,
     * and then distributes the remaining PUNKTY proportionally.
     *
     * @param orders  the list of unpaid orders
     * @param methods the list of available payment methods
     * @param punkty  the PUNKTY payment method
     * @param lowest  the lowest discount payment method
     * @param summary the payment summary
     */
    private static void distributePunktyWithFallback(
            List<Order> orders,
            List<PaymentMethod> methods,
            PaymentMethod punkty,
            PaymentMethod lowest,
            PaymentSummary summary) {

        splitPunkty10percent(orders, punkty, lowest, summary);

        // FULL-PAY PHASE
        Iterator<Order> fpIt = orders.iterator();
        while (fpIt.hasNext()) {
            Order o = fpIt.next();
            double fullCost = o.getValue() * (1 - punkty.getDiscount() / 100.0);
            if (fullCost <= summary.getRemainingLimit(punkty.getId())) {
                PaymentSummary.PaymentEntry entry = new PaymentSummary.PaymentEntry(punkty.getId(), fullCost);
                summary.putPaymentEntries(o.getId(), List.of(entry));
//                System.out.printf(
//                        "Order %s fully paid by %s (extra): cost=%.2f, left pts=%.2f%n",
//                        o.getId(), punkty.getId(), fullCost, summary.getRemainingLimit(punkty.getId())
//                );
                List<Order> notFullyPaid = filterNotFullyPunktyPaidOrders(orders, summary, punkty.getId());
                paymentReorder(notFullyPaid, methods, summary);
                splitPunkty10percent(orders, punkty, lowest, summary);

            }
        }

        List<Order> mixedOrders = filterMixedPaymentOrders(orders, summary, punkty.getId());

        // DISTRIBUTE PHASE
        double extra = summary.getRemainingLimit(punkty.getId());
        double sumOriginal = mixedOrders.stream().mapToDouble(Order::getValue).sum();

        for (Order order : mixedOrders) {
            double orig            = order.getValue();
            double discountedTotal = orig * 0.90;
            double useP = orig * 0.10;

            if (extra > 0 && sumOriginal > 0) {
                useP += extra * (orig / sumOriginal);
            }
            useP = Math.min(useP, discountedTotal);
            double useL = discountedTotal - useP;

//            System.out.printf(
//                    "Order %s: punkty=%.2f, card=%.2f; limits -> %s=%.2f, %s=%.2f%n",
//                    order.getId(),
//                    useP, useL,
//                    punkty.getId(), summary.getRemainingLimit(punkty.getId()),
//                    lowest.getId(), summary.getRemainingLimit(lowest.getId())
//            );

            PaymentSummary.PaymentEntry entryP = new PaymentSummary.PaymentEntry(punkty.getId(), useP);
            PaymentSummary.PaymentEntry entryL = new PaymentSummary.PaymentEntry(lowest.getId(), useL);
            summary.putPaymentEntries(order.getId(), List.of(entryP, entryL));
        }
    }

    /**
     * Splits the PUNKTY payment for orders that couldn't be fully paid.
     * It tries to split 10% of the order value for each order.
     *
     * @param orders  the list of unpaid orders
     * @param punkty  the PUNKTY payment method
     * @param lowest  the lowest discount payment method
     * @param summary the payment summary
     */
    private static void splitPunkty10percent(List<Order> orders, PaymentMethod punkty, PaymentMethod lowest, PaymentSummary summary) {
        List<Order> unpaid = filterUnpaidOrders(orders, summary);
        Map<Order, Double> minMap = new HashMap<>();
        double requiredMinTotal = 0;
        for (Order order : unpaid) {
            double minPts = order.getValue() * 0.10;
            minMap.put(order, minPts);
            requiredMinTotal += minPts;
        }

        double totalPoints = summary.getRemainingLimit(punkty.getId());
        List<Order> punktyOrders = new ArrayList<>(unpaid);
        while (requiredMinTotal > totalPoints && !punktyOrders.isEmpty()) {
            Order worst = Collections.max(punktyOrders, Comparator.comparingDouble(minMap::get));
            punktyOrders.remove(worst);
            requiredMinTotal -= minMap.get(worst);
        }

        for (Order order : punktyOrders) {
            PaymentSummary.PaymentEntry entryP = new PaymentSummary.PaymentEntry(punkty.getId(), order.getValue() * 0.10);
            PaymentSummary.PaymentEntry entryL = new PaymentSummary.PaymentEntry(lowest.getId(), order.getValue() * 0.90);
            summary.putPaymentEntries(order.getId(), List.of(entryP, entryL));
        }
    }

    /**
     * Fallback payment for orders that couldn't be paid for.
     * It tries to pay with the lowest discount payment method.
     *
     * @param orders  the list of unpaid orders
     * @param methods the list of available payment methods
     * @param summary the payment summary
     */
    private static void payFallbackOrders(List<Order> orders,
                                          List<PaymentMethod> methods,
                                          PaymentSummary summary) {
        List<Order> unpaid = filterUnpaidOrders(orders, summary);
        Iterator<Order> it = unpaid.iterator();
        while (it.hasNext()) {
            Order o = it.next();
            boolean paid = false;
            // Trying to pay with a discount (if any) or full price
            for (PaymentMethod pm : methods) {
                if (!supports(o, pm)) {
                    continue;
                }
                double discount = pm.getDiscount() / 100.0;
                double cost = (discount > 0)
                        ? o.getValue() * (1 - discount)
                        : o.getValue();
                if (summary.getRemainingLimit(pm.getId()) >= cost) {
                    PaymentSummary.PaymentEntry entry = new PaymentSummary.PaymentEntry(pm.getId(), cost);
                    summary.putPaymentEntries(o.getId(), List.of(entry));
//                    System.out.printf(
//                            "Fallback: Order %s paid by %s: cost=%.2f, left=%.2f%n",
//                            o.getId(), pm.getId(), cost, summary.getRemainingLimit(pm.getId())
//                    );
                    paid = true;
                    break;
                }
            }
            if (!paid) {
                // if none of the methods could, write off the full price lowest (but do not go into negative territory)
                PaymentMethod lowest = methods.getLast();
                double cost = o.getValue();
                PaymentSummary.PaymentEntry entry = new PaymentSummary.PaymentEntry(lowest.getId(), cost);
                summary.putPaymentEntries(o.getId(), List.of(entry));
//                System.out.printf(
//                        "Fallback final: Order %s forced paid by %s full=%.2f, left=%.2f%n",
//                        o.getId(), lowest.getId(), cost, lowest.getLimit()
//                );
            }
            // Removing it from the fallback
            it.remove();
        }
    }

    /**
     * Checks if the payment method supports the order.
     *
     * @param order the order to check
     * @param pm    the payment method to check
     * @return true if the payment method supports the order, false otherwise
     */
    private static boolean supports(Order order, PaymentMethod pm) {
        String[] promos = order.getPromotions();
        if (promos == null) {
            promos = new String[]{ "PUNKTY" };
        }
        return Arrays.asList(promos).contains(pm.getId());
    }

    /**
     * Reports the orders that couldn't be paid for.
     *
     * @param orders  the list of orders
     * @param summary the payment summary
     */
    private static void reportUnpaid(List<Order> orders, PaymentSummary summary) {
        // фильтруем только неоплаченные заказы
        List<Order> unpaid = filterUnpaidOrders(orders, summary);

        if (unpaid.isEmpty()) {
            System.out.println("All orders have been successfully paid for.");
        } else {
            System.err.println("Couldn't pay for the following orders:");
            unpaid.forEach(o ->
                    System.err.printf(" – Order %s: value=%.2f%n", o.getId(), o.getValue())
            );
        }
    }

    /**
     * Finds a payment method by its ID.
     *
     * @param methods the list of payment methods
     * @param id      the ID of the payment method to find
     * @return the found payment method
     */
    private static PaymentMethod findMethod(List<PaymentMethod> methods, String id) {
        return methods.stream()
                .filter(pm -> id.equals(pm.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Payment method not found: " + id)
                );
    }
}

