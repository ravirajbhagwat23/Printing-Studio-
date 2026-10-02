package com.example.printingstudio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {

    private final List<Order> orders;

    public OrderHistoryAdapter(List<Order> orders) {
        this.orders = orders;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.tvFileName.setText(order.fileName);
        holder.tvDetails.setText(order.printTypeLabel() + " · " + order.speedLabel() + " · "
                + order.copies + (order.copies == 1 ? " copy" : " copies"));
        holder.tvPriceStatus.setText("₹" + (int) order.price() + " · " + order.status);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvDetails, tvPriceStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvPriceStatus = itemView.findViewById(R.id.tvPriceStatus);
        }
    }
}
