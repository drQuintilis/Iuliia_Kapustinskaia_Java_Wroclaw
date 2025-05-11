package org.example.jsonClasses;

public class PaymentMethod {
    private final String id;
    private final int discount;
    private float limit;

    /**
     * Constructor for the PaymentMethod class.
     *
     * @param id      the unique identifier for the payment method
     * @param discount the discount percentage associated with the payment method
     * @param limit   the limit for the payment method
     */
    public PaymentMethod(String id, int discount, float limit) {
        this.id = id;
        this.discount = discount;
        this.limit = limit;
    }

    public String getId() {
        return id;
    }

    public int getDiscount() {
        return discount;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(float limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        return String.format(
                "Payment Method{id = %s, discount = %d, limit = %.2f}",
                id,
                discount,
                limit
        );
    }
}
