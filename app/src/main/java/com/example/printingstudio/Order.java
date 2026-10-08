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
    public String sides = "single"; // single | double (back to back)
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

    // Paper sheets used: single side = 1 sheet per page, back to back = 1 sheet per 2 pages.
    public int sheets() {
        int perCopy = "double".equals(sides) ? (pageCount + 1) / 2 : pageCount;
        return perCopy * copies;
    }

    // Price is charged per sheet. Must match the Firestore rules formula.
    public double price() {
        double rate = printType.equals(COLOR) ? 10.0 : 2.0;
        double multiplier = speed.equals(URGENT) ? 2.0 : 1.0;
        return rate * multiplier * sheets();
    }

    public String printTypeLabel() {
        return printType.equals(COLOR) ? "Color · ₹10/pg" : "B/W · ₹2/pg";
    }

    public String speedLabel() {
        return speed.equals(URGENT) ? "Urgent" : "Normal";
    }
}
