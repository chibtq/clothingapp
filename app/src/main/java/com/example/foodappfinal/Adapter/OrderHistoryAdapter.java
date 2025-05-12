package com.example.foodappfinal.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.foodappfinal.Model.Order;
import com.example.foodappproject.R;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {
    private Context context;
    private List<Order> orders;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClicked(String orderId);
    }

    public OrderHistoryAdapter(Context context, List<Order> orders, OnOrderClickListener listener) {
        this.context = context;
        this.orders = orders;
        this.listener = listener;
    }

    // Phương thức để cập nhật danh sách đơn hàng
    public void updateOrders(List<Order> newOrders) {
        this.orders.clear();
        this.orders.addAll(newOrders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        // Định dạng ngày
        String orderDate = order.getCreatedAt();
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            Date date = inputFormat.parse(orderDate);
            orderDate = outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        holder.orderDateText.setText(orderDate);

        // Tổng tiền
        holder.totalAmountText.setText(String.format("%.2f₫", order.getTotalAmount()));

        // Trạng thái
        String status = order.getStatus();
        holder.statusText.setText(status);
        // Đặt màu nền cho trạng thái
        int backgroundTint = status.equalsIgnoreCase("confirmed") ?
                R.color.status_confirmed : android.R.color.holo_orange_light;
        holder.statusText.setBackgroundTintList(ContextCompat.getColorStateList(context, backgroundTint));

        // Xử lý sự kiện click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClicked(order.getOrderId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderDateText, totalAmountText, statusText;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderDateText = itemView.findViewById(R.id.order_date_text);
            totalAmountText = itemView.findViewById(R.id.total_amount_text);
            statusText = itemView.findViewById(R.id.status_text);
        }
    }
}