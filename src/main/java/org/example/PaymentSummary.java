package org.example;

import org.example.jsonClasses.PaymentMethod;

import java.util.*;

public class PaymentSummary {
    private final Map<String, List<PaymentEntry>> paymentsByOrder = new LinkedHashMap<>();
    private final Map<String, Double> initialLimits = new LinkedHashMap<>();

    /**
     * Constructor that initializes the payment summary with a list of payment methods.
     *
     * @param methods the list of payment methods
     */
    public PaymentSummary(List<PaymentMethod> methods) {
        for (PaymentMethod pm : methods) {
            initialLimits.put(pm.getId(), pm.getLimit());
        }
    }

    /**
     * Adds a payment entry for a specific order.
     *
     * @param orderId the ID of the order
     * @param entries   the payment entry to add
     */
    public void putPaymentEntries(String orderId, List<PaymentEntry> entries) {
        Objects.requireNonNull(orderId, "orderId не может быть null");
        Objects.requireNonNull(entries, "entries не может быть null");
        // replace existing or add new
        paymentsByOrder.put(orderId, entries);
    }


    public List<PaymentEntry> getPaymentsForOrder(String orderId) {
        return paymentsByOrder.getOrDefault(orderId, Collections.emptyList());
    }

    /**
     * Sums all payments for a given payment method.
     *
     * @param methodId the ID of the payment method
     * @return the total sum of payments for the method
     */
    private double sumPaymentsByMethod(String methodId) {
        return paymentsByOrder.values().stream()
                .flatMap(Collection::stream)
                .filter(e -> e.getMethodId().equals(methodId))
                .mapToDouble(PaymentEntry::getAmount)
                .sum();
    }

    /**
     * Sums all card payments, excluding "PUNKTY".
     *
     * @return the total sum of card payments
     */
    public double sumCardPayments() {
        return initialLimits.keySet().stream()
                .filter(methodId -> !"PUNKTY".equals(methodId))
                .mapToDouble(this::sumPaymentsByMethod)
                .sum();
    }

    /**
     * Returns the remaining limit for a given payment method.
     *
     * @param methodId the ID of the payment method
     * @return the remaining limit
     */
    public double getRemainingLimit(String methodId) {
        double paid = sumPaymentsByMethod(methodId);
        double initial = initialLimits.getOrDefault(methodId, 0.0);
        return initial - paid;
    }

    /**
     * Prints a summary of payments by method.
     */
    public void printSummary() {
//        System.out.println("\n=== Payment Summary ===");
        for (String methodId : initialLimits.keySet()) {
            double paid = sumPaymentsByMethod(methodId);
            double remaining = getRemainingLimit(methodId);
            System.out.printf("%s paid=%.2f\n", methodId, paid);
        }
        double cardTotal = sumCardPayments();
//        System.out.printf("Total card payments (excluding PUNKTY): %.2f%n", cardTotal);
    }

    /**
     * Prints detailed payments by order.
     */
    public void printOrderPayments() {
        System.out.println("\n=== Detailed Payments by Order ===");
        paymentsByOrder.keySet().stream()
                .sorted()
                .forEach(orderId -> {
                    System.out.printf("%s:%n", orderId);
                    for (PaymentEntry e : paymentsByOrder.get(orderId)) {
                        System.out.printf("  %s -> %.2f%n", e.getMethodId(), e.getAmount());
                    }
                });
    }

    /**
     * Represents a payment entry with a method ID and amount.
     */
    public static class PaymentEntry {
        private final String methodId;
        private final double amount;

        public PaymentEntry(String methodId, double amount) {
            this.methodId = methodId;
            this.amount = amount;
        }

        public String getMethodId() { return methodId; }
        public double getAmount() { return amount; }
    }
}
