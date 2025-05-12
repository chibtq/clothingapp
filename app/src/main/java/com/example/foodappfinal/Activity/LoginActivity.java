package com.example.foodappfinal.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.foodappproject.databinding.ActivityLoginBinding;

import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends BaseActivity {
    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "onCreate: LoginActivity started");

        // Khởi tạo SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        Log.d(TAG, "onCreate: SharedPreferences initialized");

        // Kiểm tra và xác thực phiên đăng nhập trước khi chuyển sang Dashboard
        String accessToken = sharedPreferences.getString("access_token", null);
        String userId = sharedPreferences.getString("user_id", null);
        Log.d(TAG, "onCreate: Retrieved accessToken=" + (accessToken != null ? "exists" : "null") + ", userId=" + (userId != null ? "exists" : "null"));

        if (accessToken != null && userId != null) {
            Log.d(TAG, "onCreate: Validating existing session with accessToken");
            validateSession(accessToken);
        } else {
            Log.d(TAG, "onCreate: No valid session found, setting up login listeners");
            setupLoginListeners();
        }

        // Navigate to RegisterActivity
        binding.signUpTextView.setOnClickListener(v -> {
            Log.d(TAG, "signUpTextView clicked: Navigating to RegisterActivity");
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Forget Password
        binding.forgetPasswordTextView.setOnClickListener(v -> {
            Log.d(TAG, "forgetPasswordTextView clicked: Navigating to ForgetPasswordActivity");
            startActivity(new Intent(LoginActivity.this, ForgetPasswordActivity.class));
        });
    }

    private void validateSession(String accessToken) {
        String url = getSupabaseUrl() + "/auth/v1/user";
        Log.d(TAG, "validateSession: Validating session with URL: " + url + ", accessToken: " + accessToken);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", getSupabaseKey())
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "validateSession: Session validation failed: " + e.getMessage());
                runOnUiThread(() -> {
                    Log.d(TAG, "validateSession: Clearing session due to failure");
                    clearSession();
                    setupLoginListeners();
                    Toast.makeText(LoginActivity.this, "Session validation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "validateSession: Response received, code: " + response.code() + ", body: " + responseBody);
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "validateSession: Session is valid, navigating to DashboardActivity");
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        finish();
                    });
                } else {
                    Log.w(TAG, "validateSession: Session invalid, response code: " + response.code() + ", body: " + responseBody);
                    runOnUiThread(() -> {
                        Log.d(TAG, "validateSession: Clearing session due to invalid response");
                        clearSession();
                        setupLoginListeners();
                        Toast.makeText(LoginActivity.this, "Session invalid, please log in again", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void clearSession() {
        Log.d(TAG, "clearSession: Clearing session data");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("access_token");
        editor.remove("user_id");
        editor.apply();
        Log.d(TAG, "clearSession: Session data cleared");
    }

    private void setupLoginListeners() {
        Log.d(TAG, "setupLoginListeners: Setting up login button listeners");
        // Email/Password Login
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            Log.d(TAG, "loginButton clicked: Email=" + email + ", Password length=" + password.length());

            if (email.isEmpty() || password.isEmpty()) {
                Log.w(TAG, "loginButton clicked: Empty fields detected");
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "loginButton clicked: Initiating login with email");
            loginWithEmail(email, password);
        });

        // Third-party logins (Facebook, Google, Twitter)
        binding.facebookButton.setOnClickListener(v -> {
            Log.d(TAG, "facebookButton clicked: Initiating Facebook login");
            loginWithProvider("facebook");
        });
        binding.googleButton.setOnClickListener(v -> {
            Log.d(TAG, "googleButton clicked: Initiating Google login");
            loginWithProvider("google");
        });
        binding.twitterButton.setOnClickListener(v -> {
            Log.d(TAG, "twitterButton clicked: Initiating Twitter login");
            loginWithProvider("twitter");
        });
    }

    private void loginWithEmail(String email, String password) {
        String url = getSupabaseUrl() + "/auth/v1/token?grant_type=password";
        Log.d(TAG, "loginWithEmail: Sending login request to URL: " + url);
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);
            Log.d(TAG, "loginWithEmail: JSON request body: " + json.toString());
        } catch (Exception e) {
            Log.e(TAG, "loginWithEmail: Failed to create JSON: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                json.toString()
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", getSupabaseKey())
                .addHeader("Content-Type", "application/json")
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "loginWithEmail: Login request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "loginWithEmail: Response received, code: " + response.code() + ", body: " + responseBody);
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String accessToken = jsonResponse.getString("access_token");
                        JSONObject user = jsonResponse.getJSONObject("user");
                        String userId = user.getString("id");
                        String refreshToken = jsonResponse.optString("refresh_token", null);

                        Log.d(TAG, "loginWithEmail: Login successful, accessToken=" + accessToken + ", userId=" + userId + ", refreshToken=" + refreshToken);

                        // Lưu access_token, user_id và refresh_token vào SharedPreferences
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("access_token", accessToken);
                        editor.putString("user_id", userId);
                        if (refreshToken != null) {
                            editor.putString("refresh_token", refreshToken);
                        }
                        editor.apply();
                        Log.d(TAG, "loginWithEmail: Saved tokens to SharedPreferences");

                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "loginWithEmail: Failed to parse response: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Failed to parse login response", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.w(TAG, "loginWithEmail: Login failed, response code: " + response.code() + ", body: " + responseBody);
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void loginWithProvider(String provider) {
        String url = getSupabaseUrl() + "/auth/v1/authorize?provider=" + provider;
        Log.d(TAG, "loginWithProvider: Initiating OAuth with provider: " + provider + ", URL: " + url);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", getSupabaseKey())
                .build();

        // In a real app, you'd open this URL in a WebView or browser for OAuth flow
        // For now, we'll simulate the action
        runOnUiThread(() -> Toast.makeText(this, "Login with " + provider + " initiated", Toast.LENGTH_SHORT).show());
        // TODO: Implement OAuth flow with WebView or custom tab
    }
}