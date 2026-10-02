package com.example.printingstudio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OrderConfirmedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmed);

        Order order = OrderStore.get().activeOrder;

        TextView tvOrderId = findViewById(R.id.tvOrderId);
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvSummary = findViewById(R.id.tvSummary);

        if (order != null) {
            tvOrderId.setText("Order #" + order.id + " · College Print Shop");
            tvStatus.setText("Status: " + order.status);
            String summary = "File: " + order.fileName + "\n"
                    + "Pages: " + order.pageCount + "\n"
                    + "Print type: " + order.printTypeLabel() + "\n"
                    + "Speed: " + order.speedLabel() + "\n"
                    + "Copies: " + order.copies + "\n\n"
                    + "Total paid: ₹" + (int) order.price();
            tvSummary.setText(summary);
        }

        findViewById(R.id.btnTrack).setOnClickListener(v ->
                startActivity(new Intent(this, OrderTrackingActivity.class)));

        findViewById(R.id.btnBackHome).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }
}
