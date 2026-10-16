package com.example.printingstudio;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OrderPriceTest {

    private Order make(String type, String sides, int pages, int copies) {
        Order o = new Order("T", "a.pdf", pages, type, Order.NORMAL, copies);
        o.sides = sides;
        return o;
    }

    @Test
    public void bw_single_side() {
        assertEquals(34.0, make(Order.BW, "single", 17, 1).price(), 0.001);
    }

    @Test
    public void color_single_side() {
        assertEquals(100.0, make(Order.COLOR, "single", 5, 2).price(), 0.001);
    }

    @Test
    public void bw_double_side_odd_pages() {
        assertEquals(18.0, make(Order.BW, "double", 17, 1).price(), 0.001);
    }

    @Test
    public void copies_multiply_sheets() {
        assertEquals(3, make(Order.BW, "single", 1, 3).sheets());
    }
}
