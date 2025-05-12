package com.example.foodappfinal.Activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.example.foodappproject.R;
import com.example.foodappproject.databinding.ActivityChangePasswordBinding;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class ChangePasswordActivity extends BaseActivity {
    private static final String TAG = "ChangePasswordActivity";
    private ActivityChangePasswordBinding binding;
    private static final String AUTH_LOGIN_URL = "https://fmheidqpephjpsgjuupf.supabase.co/auth/v1/token?grant_type=password";
    private static final String AUTH_USER_URL = "https://fmheidqpephjpsgjuupf.supabase.co/auth/v1/user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Đổi mật khẩu");
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.confirmButton.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String oldPassword = binding.oldPasswordEdit.getText().toString().trim();
        String newPassword = binding.newPasswordEdit.getText().toString().trim();
        String confirmNewPassword = binding.confirmNewPasswordEdit.getText().toString().trim();

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        getCurrentUserEmail(new EmailCallback() {
            @Override
            public void onSuccess(String email) {
                verifyOldPassword(email, oldPassword, newPassword);
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> Toast.makeText(ChangePasswordActivity.this, "Unable to retrieve user email", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void verifyOldPassword(String email, String oldPassword, String newPassword) {
        String loginJson = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, oldPassword);
        RequestBody loginBody = RequestBody.create(loginJson, MediaType.parse("application/json"));
        Request loginRequest = new Request.Builder()
                .url(AUTH_LOGIN_URL)
                .post(loginBody)
                .addHeader("apikey", getSupabaseKey())
                .build();

        getOkHttpClient().newCall(loginRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to verify old password: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ChangePasswordActivity.this, "Failed to verify old password", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    updatePassword(newPassword);
                } else {
                    String errorMsg = response.body().string();
                    Log.e(TAG, "Old password verification failed: HTTP " + response.code() + " - " + errorMsg);
                    runOnUiThread(() -> Toast.makeText(ChangePasswordActivity.this, "Invalid old password", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updatePassword(String newPassword) {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            refreshToken(new RefreshTokenCallback() {
                @Override
                public void onSuccess(String newAccessToken) {
                    sendPasswordUpdateRequest(newAccessToken, newPassword);
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> Toast.makeText(ChangePasswordActivity.this, "Failed to refresh token", Toast.LENGTH_SHORT).show());
                }
            });
            return;
        }

        sendPasswordUpdateRequest(accessToken, newPassword);
    }

    private void sendPasswordUpdateRequest(String accessToken, String newPassword) {
        String json = String.format("{\"password\":\"%s\"}", newPassword);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = getRequestBuilder()
                .url(AUTH_USER_URL)
                .put(body)
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to change password: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ChangePasswordActivity.this, "Failed to change password", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChangePasswordActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                        sendNotification("Đổi mật khẩu thành công");
                        logout();
                        finish();
                    });
                } else {
                    String errorMsg = response.body().string();
                    Log.e(TAG, "Failed to change password: HTTP " + response.code() + " - " + errorMsg);
                    runOnUiThread(() -> Toast.makeText(ChangePasswordActivity.this, "Failed to change password: HTTP " + response.code() + " - " + errorMsg, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void getCurrentUserEmail(EmailCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("User ID is null"));
            return;
        }

        String url = getSupabaseUrl() + "/rest/v1/user_profiles?user_id=eq." + userId + "&select=email";
        Request request = getRequestBuilder().url(url).get().build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get user email: " + e.getMessage());
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        Log.d(TAG, "User email response: " + responseData);
                        JSONArray jsonArray = new JSONArray(responseData);
                        if (jsonArray.length() > 0) {
                            JSONObject jsonObject = jsonArray.getJSONObject(0);
                            String email = jsonObject.getString("email");
                            callback.onSuccess(email);
                        } else {
                            callback.onFailure(new Exception("No email found for user"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse user email: " + e.getMessage());
                        callback.onFailure(e);
                    }
                } else {
                    String errorMsg = response.body().string();
                    Log.e(TAG, "Failed to get user email: HTTP " + response.code() + " - " + errorMsg);
                    callback.onFailure(new Exception("HTTP " + response.code()));
                }
            }
        });
    }

    private interface EmailCallback {
        void onSuccess(String email);
        void onFailure(Exception e);
    }
}