package com.example.printingstudio;

public class Order {
    public static final String BW = "bw";
    public static final String COLOR = "color";
    public static final String NORMAL = "normal";
    public static final String URGENT = "urgent";
    public static final String PLACED = "Placed";
    public static final String PRINTING = "Printing";
    public static final String READY = "Ready";

    public String id;
    public String fileName;
    public int pageCount;
    public String printType; // bw | color
    public String speed;     // normal | urgent
    public int copies;
    public String status;

    public Order(String id, String fileName, int pageCount, String printType, String speed, int copies) {
        this.id = id;
        this.fileName = fileName;
        this.pageCount = pageCount;
        this.printType = printType;
        this.speed = speed;
        this.copies = copies;
        this.status = PLACED;
    }

    // Mirrors the backend formula for live UI preview only.
    // Real total must always come from the server response.
    public double price() {
        double rate = printType.equals(COLOR) ? 10.0 : 2.0;
        double multiplier = speed.equals(URGENT) ? 2.0 : 1.0;
        return rate * multiplier * pageCount * copies;
    }

    public String printTypeLabel() {
        return printType.equals(COLOR) ? "Color · ₹10/pg" : "B/W · ₹2/pg";
    }

    public String speedLabel() {
        return speed.equals(URGENT) ? "Urgent" : "Normal";
    }
}
