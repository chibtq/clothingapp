package com.example.foodappfinal.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.example.foodappfinal.Model.UserProfile;
import com.example.foodappproject.R;
import com.example.foodappproject.databinding.ActivityProfileBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class ProfileActivity extends BaseActivity {
    private static final String TAG = "ProfileActivity";
    private ActivityProfileBinding binding;
    private static final String USER_PROFILES_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/user_profiles";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Profile");
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Load profile data initially
        loadUserProfile();

        // Sự kiện cho các nút menu
        binding.personalInfoButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, PersonalInfoActivity.class);
            startActivity(intent);
        });

        binding.orderHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, OrderHistoryActivity.class);
            startActivity(intent);
        });

        binding.statisticsButton.setOnClickListener(v -> {
            String userId = getCurrentUserId();
            if (userId != null) {
                checkUserRole(userId);
            } else {
                Toast.makeText(this, "Please log in to access statistics", Toast.LENGTH_SHORT).show();
            }
        });

        binding.changePasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        binding.logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload profile data every time the activity resumes to reflect any changes
        loadUserProfile();
    }

    private void loadUserProfile() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Please log in to view profile", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        String url = USER_PROFILES_URL + "?user_id=eq." + userId;
        Request request = getRequestBuilder()
                .url(url)
                .get()
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Type userProfileType = new TypeToken<List<UserProfile>>(){}.getType();
                    List<UserProfile> userProfiles = gson.fromJson(responseData, userProfileType);

                    if (userProfiles != null && !userProfiles.isEmpty()) {
                        UserProfile user = userProfiles.get(0);
                        runOnUiThread(() -> {
                            binding.usernameText.setText(user.getUsername() != null ? user.getUsername() : "");
                            binding.emailText.setText(user.getEmail() != null ? user.getEmail() : "email@example.com");
                            // Hiển thị ảnh đại diện nếu có, with timestamp to avoid cache
                            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                                Glide.with(ProfileActivity.this)
                                        .load(user.getAvatarUrl() + "?t=" + System.currentTimeMillis()) // Add timestamp to avoid cache
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_profile_placeholder)
                                        .error(R.drawable.ic_profile_placeholder)
                                        .into(binding.avatarImage);
                            } else {
                                binding.avatarImage.setImageResource(R.drawable.ic_profile_placeholder);
                            }
                        });
                    }
                }
            }
        });
    }

    private void checkUserRole(String userId) {
        String url = USER_PROFILES_URL + "?user_id=eq." + userId + "&select=role";
        Request request = getRequestBuilder()
                .url(url)
                .get()
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Failed to check role", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        Gson gson = new Gson();
                        Type userProfileType = new TypeToken<List<UserProfile>>(){}.getType();
                        List<UserProfile> userProfiles = gson.fromJson(responseData, userProfileType);
                        if (userProfiles != null && !userProfiles.isEmpty()) {
                            String role = userProfiles.get(0).getRole();
                            if ("admin".equals(role)) {
                                runOnUiThread(() -> {
                                    Intent intent = new Intent(ProfileActivity.this, StatisticsActivity.class);
                                    startActivity(intent);
                                });
                            } else {
                                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Chức năng chỉ dành cho quản trị viên", Toast.LENGTH_SHORT).show());
                            }
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Error parsing role data", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Failed to check role", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Có", (dialog, which) -> logout())
                .setNegativeButton("Không", null)
                .show();
    }
}