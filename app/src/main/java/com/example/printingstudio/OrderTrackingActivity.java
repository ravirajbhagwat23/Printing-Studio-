package com.example.printingstudio;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OrderTrackingActivity extends AppCompatActivity {

    TextView tvStatus, tvReady;
    Button btnNextStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        TextView tvOrderId = findViewById(R.id.tvOrderId);
        tvStatus = findViewById(R.id.tvStatus);
        tvReady = findViewById(R.id.tvReady);
        btnNextStatus = findViewById(R.id.btnNextStatus);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Order order = OrderStore.get().activeOrder;
        if (order != null) tvOrderId.setText("Order #" + order.id);

        // TODO: replace with polling GET /orders/:id every 15–30s, or push notifications
        btnNextStatus.setOnClickListener(v -> {
            if (order == null) return;
            if (order.status.equals(Order.PLACED)) order.status = Order.PRINTING;
            else if (order.status.equals(Order.PRINTING)) order.status = Order.READY;
            refresh(order);
        });

        refresh(order);
    }

    private void refresh(Order order) {
        if (order == null) return;
        tvStatus.setText("Status: " + order.status);
        boolean ready = order.status.equals(Order.READY);
        tvReady.setVisibility(ready ? View.VISIBLE : View.GONE);
        btnNextStatus.setVisibility(ready ? View.GONE : View.VISIBLE);
    }
}
