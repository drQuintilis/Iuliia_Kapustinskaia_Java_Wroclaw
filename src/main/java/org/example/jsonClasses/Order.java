package org.example.jsonClasses;

import java.util.Arrays;

public class Order {
    private final String id;
    private final float value;
    private final String[] promotions;

    /**
     * Constructor for the Order class.
     *
     * @param id         the unique identifier for the order
     * @param value      the value of the order
     * @param promotions an array of promotions associated with the order
     */
    public Order(String id, float value, String[] promotions) {
        this.id = id;
        this.value = value;
        this.promotions = promotions;
    }

    public String getId() {
        return id;
    }

    public float getValue(){
        return value;
    }

    public String[] getPromotions(){
        return promotions;
    }

    @Override
    public String toString() {
        return String.format(
                "Order{id = %s, value = %.2f, promotions = %s}",
                id,
                value,
                Arrays.toString(promotions)
        );
    }
}
