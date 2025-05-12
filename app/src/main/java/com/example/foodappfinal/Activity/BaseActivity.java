package com.example.foodappfinal.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.foodappfinal.NotificationBroadcast;
import com.example.foodappproject.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    private OkHttpClient okHttpClient;
    private SharedPreferences sharedPreferences;
    private static final String NOTIFICATIONS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/notifications";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: BaseActivity initialized");
        okHttpClient = new OkHttpClient();
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        Log.d(TAG, "onCreate: OkHttpClient and SharedPreferences initialized");
    }

    public OkHttpClient getOkHttpClient() {
        Log.d(TAG, "getOkHttpClient: Returning OkHttpClient instance");
        return okHttpClient;
    }

    public String getSupabaseKey() {
        String key = getString(R.string.supabase_key);
        Log.d(TAG, "getSupabaseKey: Retrieved Supabase key: " + (key != null ? "valid" : "null"));
        return key;
    }

    protected String getSupabaseServiceKey() {
        String serviceKey = getString(R.string.supabase_service_key);
        Log.d(TAG, "getSupabaseServiceKey: Retrieved Supabase service key: " + (serviceKey != null ? "valid" : "null"));
        return serviceKey;
    }

    protected String getSendGridApiKey() {
        String sendGridKey = getString(R.string.sendgrid_api_key);
        Log.d(TAG, "getSendGridApiKey: Retrieved SendGrid API key: " + (sendGridKey != null ? "valid" : "null"));
        return sendGridKey;
    }

    public String getSupabaseUrl() {
        String url = "https://fmheidqpephjpsgjuupf.supabase.co";
        Log.d(TAG, "getSupabaseUrl: Returning Supabase URL: " + url);
        return url;
    }

    public Request.Builder getRequestBuilder() {
        String accessToken = getAccessToken();
        Log.d(TAG, "getRequestBuilder: Access token retrieved: " + (accessToken != null ? "exists" : "null"));
        if (accessToken == null) {
            Log.e(TAG, "getRequestBuilder: Access token is missing, initiating logout");
            runOnUiThread(this::logout);
            throw new IllegalStateException("Access token is missing");
        }

        Request.Builder builder = new Request.Builder()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", getSupabaseKey());
        Log.d(TAG, "getRequestBuilder: Request builder configured with Authorization header");
        return builder;
    }

    public Request.Builder getServiceRequestBuilder() {
        String serviceKey = getSupabaseServiceKey();
        Log.d(TAG, "getServiceRequestBuilder: Service key retrieved: " + (serviceKey != null ? "valid" : "null"));
        Request.Builder builder = new Request.Builder()
                .addHeader("Authorization", "Bearer " + serviceKey)
                .addHeader("apikey", serviceKey);
        Log.d(TAG, "getServiceRequestBuilder: Request builder configured with service key");
        return builder;
    }

    public String getCurrentUserId() {
        String userId = sharedPreferences.getString("user_id", null);
        Log.d(TAG, "getCurrentUserId: Retrieved userId: " + (userId != null ? userId : "null"));
        return userId;
    }

    public String getAccessToken() {
        String token = sharedPreferences.getString("access_token", null);
        Log.d(TAG, "getAccessToken: Retrieved accessToken: " + (token != null ? "exists" : "null"));
        return token;
    }

    public String getRefreshToken() {
        String refreshToken = sharedPreferences.getString("refresh_token", null);
        Log.d(TAG, "getRefreshToken: Retrieved refreshToken: " + (refreshToken != null ? "exists" : "null"));
        return refreshToken;
    }

    public void refreshToken(RefreshTokenCallback callback) {
        String refreshToken = getRefreshToken();
        Log.d(TAG, "refreshToken: Attempting to refresh token with refreshToken: " + (refreshToken != null ? "exists" : "null"));
        if (refreshToken == null) {
            Log.e(TAG, "refreshToken: Refresh token is missing");
            runOnUiThread(() -> callback.onFailure(new Exception("Refresh token is missing")));
            return;
        }

        String url = getSupabaseUrl() + "/auth/v1/token?grant_type=refresh_token";
        String json = String.format("{\"refresh_token\":\"%s\"}", refreshToken);
        Log.d(TAG, "refreshToken: Refresh request URL: " + url + ", JSON body: " + json);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", getSupabaseKey())
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "refreshToken: Failed to refresh token: " + e.getMessage());
                runOnUiThread(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "refreshToken: Response received, code: " + response.code() + ", body: " + responseBody);
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        String newAccessToken = jsonObject.getString("access_token");
                        String newRefreshToken = jsonObject.optString("refresh_token", refreshToken);
                        int expiresIn = jsonObject.getInt("expires_in");

                        Log.d(TAG, "refreshToken: Successfully refreshed, newAccessToken=" + newAccessToken + ", newRefreshToken=" + newRefreshToken + ", expiresIn=" + expiresIn);

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("access_token", newAccessToken);
                        editor.putString("refresh_token", newRefreshToken);
                        editor.apply();
                        Log.d(TAG, "refreshToken: Saved new tokens to SharedPreferences");

                        runOnUiThread(() -> callback.onSuccess(newAccessToken));
                    } catch (Exception e) {
                        Log.e(TAG, "refreshToken: Failed to parse response: " + e.getMessage());
                        runOnUiThread(() -> callback.onFailure(e));
                    }
                } else {
                    Log.e(TAG, "refreshToken: Failed to refresh token: HTTP " + response.code() + ", body: " + responseBody);
                    runOnUiThread(() -> callback.onFailure(new Exception("Failed to refresh token: HTTP " + response.code())));
                }
            }
        });
    }

    public interface RefreshTokenCallback {
        void onSuccess(String newAccessToken);
        void onFailure(Exception e);
    }

    public void logout() {
        Log.d(TAG, "logout: Initiating logout");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("user_id");
        editor.remove("access_token");
        editor.remove("refresh_token");
        editor.apply();
        Log.d(TAG, "logout: Cleared all session data");
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    public void sendNotification(String content) {
        String userId = getCurrentUserId();
        Log.d(TAG, "sendNotification: Attempting to send notification, userId: " + (userId != null ? userId : "null"));
        if (userId == null) {
            Log.e(TAG, "sendNotification: User ID is null, cannot send notification");
            return;
        }

        String notificationId = UUID.randomUUID().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String timestamp = sdf.format(new Date());
        Log.d(TAG, "sendNotification: Generated notificationId=" + notificationId + ", timestamp=" + timestamp);

        String json = String.format(
                "{\"notification_id\":\"%s\",\"user_id\":\"%s\",\"content\":\"%s\",\"created_at\":\"%s\",\"is_read\":false}",
                notificationId, userId, content, timestamp
        );
        Log.d(TAG, "sendNotification: JSON request body: " + json);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = getRequestBuilder()
                .url(NOTIFICATIONS_URL)
                .post(body)
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "sendNotification: Failed to send notification: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "sendNotification: Response received, code: " + response.code() + ", body: " + responseBody);
                if (response.isSuccessful()) {
                    Log.d(TAG, "sendNotification: Notification sent successfully");
                    Intent intent = new Intent(NotificationBroadcast.NOTIFICATION_ADDED);
                    sendBroadcast(intent);
                } else if (response.code() == 401) {
                    Log.w(TAG, "sendNotification: Unauthorized, attempting token refresh");
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess(String newAccessToken) {
                            Log.d(TAG, "sendNotification: Token refreshed, retrying notification");
                            sendNotification(content); // Retry with new token
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "sendNotification: Failed to refresh token: " + e.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "sendNotification: Failed: HTTP " + response.code() + ", body: " + responseBody);
                }
            }
        });
    }
}