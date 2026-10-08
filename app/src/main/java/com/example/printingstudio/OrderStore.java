package com.example.printingstudio;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.tasks.Tasks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrderStore {
    private static OrderStore instance;

    public String userName;
    public String userEmail;

    public static final int MAX_FILE_BYTES = 100 * 1024 * 1024;
    private static final int CHUNK = 700000;

    public Uri fileUri;
    public String fileMime;
    public String fileName;
    public int pageCount;
    public String printType = Order.BW;
    public String speed = Order.NORMAL;
    public String sides = "single";   // "single" or "double" (back to back)
    public int copies = 1;

    public Order activeOrder;
    public List<Order> history = new ArrayList<>();

    private OrderStore() {}

    public static OrderStore get() {
        if (instance == null) instance = new OrderStore();
        return instance;
    }

    public double estimatedPrice() {
        Order o = new Order("", fileName, pageCount, printType, speed, copies);
        o.sides = sides;
        return o.price();
    }

    public Order submitOrder(ContentResolver cr) {
        String id = "C-" + (1000 + (int) (Math.random() * 9000));
        Order order = new Order(id, fileName, pageCount, printType, speed, copies);
        order.sides = sides;
        activeOrder = order;
        history.add(0, order);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final String uid = user != null ? user.getUid() : "";

        final Map<String, Object> data = new HashMap<>();
        data.put("orderId", id);
        data.put("userId", uid);
        data.put("userEmail", user != null ? user.getEmail() : "");
        data.put("fileName", fileName);
        data.put("pageCount", pageCount);
        data.put("printType", printType);
        data.put("speed", speed);
        data.put("sides", sides);
        data.put("copies", copies);
        data.put("price", order.price());
        data.put("status", Order.PLACED);
        data.put("createdAt", FieldValue.serverTimestamp());

        final Uri uri = fileUri;
        final String mime = fileMime != null ? fileMime : "application/octet-stream";
        final String name = fileName;

        // Save the order, then read the file piece by piece (never all in memory)
        // and save each piece. The app must stay open until this finishes.
        new Thread(() -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            long size = 0;
            if (uri != null) {
                try (InputStream in = cr.openInputStream(uri)) {
                    byte[] buf = new byte[65536];
                    int r;
                    while (in != null && (r = in.read(buf)) != -1) {
                        size += r;
                        if (size > MAX_FILE_BYTES) { size = 0; break; }
                    }
                } catch (Exception e) {
                    size = 0;
                }
            }
            final int n = (int) ((size + CHUNK - 1) / CHUNK);
            if (n > 0) {
                data.put("fileChunks", n);
                data.put("fileMime", mime);
                data.put("fileSize", size);
            }

            String docId;
            try {
                DocumentReference ref = Tasks.await(db.collection("orders").add(data), 60, TimeUnit.SECONDS);
                docId = ref.getId();
            } catch (Exception e) {
                return;
            }
            if (n == 0) return;

            try (InputStream in = cr.openInputStream(uri)) {
                if (in == null) return;
                for (int i = 0; i < n; i++) {
                    byte[] part = new byte[CHUNK];
                    int got = 0, r;
                    while (got < CHUNK && (r = in.read(part, got, CHUNK - got)) != -1) got += r;
                    if (got <= 0) return;
                    if (got < CHUNK) part = java.util.Arrays.copyOf(part, got);

                    Map<String, Object> piece = new HashMap<>();
                    piece.put("orderDocId", docId);
                    piece.put("userId", uid);
                    piece.put("index", i);
                    piece.put("total", n);
                    piece.put("fileName", name);
                    piece.put("data", Blob.fromBytes(part));
                    Tasks.await(db.collection("orderFiles").document(docId + "_" + i).set(piece), 120, TimeUnit.SECONDS);
                }
            } catch (Exception ignored) {
            }
        }).start();

        return order;
    }

    public void logout() {
        fileUri = null;
        userName = null;
        userEmail = null;
    }
}
