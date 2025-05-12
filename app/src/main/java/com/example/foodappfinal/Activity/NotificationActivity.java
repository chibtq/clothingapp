package com.example.foodappfinal.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.foodappfinal.Adapter.NotificationAdapter;
import com.example.foodappfinal.Domain.NotificationDomain;
import com.example.foodappproject.R;
import com.example.foodappproject.databinding.ActivityNotificationBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationActivity extends BaseActivity implements NotificationAdapter.OnNotificationClickListener {
    private static final String TAG = "NotificationActivity";
    private ActivityNotificationBinding binding;
    private ArrayList<NotificationDomain> notifications;
    private NotificationAdapter notificationAdapter;
    private int unreadCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting NotificationActivity");
        setContentView(R.layout.activity_notification);
        binding = ActivityNotificationBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        notifications = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(this, notifications, notification -> onNotificationRead(notification));
        binding.notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.notificationRecyclerView.setAdapter(notificationAdapter);

        initNotifications();
    }

    private void initNotifications() {
        Log.d(TAG, "initNotifications: Starting to load notifications");
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "initNotifications: User ID is null, skipping notification fetch");
            runOnUiThread(() -> {
                notifications.clear();
                notificationAdapter.notifyDataSetChanged();
                binding.noNotificationsText.setVisibility(View.VISIBLE);
                binding.notificationRecyclerView.setVisibility(View.GONE);
                Log.d(TAG, "initNotifications: Cleared notifications due to null user ID");
            });
            return;
        }

        String url = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/notifications?user_id=eq." + userId + "&order=created_at.desc";
        Request request = getRequestBuilder().url(url).get().build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "initNotifications: Notifications fetch failed: " + e.getMessage());
                runOnUiThread(() -> {
//                    Toast.makeText(NotificationActivity.this, "Failed to load notifications", Toast.LENGTH_SHORT).show());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d(TAG, "initNotifications: Notification data received: " + responseData);
                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<NotificationDomain>>(){}.getType();
                    List<NotificationDomain> tempNotifications = gson.fromJson(responseData, listType);
                    runOnUiThread(() -> {
                        notifications.clear();
                        if (tempNotifications != null && !tempNotifications.isEmpty()) {
                            notifications.addAll(tempNotifications);
                            binding.noNotificationsText.setVisibility(View.GONE);
                            binding.notificationRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            binding.noNotificationsText.setVisibility(View.VISIBLE);
                            binding.notificationRecyclerView.setVisibility(View.GONE);
                        }
                        unreadCount = (int) notifications.stream().filter(n -> !n.isRead()).count();
                        notificationAdapter.updateNotifications(notifications);
                        Log.d(TAG, "initNotifications: Updated notifications, total count: " + notifications.size() + ", unread count: " + unreadCount);
                    });
                } else {
                    Log.e(TAG, "initNotifications: Notifications fetch failed, code: " + response.code() + ", message: " + response.message());
                    runOnUiThread(() -> {
//                        Toast.makeText(NotificationActivity.this, "Failed to load notifications", Toast.LENGTH_SHORT).show());
                    });
                }
            }
        });
    }

    @Override
    public void onNotificationRead(NotificationDomain notification) {
        Log.d(TAG, "onNotificationRead: Marking notification as read, ID: " + notification.getId());
        unreadCount--;
        notificationAdapter.notifyDataSetChanged();
        Log.d(TAG, "onNotificationRead: Updated unread count to " + unreadCount);
    }
}