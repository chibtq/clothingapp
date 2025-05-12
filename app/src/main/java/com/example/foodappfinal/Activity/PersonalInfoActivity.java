package com.example.foodappfinal.Activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.example.foodappfinal.Adapter.AddressAdapter;
import com.example.foodappfinal.Model.Address;
import com.example.foodappfinal.Model.UserProfile;
import com.example.foodappproject.R;
import com.example.foodappproject.databinding.ActivityPersonalInfoBinding;
import com.example.foodappproject.databinding.DialogAddAddressBinding;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PersonalInfoActivity extends BaseActivity {
    private static final String TAG = "PersonalInfoActivity";
    private ActivityPersonalInfoBinding binding;
    private static final String USER_PROFILES_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/user_profiles";
    private static final String ADDRESSES_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/addresses";
    private static final String STORAGE_URL = "https://fmheidqpephjpsgjuupf.supabase.co/storage/v1/object/avatar/";
    private UserProfile userProfile;
    private List<Address> addressList;
    private AddressAdapter addressAdapter;
    private ArrayAdapter<String> genderAdapter;
    private Uri selectedImageUri;
    private static final int STORAGE_PERMISSION_CODE = 100;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        Glide.with(this)
                                .load(selectedImageUri)
                                .circleCrop()
                                .into(binding.avatarImage);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPersonalInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Thông tin cá nhân");
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Thiết lập Spinner cho gender
        genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                Arrays.asList("Male", "Female", "Other"));
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.genderSpinner.setAdapter(genderAdapter);

        // Thiết lập RecyclerView cho địa chỉ
        addressList = new ArrayList<>();
        addressAdapter = new AddressAdapter(this, addressList, new AddressAdapter.OnAddressActionListener() {
            @Override
            public void onSetDefault(Address address) {
                setDefaultAddress(address);
            }

            @Override
            public void onDelete(Address address) {
                deleteAddress(address);
            }
        });
        binding.addressRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.addressRecyclerView.setAdapter(addressAdapter);

        // Sự kiện chọn ảnh
        binding.selectAvatarButton.setOnClickListener(v -> checkStoragePermission());

        // Sự kiện thêm địa chỉ
        binding.addAddressButton.setOnClickListener(v -> showAddAddressDialog());

        // Sự kiện lưu hồ sơ
        binding.saveButton.setOnClickListener(v -> saveProfile());

        loadUserProfile();
        loadAddresses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload profile data every time the activity resumes to reflect any changes
        loadUserProfile();
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_CODE);
            } else {
                openImagePicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            } else {
                openImagePicker();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied to access storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void loadUserProfile() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Please log in to view profile", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        String accessToken = getAccessToken();
        String tokenUserId = getUserIdFromToken(accessToken);
        Log.d(TAG, "loadUserProfile: Current User ID: " + userId + ", Token User ID: " + tokenUserId);
        if (!userId.equals(tokenUserId)) {
            Log.e(TAG, "loadUserProfile: Token user ID does not match current user ID");
            runOnUiThread(() -> {
                Toast.makeText(PersonalInfoActivity.this, "Session mismatch. Please log in again.", Toast.LENGTH_LONG).show();
                logout();
            });
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
                runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "Profile Response: " + responseData);
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Type userProfileType = new TypeToken<List<UserProfile>>(){}.getType();
                    List<UserProfile> userProfiles = gson.fromJson(responseData, userProfileType);

                    if (userProfiles != null && !userProfiles.isEmpty()) {
                        userProfile = userProfiles.get(0);
                        runOnUiThread(() -> {
                            binding.usernameEdit.setText(userProfile.getUsername() != null ? userProfile.getUsername() : "");
                            binding.emailText.setText(userProfile.getEmail() != null ? userProfile.getEmail() : "email@example.com");
                            String phoneNumber = userProfile.getPhoneNumber();
                            binding.phoneEdit.setText(phoneNumber != null ? phoneNumber : "");
                            Log.d(TAG, "Phone Number Loaded: " + phoneNumber);

                            if (userProfile.getBirthDate() != null && !userProfile.getBirthDate().isEmpty()) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(sdf.parse(userProfile.getBirthDate()));
                                    binding.birthDatePicker.init(
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH),
                                            null
                                    );
                                    Log.d(TAG, "Birth Date Loaded: " + userProfile.getBirthDate());
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing birth date: " + e.getMessage());
                                    Calendar defaultCal = Calendar.getInstance();
                                    binding.birthDatePicker.init(
                                            defaultCal.get(Calendar.YEAR),
                                            defaultCal.get(Calendar.MONTH),
                                            defaultCal.get(Calendar.DAY_OF_MONTH),
                                            null
                                    );
                                }
                            } else {
                                Log.d(TAG, "Birth Date is null or empty");
                                Calendar defaultCal = Calendar.getInstance();
                                binding.birthDatePicker.init(
                                        defaultCal.get(Calendar.YEAR),
                                        defaultCal.get(Calendar.MONTH),
                                        defaultCal.get(Calendar.DAY_OF_MONTH),
                                        null
                                );
                            }

                            int genderPosition = genderAdapter.getPosition(userProfile.getGender());
                            binding.genderSpinner.setSelection(genderPosition != -1 ? genderPosition : 0);

                            String avatarUrl = userProfile.getAvatarUrl();
                            Log.d(TAG, "Avatar URL Loaded: " + avatarUrl);
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(PersonalInfoActivity.this)
                                        .load(avatarUrl + "?t=" + System.currentTimeMillis()) // Add timestamp to avoid cache
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_profile_placeholder)
                                        .error(R.drawable.ic_profile_placeholder)
                                        .into(binding.avatarImage);
                            } else {
                                binding.avatarImage.setImageResource(R.drawable.ic_profile_placeholder);
                            }
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "No profile data found", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to load profile: HTTP " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void loadAddresses() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        String accessToken = getAccessToken();
        String tokenUserId = getUserIdFromToken(accessToken);
        Log.d(TAG, "loadAddresses: Current User ID: " + userId + ", Token User ID: " + tokenUserId);
        if (!userId.equals(tokenUserId)) {
            Log.e(TAG, "loadAddresses: Token user ID does not match current user ID");
            runOnUiThread(() -> {
                Toast.makeText(PersonalInfoActivity.this, "Session mismatch. Please log in again.", Toast.LENGTH_LONG).show();
                logout();
            });
            return;
        }

        String url = ADDRESSES_URL + "?user_id=eq." + userId + "&select=*";
        Request request = getRequestBuilder()
                .url(url)
                .get()
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to load addresses", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Type addressType = new TypeToken<List<Address>>(){}.getType();
                    List<Address> addresses = gson.fromJson(responseData, addressType);

                    addressList.clear();
                    if (addresses != null) {
                        addressList.addAll(addresses);
                    }
                    runOnUiThread(() -> {
                        addressAdapter.notifyDataSetChanged();
                        binding.addressRecyclerView.setVisibility(addressList.isEmpty() ? View.GONE : View.VISIBLE);
                    });
                }
            }
        });
    }

    private void showAddAddressDialog() {
        DialogAddAddressBinding dialogBinding = DialogAddAddressBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        dialogBinding.saveButton.setOnClickListener(v -> {
            String addressName = dialogBinding.addressNameEdit.getText().toString().trim();
            String addressLine = dialogBinding.addressLineEdit.getText().toString().trim();
            String city = dialogBinding.cityEdit.getText().toString().trim();
            String country = dialogBinding.countryEdit.getText().toString().trim();
            String postalCode = dialogBinding.postalCodeEdit.getText().toString().trim();
            boolean isDefault = dialogBinding.defaultCheck.isChecked();

            if (addressLine.isEmpty() || city.isEmpty() || country.isEmpty()) {
                Toast.makeText(this, "Please fill all required address fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Address newAddress = new Address();
            newAddress.setAddressId(UUID.randomUUID().toString());
            newAddress.setUserId(getCurrentUserId());
            newAddress.setAddressName(addressName);
            newAddress.setAddressLine(addressLine);
            newAddress.setCity(city);
            newAddress.setCountry(country);
            newAddress.setPostalCode(postalCode.isEmpty() ? null : postalCode);
            newAddress.setDefault(isDefault);

            addAddress(newAddress, dialog);
        });

        dialogBinding.cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void addAddress(Address address, AlertDialog dialog) {
        String accessToken = getAccessToken();
        String tokenUserId = getUserIdFromToken(accessToken);
        String userId = getCurrentUserId();
        Log.d(TAG, "addAddress: Current User ID: " + userId + ", Token User ID: " + tokenUserId);
        if (!userId.equals(tokenUserId)) {
            Log.e(TAG, "addAddress: Token user ID does not match current user ID");
            runOnUiThread(() -> {
                Toast.makeText(PersonalInfoActivity.this, "Session mismatch. Please log in again.", Toast.LENGTH_LONG).show();
                logout();
            });
            return;
        }

        String addressJson = new Gson().toJson(address);
        RequestBody addressBody = RequestBody.create(addressJson, MediaType.parse("application/json"));
        Request addressRequest = getRequestBuilder()
                .url(ADDRESSES_URL)
                .post(addressBody)
                .build();

        getOkHttpClient().newCall(addressRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to add address", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    if (address.isDefault()) {
                        updateOtherAddressesToNonDefault(address.getAddressId());
                    } else {
                        addressList.add(address);
                        runOnUiThread(() -> {
                            addressAdapter.notifyDataSetChanged();
                            binding.addressRecyclerView.setVisibility(View.VISIBLE);
                            dialog.dismiss();
                            Toast.makeText(PersonalInfoActivity.this, "Address added successfully", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to add address: HTTP " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void setDefaultAddress(Address address) {
        String accessToken = getAccessToken();
        String tokenUserId = getUserIdFromToken(accessToken);
        String userId = getCurrentUserId();
        Log.d(TAG, "setDefaultAddress: Current User ID: " + userId + ", Token User ID: " + tokenUserId);
        if (!userId.equals(tokenUserId)) {
            Log.e(TAG, "setDefaultAddress: Token user ID does not match current user ID");
            runOnUiThread(() -> {
                Toast.makeText(PersonalInfoActivity.this, "Session mismatch. Please log in again.", Toast.LENGTH_LONG).show();
                logout();
            });
            return;
        }

        String addressJson = String.format("{\"is_default\":true}");
        RequestBody addressBody = RequestBody.create(addressJson, MediaType.parse("application/json"));
        Request addressRequest = getRequestBuilder()
                .url(ADDRESSES_URL + "?address_id=eq." + address.getAddressId())
                .patch(addressBody)
                .build();

        getOkHttpClient().newCall(addressRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to set default address", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    updateOtherAddressesToNonDefault(address.getAddressId());
                } else {
                    runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to set default address: HTTP " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updateOtherAddressesToNonDefault(String defaultAddressId) {
        String accessToken = getAccessToken();
        String tokenUserId = getUserIdFromToken(accessToken);
        String userId = getCurrentUserId();
        Log.d(TAG, "updateOtherAddressesToNonDefault: Current User ID: " + userId + ", Token User ID: " + tokenUserId);
        if (!userId.equals(tokenUserId)) {
            Log.e(TAG, "updateOtherAddressesToNonDefault: Token user ID does not match current user ID");
            runOnUiThread(() -> {
                Toast.makeText(PersonalInfoActivity.this, "Session mismatch. Please log in again.", Toast.LENGTH_LONG).show();
                logout();
            });
            return;
        }

        String addressJson = String.format("{\"is_default\":false}");
        RequestBody addressBody = RequestBody.create(addressJson, MediaType.parse("application/json"));
        Request addressRequest = getRequestBuilder()
                .url(ADDRESSES_URL + "?user_id=eq." + userId + "&address_id=neq." + defaultAddressId)
                .patch(addressBody)
                .build();

        getOkHttpClient().newCall(addressRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to update other addresses", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    for (Address addr : addressList) {
                        addr.setDefault(addr.getAddressId().equals(defaultAddressId));
                    }
                    runOnUiThread(() -> {
                        addressAdapter.notifyDataSetChanged();
                        Toast.makeText(PersonalInfoActivity.this, "Default address set successfully", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void deleteAddress(Address address) {
        String accessToken = getAccessToken();
        String tokenUserId = getUserIdFromToken(accessToken);
        String userId = getCurrentUserId();
        Log.d(TAG, "deleteAddress: Current User ID: " + userId + ", Token User ID: " + tokenUserId);
        if (!userId.equals(tokenUserId)) {
            Log.e(TAG, "deleteAddress: Token user ID does not match current user ID");
            runOnUiThread(() -> {
                Toast.makeText(PersonalInfoActivity.this, "Session mismatch. Please log in again.", Toast.LENGTH_LONG).show();
                logout();
            });
            return;
        }

        Request addressRequest = getRequestBuilder()
                .url(ADDRESSES_URL + "?address_id=eq." + address.getAddressId())
                .delete()
                .build();

        getOkHttpClient().newCall(addressRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to delete address", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    addressList.remove(address);
                    runOnUiThread(() -> {
                        addressAdapter.notifyDataSetChanged();
                        binding.addressRecyclerView.setVisibility(addressList.isEmpty() ? View.GONE : View.VISIBLE);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to delete address: HTTP " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void saveProfile() {
        Log.d(TAG, "saveProfile called");
        String username = binding.usernameEdit.getText().toString().trim();
        String phone = binding.phoneEdit.getText().toString().trim();
        int year = binding.birthDatePicker.getYear();
        int month = binding.birthDatePicker.getMonth();
        int day = binding.birthDatePicker.getDayOfMonth();
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String birthDate = sdf.format(cal.getTime());
        String gender = binding.genderSpinner.getSelectedItem().toString();

        if (username.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String accessToken = getAccessToken();
        String tokenUserId = getUserIdFromToken(accessToken);
        String userId = getCurrentUserId();
        Log.d(TAG, "saveProfile: Current User ID: " + userId + ", Token User ID: " + tokenUserId);
        if (!userId.equals(tokenUserId)) {
            Log.e(TAG, "saveProfile: Token user ID does not match current user ID");
            runOnUiThread(() -> {
                Toast.makeText(PersonalInfoActivity.this, "Session mismatch. Please log in again.", Toast.LENGTH_LONG).show();
                logout();
            });
            return;
        }

        if (selectedImageUri != null) {
            uploadAvatar(() -> updateProfile(username, phone, birthDate, gender));
        } else {
            updateProfile(username, phone, birthDate, gender);
        }
    }

    private String getUserIdFromToken(String accessToken) {
        if (accessToken == null) {
            Log.e(TAG, "getUserIdFromToken: Access token is null");
            return null;
        }
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length != 3) {
                Log.e(TAG, "getUserIdFromToken: Invalid JWT token format");
                return null;
            }
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
            JsonObject payloadJson = JsonParser.parseString(payload).getAsJsonObject();
            String userId = payloadJson.get("sub").getAsString();
            Log.d(TAG, "getUserIdFromToken: Extracted User ID from token: " + userId);
            return userId;
        } catch (Exception e) {
            Log.e(TAG, "getUserIdFromToken: Failed to decode JWT token: " + e.getMessage());
            return null;
        }
    }

    private void uploadAvatar(Runnable onSuccess) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String accessToken = getAccessToken();
        String tokenUserId = getUserIdFromToken(accessToken);
        Log.d(TAG, "uploadAvatar: Current User ID: " + userId + ", Token User ID: " + tokenUserId);
        if (!userId.equals(tokenUserId)) {
            Log.e(TAG, "uploadAvatar: Token user ID does not match current user ID");
            runOnUiThread(() -> {
                Toast.makeText(PersonalInfoActivity.this, "Session mismatch. Please log in again.", Toast.LENGTH_LONG).show();
                logout();
            });
            return;
        }

        String fileName = userId + ".jpg";
        String filePath = STORAGE_URL + fileName;
        Log.d(TAG, "uploadAvatar: Uploading to URL: " + filePath + ", File Name: " + fileName + ", User ID: " + userId);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName,
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .build();

            Request request;
            try {
                Log.d(TAG, "uploadAvatar: Access Token (partial): " + (accessToken != null ? accessToken.substring(0, 10) + "..." : "null"));
                request = getRequestBuilder()
                        .url(filePath)
                        .addHeader("x-upsert", "true")
                        .addHeader("Content-Type", "multipart/form-data")
                        .post(requestBody)
                        .build();
                Log.d(TAG, "uploadAvatar: Request Headers: " + request.headers().toString());
            } catch (IllegalStateException e) {
                Log.e(TAG, "uploadAvatar: Access token is missing, attempting to refresh: " + e.getMessage());
                refreshToken(new RefreshTokenCallback() {
                    @Override
                    public void onSuccess(String newAccessToken) {
                        Log.d(TAG, "uploadAvatar: Token refreshed successfully, retrying upload");
                        Request retryRequest = getRequestBuilder()
                                .url(filePath)
                                .addHeader("x-upsert", "true")
                                .addHeader("Content-Type", "multipart/form-data")
                                .post(requestBody)
                                .build();
                        Log.d(TAG, "uploadAvatar: Retry Request Headers: " + retryRequest.headers().toString());
                        getOkHttpClient().newCall(retryRequest).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to upload avatar: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String responseBody = response.body() != null ? response.body().string() : "";
                                Log.d(TAG, "uploadAvatar: Retry Response Headers: " + response.headers().toString());
                                Log.d(TAG, "uploadAvatar: Retry response code: " + response.code() + ", body: " + responseBody);
                                if (response.isSuccessful()) {
                                    // Update the UI with the new avatar URL after successful upload
                                    runOnUiThread(() -> {
                                        String newAvatarUrl = STORAGE_URL + userId + ".jpg";
                                        Glide.with(PersonalInfoActivity.this)
                                                .load(newAvatarUrl + "?t=" + System.currentTimeMillis()) // Add timestamp to avoid cache
                                                .circleCrop()
                                                .placeholder(R.drawable.ic_profile_placeholder)
                                                .error(R.drawable.ic_profile_placeholder)
                                                .into(binding.avatarImage);
                                        Log.d(TAG, "uploadAvatar: UI updated with new avatar URL: " + newAvatarUrl);
                                    });
                                    onSuccess.run();
                                } else {
                                    runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to upload avatar after retry: HTTP " + response.code() + " - " + responseBody, Toast.LENGTH_LONG).show());
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "uploadAvatar: Failed to refresh token: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show());
                    }
                });
                return;
            }

            getOkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to upload avatar: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "uploadAvatar: Response Headers: " + response.headers().toString());
                    Log.d(TAG, "uploadAvatar: Response code: " + response.code() + ", body: " + responseBody);
                    if (response.isSuccessful()) {
                        // Update the UI with the new avatar URL after successful upload
                        runOnUiThread(() -> {
                            String newAvatarUrl = STORAGE_URL + userId + ".jpg";
                            Glide.with(PersonalInfoActivity.this)
                                    .load(newAvatarUrl + "?t=" + System.currentTimeMillis()) // Add timestamp to avoid cache
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .error(R.drawable.ic_profile_placeholder)
                                    .into(binding.avatarImage);
                            Log.d(TAG, "uploadAvatar: UI updated with new avatar URL: " + newAvatarUrl);
                        });
                        onSuccess.run();
                    } else if (response.code() == 401) {
                        Log.w(TAG, "uploadAvatar: Unauthorized, attempting token refresh");
                        refreshToken(new RefreshTokenCallback() {
                            @Override
                            public void onSuccess(String newAccessToken) {
                                Log.d(TAG, "uploadAvatar: Token refreshed, retrying upload");
                                Request retryRequest = getRequestBuilder()
                                        .url(filePath)
                                        .addHeader("x-upsert", "true")
                                        .addHeader("Content-Type", "multipart/form-data")
                                        .post(requestBody)
                                        .build();
                                Log.d(TAG, "uploadAvatar: Retry Request Headers: " + retryRequest.headers().toString());
                                getOkHttpClient().newCall(retryRequest).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to upload avatar: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        String responseBody = response.body() != null ? response.body().string() : "";
                                        Log.d(TAG, "uploadAvatar: Retry Response Headers: " + response.headers().toString());
                                        Log.d(TAG, "uploadAvatar: Retry response code: " + response.code() + ", body: " + responseBody);
                                        if (response.isSuccessful()) {
                                            // Update the UI with the new avatar URL after successful upload
                                            runOnUiThread(() -> {
                                                String newAvatarUrl = STORAGE_URL + userId + ".jpg";
                                                Glide.with(PersonalInfoActivity.this)
                                                        .load(newAvatarUrl + "?t=" + System.currentTimeMillis()) // Add timestamp to avoid cache
                                                        .circleCrop()
                                                        .placeholder(R.drawable.ic_profile_placeholder)
                                                        .error(R.drawable.ic_profile_placeholder)
                                                        .into(binding.avatarImage);
                                                Log.d(TAG, "uploadAvatar: UI updated with new avatar URL: " + newAvatarUrl);
                                            });
                                            onSuccess.run();
                                        } else {
                                            runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to upload avatar after retry: HTTP " + response.code() + " - " + responseBody, Toast.LENGTH_LONG).show());
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "uploadAvatar: Failed to refresh token: " + e.getMessage());
                                runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show());
                            }
                        });
                    } else if (response.code() == 403) {
                        Log.w(TAG, "uploadAvatar: Unauthorized: HTTP " + response.code() + ", body: " + responseBody);
                        runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to upload avatar: Permission denied", Toast.LENGTH_LONG).show());
                    } else {
                        runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to upload avatar: HTTP " + response.code() + " - " + responseBody, Toast.LENGTH_LONG).show());
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateProfile(String username, String phone, String birthDate, String gender) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String accessToken = getAccessToken();
        String tokenUserId = getUserIdFromToken(accessToken);
        Log.d(TAG, "updateProfile: Current User ID: " + userId + ", Token User ID: " + tokenUserId);
        if (!userId.equals(tokenUserId)) {
            Log.e(TAG, "updateProfile: Token user ID does not match current user ID");
            runOnUiThread(() -> {
                Toast.makeText(PersonalInfoActivity.this, "Session mismatch. Please log in again.", Toast.LENGTH_LONG).show();
                logout();
            });
            return;
        }

        JSONObject profileJson = new JSONObject();
        try {
            profileJson.put("username", username);
            if (phone != null && !phone.isEmpty()) {
                profileJson.put("phone_number", phone);
            }
            if (birthDate != null && !birthDate.equals(userProfile.getBirthDate())) {
                profileJson.put("birth_date", birthDate);
            }
            if (gender != null && !gender.equals(userProfile.getGender())) {
                profileJson.put("gender", gender);
            }
            if (selectedImageUri != null) {
                String avatarUrl = STORAGE_URL + userId + ".jpg";
                profileJson.put("avatar_url", avatarUrl);
            }
            Log.d(TAG, "Profile Update JSON: " + profileJson.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error creating profile JSON: " + e.getMessage());
            Toast.makeText(this, "Error preparing profile update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody profileBody = RequestBody.create(profileJson.toString(), MediaType.parse("application/json"));
        Request profileRequest = getRequestBuilder()
                .url(USER_PROFILES_URL + "?user_id=eq." + userId)
                .patch(profileBody)
                .build();

        getOkHttpClient().newCall(profileRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(PersonalInfoActivity.this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
                        // Reload the user profile to update the UI with the latest data
                        loadUserProfile();
                        // Finish the activity to return to ProfileActivity, which will reload on resume
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(PersonalInfoActivity.this, "Failed to save profile: HTTP " + response.code() + " - " + responseBody, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}