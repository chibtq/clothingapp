package com.example.foodappfinal.Activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.foodappproject.databinding.ActivityForgetPasswordBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ForgetPasswordActivity extends BaseActivity {
    private static final String TAG = "ForgetPasswordActivity";
    private ActivityForgetPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Back to Login
        binding.backToLoginTextView.setOnClickListener(v -> {
            startActivity(new Intent(ForgetPasswordActivity.this, LoginActivity.class));
            finish();
        });

        // Request OTP
        binding.requestOtpButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestOtp(email);
            }
        });

        // Reset Password
        binding.resetPasswordButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            String otp = binding.otpEditText.getText().toString().trim();
            String newPassword = binding.newPasswordEditText.getText().toString().trim();
            String confirmNewPassword = binding.confirmNewPasswordEditText.getText().toString().trim();

            if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmNewPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyOtpAndResetPassword(email, otp, newPassword);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void requestOtp(String email) {
        // Step 1: Generate a 6-digit OTP
        String otp = String.valueOf(new Random().nextInt(999999 - 100000) + 100000);

        // Step 2: Calculate created_at and expires_at (in UTC)
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(10, ChronoUnit.MINUTES);

        // Step 3: Store OTP in Supabase
        String otpUrl = getSupabaseUrl() + "/rest/v1/otp_verification";
        JSONObject otpJson = new JSONObject();
        try {
            otpJson.put("email", email);
            otpJson.put("otp", otp);
            otpJson.put("created_at", createdAt.toString());
            otpJson.put("expires_at", expiresAt.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error creating OTP JSON: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Failed to prepare OTP: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        RequestBody otpBody = RequestBody.create(
                MediaType.parse("application/json"),
                otpJson.toString()
        );

        Request otpRequest = new Request.Builder()
                .url(otpUrl)
                .post(otpBody)
                .addHeader("apikey", getSupabaseKey())
                .addHeader("Content-Type", "application/json")
                .build();

        getOkHttpClient().newCall(otpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to generate OTP: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Store OTP Response: " + responseBody);
                if (response.isSuccessful()) {
                    // Step 4: Send OTP via email using SendGrid API
                    sendOtpEmail(email, otp);
                } else {
                    runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to store OTP: " + responseBody, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void sendOtpEmail(String email, String otp) {
        String sendGridUrl = "https://api.sendgrid.com/v3/mail/send";
        JSONObject payload = new JSONObject();
        try {
            JSONObject personalizations = new JSONObject();
            personalizations.put("to", new JSONArray().put(new JSONObject().put("email", email)));
            payload.put("personalizations", new JSONArray().put(personalizations));
            payload.put("from", new JSONObject().put("email", "TuanLA.B21AT205@stu.ptit.edu.vn"));
            payload.put("subject", "Your OTP for Password Reset");
            payload.put("content", new JSONArray()
                    .put(new JSONObject()
                            .put("type", "text/plain")
                            .put("value", "Your OTP is: " + otp + ". It will expire in 10 minutes.")
                    )
                    .put(new JSONObject()
                            .put("type", "text/html")
                            .put("value", "<p>Your OTP is: <strong>" + otp + "</strong>. It will expire in 10 minutes.</p>")
                    )
            );
        } catch (Exception e) {
            Log.e(TAG, "Error preparing email: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Failed to prepare email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            return;
        }

        RequestBody emailBody = RequestBody.create(
                MediaType.parse("application/json"),
                payload.toString()
        );

        Request emailRequest = new Request.Builder()
                .url(sendGridUrl)
                .post(emailBody)
                .addHeader("Authorization", "Bearer " + getSendGridApiKey())
                .addHeader("Content-Type", "application/json")
                .build();

        getOkHttpClient().newCall(emailRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to send OTP email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String emailResponse = response.body().string();
                Log.d(TAG, "SendGrid Response: " + emailResponse);
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "OTP sent to your email. Check your inbox!", Toast.LENGTH_LONG).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to send OTP email: " + emailResponse, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void verifyOtpAndResetPassword(String email, String otp, String newPassword) {
        String otpUrl = getSupabaseUrl() + "/rest/v1/otp_verification?email=eq." + email + "&otp=eq." + otp;
        Request request = new Request.Builder()
                .url(otpUrl)
                .get()
                .addHeader("apikey", getSupabaseKey())
                .addHeader("Content-Type", "application/json")
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to verify OTP: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Verify OTP Response: " + responseBody);
                if (response.isSuccessful()) {
                    try {
                        JSONArray otpArray = new JSONArray(responseBody);
                        if (otpArray.length() == 0) {
                            runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Invalid or expired OTP", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        JSONObject otpRecord = otpArray.getJSONObject(0);
                        String storedOtp = otpRecord.getString("otp");
                        String expiresAtStr = otpRecord.getString("expires_at");

                        if (!storedOtp.equals(otp)) {
                            runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.parse(expiresAtStr);
                        Instant expiresAt = zonedDateTime.toInstant();
                        Instant now = Instant.now();
                        if (now.isAfter(expiresAt)) {
                            runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "OTP has expired", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        // OTP hợp lệ, gọi resetPassword
                        resetPassword(email, newPassword, otp);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing OTP response: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Error parsing OTP response: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to verify OTP: " + responseBody, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void resetPassword(String email, String newPassword, String otp) {
        // Step 1: Lấy user_id từ bảng user_profiles dựa trên email
        String usersUrl = getSupabaseUrl() + "/rest/v1/user_profiles?select=user_id&email=eq." + email;
        Request usersRequest = new Request.Builder()
                .url(usersUrl)
                .get()
                .addHeader("apikey", getSupabaseKey())
                .addHeader("Authorization", "Bearer " + getSupabaseServiceKey())
                .addHeader("Content-Type", "application/json")
                .build();

        getOkHttpClient().newCall(usersRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch user: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to fetch user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Fetch User Response: " + responseBody);
                if (response.isSuccessful()) {
                    try {
                        JSONArray usersArray = new JSONArray(responseBody);
                        if (usersArray.length() == 0) {
                            runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "No account found with this email", Toast.LENGTH_SHORT).show());
                            return;
                        }
                        JSONObject user = usersArray.getJSONObject(0);
                        String userId = user.getString("user_id");

                        // Step 2: Cập nhật mật khẩu
                        String authUrl = getSupabaseUrl() + "/auth/v1/admin/users/" + userId;
                        JSONObject payload = new JSONObject();
                        try {
                            payload.put("password", newPassword);
                            Log.d(TAG, "Update Password Payload: " + payload.toString());
                        } catch (Exception e) {
                            Log.e(TAG, "Error preparing password update: " + e.getMessage());
                            runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to prepare password update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            return;
                        }

                        RequestBody body = RequestBody.create(
                                MediaType.parse("application/json"),
                                payload.toString()
                        );

                        // Sử dụng PUT thay vì PATCH
                        Request updateRequest = new Request.Builder()
                                .url(authUrl)
                                .put(body) // Thay .patch thành .put
                                .addHeader("apikey", getSupabaseServiceKey())
                                .addHeader("Authorization", "Bearer " + getSupabaseServiceKey())
                                .addHeader("Content-Type", "application/json")
                                .build();

                        getOkHttpClient().newCall(updateRequest).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.e(TAG, "Failed to reset password: " + e.getMessage());
                                runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to reset password: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String updateResponseBody = response.body() != null ? response.body().string() : "No response body";
                                Log.d(TAG, "Update Password Response - Status Code: " + response.code() + ", Body: " + updateResponseBody);
                                if (response.isSuccessful()) {
                                    // Step 3: Xóa OTP sau khi đổi mật khẩu thành công
                                    deleteOtp(email, otp);
                                } else {
                                    runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to reset password: " + updateResponseBody, Toast.LENGTH_SHORT).show());
                                }
                            }
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing user response: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Error parsing user response: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to fetch user: " + responseBody, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void deleteOtp(String email, String otp) {
        String otpUrl = getSupabaseUrl() + "/rest/v1/otp_verification?email=eq." + email + "&otp=eq." + otp;
        Request request = new Request.Builder()
                .url(otpUrl)
                .delete()
                .addHeader("apikey", getSupabaseKey())
                .addHeader("Content-Type", "application/json")
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to delete OTP: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to delete OTP: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Delete OTP Response: " + responseBody);
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ForgetPasswordActivity.this, "Password reset successfully. Please login with your new password.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(ForgetPasswordActivity.this, LoginActivity.class));
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ForgetPasswordActivity.this, "Failed to delete OTP: " + responseBody, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}