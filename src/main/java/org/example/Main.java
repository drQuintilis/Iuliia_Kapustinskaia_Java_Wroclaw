package org.example;

import com.google.gson.reflect.TypeToken;
import org.example.jsonClasses.Order;
import org.example.jsonClasses.PaymentMethod;


import java.lang.reflect.Type;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        // Check if the correct number of arguments is provided
        String ordersPath = args[0];
        String paymentsPath = args[1];

        // Check if the provided paths are valid
        Type ordersType = new TypeToken<List<Order>>(){}.getType();
        Type paymentsType = new TypeToken<List<PaymentMethod>>(){}.getType();

        // Parse the JSON files
        JsonFileParser<Order> orderParser = new JsonFileParser<>(ordersPath, ordersType);
        List<Order> orders = orderParser.parse();

        // Check if the provided paths are valid
        JsonFileParser<PaymentMethod> paymentParser = new JsonFileParser<>(paymentsPath, paymentsType);
        List<PaymentMethod> methods = paymentParser.parse();

        PaymentSummary summary = new PaymentSummary(methods);

//        orders.forEach(System.out::println);
//        methods.forEach(System.out::println);
//        System.out.println();

        // Process the payments
        PaymentProcessor.payByMethod(orders, methods, summary);

        summary.printSummary();
//        System.out.println();
//        summary.printOrderPayments();
    }
}
