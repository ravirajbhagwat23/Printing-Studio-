package com.example.printingstudio;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private final List<Order> orders = new ArrayList<>();
    private OrderHistoryAdapter adapter;
    private ListenerRegistration listener;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView rvOrders = findViewById(R.id.rvOrders);
        adapter = new OrderHistoryAdapter(orders);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        listener = FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;

                    List<DocumentSnapshot> docs = new ArrayList<>(snap.getDocuments());
                    Collections.sort(docs, (a, b) -> {
                        Timestamp ta = a.getTimestamp("createdAt");
                        Timestamp tb = b.getTimestamp("createdAt");
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return -1;
                        if (tb == null) return 1;
                        return tb.compareTo(ta);
                    });

                    orders.clear();
                    for (DocumentSnapshot doc : docs) {
                        String id = doc.getString("orderId");
                        String type = doc.getString("printType");
                        String speed = doc.getString("speed");
                        Long pages = doc.getLong("pageCount");
                        Long copies = doc.getLong("copies");

                        Order o = new Order(
                                id != null ? id : "",
                                doc.getString("fileName"),
                                pages != null ? pages.intValue() : 0,
                                type != null ? type : Order.BW,
                                speed != null ? speed : Order.NORMAL,
                                copies != null ? copies.intValue() : 1);
                        String status = doc.getString("status");
                        if (status != null) o.status = status;
                        orders.add(o);
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }
}