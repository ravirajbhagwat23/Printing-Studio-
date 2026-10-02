package com.example.printingstudio;

import java.util.ArrayList;
import java.util.List;

// Simple in-memory singleton holding app state. No backend yet —
// TODO: replace with real API calls per the Database & Login spec.
public class OrderStore {
    private static OrderStore instance;

    public String userName;
    public String userEmail;

    public String fileName;
    public int pageCount;
    public String printType = Order.BW;
    public String speed = Order.NORMAL;
    public int copies = 1;

    public Order activeOrder;
    public List<Order> history = new ArrayList<>();

    private OrderStore() {}

    public static OrderStore get() {
        if (instance == null) instance = new OrderStore();
        return instance;
    }

    public double estimatedPrice() {
        double rate = printType.equals(Order.COLOR) ? 10.0 : 2.0;
        double multiplier = speed.equals(Order.URGENT) ? 2.0 : 1.0;
        return rate * multiplier * pageCount * copies;
    }

    // TODO: replace with real POST /orders call; server returns authoritative price.
    public Order submitOrder() {
        String id = "C-" + (1000 + (int) (Math.random() * 9000));
        Order order = new Order(id, fileName, pageCount, printType, speed, copies);
        activeOrder = order;
        history.add(0, order);
        return order;
    }

    public void logout() {
        userName = null;
        userEmail = null;
    }
}
