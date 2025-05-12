package com.example.foodappfinal.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabsIntent;

import com.example.foodappproject.databinding.ActivityRegisterBinding;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends BaseActivity {
    private ActivityRegisterBinding binding;
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Xử lý deep link từ OAuth callback
        handleDeepLink(getIntent());

        // Chuyển hướng sang LoginActivity
        binding.loginTextView.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });

        // Đăng ký bằng email/password
        binding.signUpButton.setOnClickListener(v -> {
            String username = binding.usernameEditText.getText().toString().trim();
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(username, email, password);
        });

        // Đăng nhập bằng Google
        binding.googleButton.setOnClickListener(v -> signInWithProvider("google"));

        // Đăng nhập bằng Facebook
        binding.facebookButton.setOnClickListener(v -> signInWithProvider("facebook"));

        // Đăng nhập bằng Twitter
        binding.twitterButton.setOnClickListener(v -> signInWithProvider("twitter"));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleDeepLink(intent);
    }

    // Xử lý deep link từ OAuth callback
    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            Log.d(TAG, "Deep Link URI: " + data.toString());
            if (data.toString().startsWith("yourapp://auth/callback")) {
                String accessToken = data.getQueryParameter("access_token");
                String error = data.getQueryParameter("error_description");

                Log.d(TAG, "Access Token: " + accessToken);
                Log.d(TAG, "Error Description: " + (error != null ? error : "None"));

                if (error != null) {
                    Toast.makeText(this, "Authentication failed: " + error, Toast.LENGTH_LONG).show();
                    return;
                }

                if (accessToken != null) {
                    fetchUserInfo(accessToken);
                } else {
                    Toast.makeText(this, "Failed to retrieve access token", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Log.d(TAG, "No deep link data received");
        }
    }

    // Lấy thông tin người dùng từ Supabase sau khi đăng nhập OAuth
    private void fetchUserInfo(String accessToken) {
        String userUrl = getSupabaseUrl() + "/auth/v1/user";
        Request request = new Request.Builder()
                .url(userUrl)
                .get()
                .addHeader("apikey", getSupabaseKey())
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Failed to fetch user info: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "User Info Response: " + responseBody);
                if (response.isSuccessful()) {
                    try {
                        JSONObject userJson = new JSONObject(responseBody);
                        String userId = userJson.getString("id");
                        String email = userJson.optString("email", "unknown@example.com");
                        String username = userJson.optString("user_metadata", "{}")
                                .contains("name") ? userJson.getJSONObject("user_metadata").optString("name", "OAuthUser") : "OAuthUser";

                        saveUserProfile(userId, username, email, null, null, null);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing user info: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Error parsing user info: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Failed to fetch user info: " + responseBody, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // Đăng ký bằng email/password
    private void registerUser(String username, String email, String password) {
        String authUrl = getSupabaseUrl() + "/auth/v1/signup";
        JSONObject authJson = new JSONObject();
        try {
            authJson.put("email", email);
            authJson.put("password", password);
            JSONObject metadata = new JSONObject();
            metadata.put("username", username);
            authJson.put("user_metadata", metadata);
        } catch (Exception e) {
            Log.e(TAG, "Error creating auth JSON: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Failed to prepare registration: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        RequestBody authBody = RequestBody.create(
                MediaType.parse("application/json"),
                authJson.toString()
        );

        Request authRequest = new Request.Builder()
                .url(authUrl)
                .post(authBody)
                .addHeader("apikey", getSupabaseKey())
                .addHeader("Content-Type", "application/json")
                .build();

        getOkHttpClient().newCall(authRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Signup failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Signup Response (Full): " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        JSONObject responseJson = new JSONObject(responseBody);
                        if (!responseJson.has("id")) {
                            Log.e(TAG, "User ID missing in response: " + responseBody);
                            runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "User ID not found in response", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        String userId = responseJson.getString("id");
                        // Lưu thông tin vào user_profiles với role mặc định là 'user'
                        saveUserProfile(userId, username, email, null, null, null);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing signup response: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    try {
                        JSONObject errorJson = new JSONObject(responseBody);
                        String errorCode = errorJson.optString("error_code", "");
                        String errorMessage = errorJson.optString("msg", "Registration failed");

                        if ("unexpected_failure".equals(errorCode) && errorMessage.contains("Error sending confirmation email")) {
                            errorMessage = "Unable to send confirmation email. Please check your email provider settings or try again later.";
                        } else if ("over_email_send_rate_limit".equals(errorCode)) {
                            errorMessage = "Too many email requests. Please wait a while and try again.";
                        } else if ("invalid_parameters".equals(errorCode)) {
                            errorMessage = "Invalid email or password. Please check your input.";
                        } else if ("user_already_exists".equals(errorCode)) {
                            errorMessage = "This email is already registered. Please login or use a different email.";
                        }

                        final String finalErrorMessage = errorMessage;
                        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, finalErrorMessage, Toast.LENGTH_LONG).show());
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Registration failed: " + responseBody, Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    // Bắt đầu đăng nhập OAuth với nhà cung cấp
    private void signInWithProvider(String provider) {
        String authUrl = getSupabaseUrl() + "/auth/v1/authorize?provider=" + provider + "&redirect_to=yourapp://auth/callback";
        Log.d(TAG, "OAuth URL: " + authUrl);

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        try {
            customTabsIntent.launchUrl(this, Uri.parse(authUrl));
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch Custom Tab: " + e.getMessage());
            Toast.makeText(this, "Failed to open browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Lưu thông tin người dùng vào user_profiles
    private void saveUserProfile(String userId, String username, String email, String phoneNumber, String birthDate, String gender) {
        String profileUrl = getSupabaseUrl() + "/rest/v1/user_profiles";
        JSONObject profileJson = new JSONObject();
        try {
            profileJson.put("user_id", userId);
            profileJson.put("username", username);
            if (email != null && !email.isEmpty()) {
                profileJson.put("email", email);
            }
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                profileJson.put("phone_number", phoneNumber);
            }
            if (birthDate != null && !birthDate.isEmpty()) {
                profileJson.put("birth_date", birthDate);
            }
            if (gender != null && !gender.isEmpty()) {
                profileJson.put("gender", gender);
            }
            profileJson.put("role", "user"); // Default role for new users
        } catch (Exception e) {
            Log.e(TAG, "Error creating profile JSON: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Failed to prepare profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        RequestBody profileBody = RequestBody.create(
                MediaType.parse("application/json"),
                profileJson.toString()
        );

        Request profileRequest = new Request.Builder()
                .url(profileUrl)
                .post(profileBody)
                .addHeader("apikey", getSupabaseServiceKey())
                .addHeader("Authorization", "Bearer " + getSupabaseServiceKey())
                .addHeader("Content-Type", "application/json")
                .build();

        getOkHttpClient().newCall(profileRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to save profile: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String profileResponse = response.body().string();
                Log.d(TAG, "Profile Save Response: " + profileResponse);
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_LONG).show();
                        binding.emailEditText.setText("");
                        binding.passwordEditText.setText("");
                        binding.confirmPasswordEditText.setText("");
                        binding.usernameEditText.setText("");
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Failed to save profile: " + profileResponse, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}