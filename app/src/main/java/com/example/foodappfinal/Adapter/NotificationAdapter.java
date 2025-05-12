package com.example.foodappfinal.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.foodappfinal.Activity.OrderDetailActivity;
import com.example.foodappfinal.Domain.NotificationDomain;
import com.example.foodappproject.R;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private static final String TAG = "NotificationAdapter";
    private List<NotificationDomain> notifications;
    private Context context;
    private OnNotificationClickListener onNotificationClickListener;

    public interface OnNotificationClickListener {
        void onNotificationRead(NotificationDomain notification);
    }

    public NotificationAdapter(Context context, List<NotificationDomain> notifications, OnNotificationClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.onNotificationClickListener = listener;
        sortNotifications();
        Log.d(TAG, "Adapter initialized with " + (notifications != null ? notifications.size() : 0) + " notifications");
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.notification_item, parent, false);
        Log.d(TAG, "ViewHolder created");
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        NotificationDomain notification = notifications.get(position);
        holder.contentTextView.setText(notification.getContent());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
        holder.timeTextView.setText(sdf.format(notification.getCreated_at()));

        int textColor = notification.isRead() ? R.color.gray : R.color.black;
        holder.contentTextView.setTextColor(context.getResources().getColor(textColor));
        holder.timeTextView.setTextColor(context.getResources().getColor(textColor));
        Log.d(TAG, "Binding notification at position " + position + ": " + notification.getContent() + ", isRead=" + notification.isRead());

        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);

        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Notification clicked at position " + position + ": " + notification.getContent());
            if (!notification.isRead()) {
                if (onNotificationClickListener != null) {
                    onNotificationClickListener.onNotificationRead(notification);
                    Log.d(TAG, "onNotificationClickListener called for notification ID: " + notification.getId());
                } else {
                    Log.w(TAG, "onNotificationClickListener is null, cannot notify read event");
                }
                notifyItemChanged(position);
                sortNotifications();
            } else {
                Log.d(TAG, "Click ignored: Notification already read at position " + position);
            }
            if (notification.getContent().contains("Order")) {
                String orderId = notification.getContent().split("Order ")[1].split(" ")[0];
                Intent intent = new Intent(context, OrderDetailActivity.class);
                intent.putExtra("order_id", orderId);
                context.startActivity(intent);
                Log.d(TAG, "Starting OrderDetailActivity for order: " + orderId);
            }
        });

        holder.itemView.setOnTouchListener((v, event) -> {
            Log.d(TAG, "Touch event detected at position " + position + ": action=" + event.getAction());
            return false;
        });

        Log.d(TAG, "Set OnClickListener and OnTouchListener for notification at position " + position);
    }

    @Override
    public int getItemCount() {
        int count = Math.min(notifications != null ? notifications.size() : 0, 5);
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    public void updateNotifications(List<NotificationDomain> newNotifications) {
        Log.d(TAG, "updateNotifications: Received " + (newNotifications != null ? newNotifications.size() : 0) + " notifications");
        this.notifications.clear();
        if (newNotifications != null) {
            this.notifications.addAll(newNotifications);
        }
        sortNotifications();
        notifyDataSetChanged();
        Log.d(TAG, "updateNotifications: Updated notifications, new size: " + notifications.size());
    }

    private void sortNotifications() {
        Collections.sort(notifications, new Comparator<NotificationDomain>() {
            @Override
            public int compare(NotificationDomain n1, NotificationDomain n2) {
                if (n1.isRead() != n2.isRead()) {
                    return n1.isRead() ? 1 : -1;
                }
                return n2.getCreated_at().compareTo(n1.getCreated_at());
            }
        });
        Log.d(TAG, "sortNotifications: Notifications sorted, total: " + notifications.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView contentTextView;
        TextView timeTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            contentTextView = itemView.findViewById(R.id.notificationContent);
            timeTextView = itemView.findViewById(R.id.notificationTime);
            Log.d(TAG, "ViewHolder initialized");
        }
    }
}