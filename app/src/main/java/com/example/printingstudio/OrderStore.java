package com.example.printingstudio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

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

    public Order submitOrder() {
        String id = "C-" + (1000 + (int) (Math.random() * 9000));
        Order order = new Order(id, fileName, pageCount, printType, speed, copies);
        activeOrder = order;
        history.add(0, order);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", id);
        data.put("userId", user != null ? user.getUid() : "");
        data.put("userEmail", user != null ? user.getEmail() : "");
        data.put("fileName", fileName);
        data.put("pageCount", pageCount);
        data.put("printType", printType);
        data.put("speed", speed);
        data.put("copies", copies);
        data.put("price", order.price());
        data.put("status", Order.PLACED);
        data.put("createdAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("orders").add(data);

        return order;
    }

    public void logout() {
        userName = null;
        userEmail = null;
    }
}