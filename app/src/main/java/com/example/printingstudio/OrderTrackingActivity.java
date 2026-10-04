package com.example.printingstudio;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class OrderTrackingActivity extends AppCompatActivity {

    TextView tvStatus, tvReady;
    Button btnNextStatus;
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        TextView tvOrderId = findViewById(R.id.tvOrderId);
        tvStatus = findViewById(R.id.tvStatus);
        tvReady = findViewById(R.id.tvReady);
        btnNextStatus = findViewById(R.id.btnNextStatus);
        btnNextStatus.setVisibility(View.GONE);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Order order = OrderStore.get().activeOrder;
        if (order != null) tvOrderId.setText("Order #" + order.id);

        refresh(order);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Order order = OrderStore.get().activeOrder;
        if (order == null) return;

        listener = FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("orderId", order.id)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || snap.isEmpty()) return;
                    String status = snap.getDocuments().get(0).getString("status");
                    if (status != null) {
                        order.status = status;
                        refresh(order);
                    }
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

    private void refresh(Order order) {
        if (order == null) return;
        tvStatus.setText("Status: " + order.status);
        tvReady.setVisibility(order.status.equals(Order.READY) ? View.VISIBLE : View.GONE);
    }
}