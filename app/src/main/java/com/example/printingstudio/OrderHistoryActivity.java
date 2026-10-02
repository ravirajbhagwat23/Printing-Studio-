package com.example.printingstudio;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class OrderHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView rvOrders = findViewById(R.id.rvOrders);

        if (OrderStore.get().history.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvOrders.setLayoutManager(new LinearLayoutManager(this));
            rvOrders.setAdapter(new OrderHistoryAdapter(OrderStore.get().history));
        }
    }
}
