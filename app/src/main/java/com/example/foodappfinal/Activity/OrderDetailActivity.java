package com.example.foodappfinal.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.foodappfinal.Model.Address;
import com.example.foodappfinal.Model.Order;
import com.example.foodappfinal.Model.OrderItem;
import com.example.foodappfinal.Model.UserProfile;
import com.example.foodappproject.R;
import com.example.foodappproject.databinding.ActivityOrderConfirmationBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class OrderDetailActivity extends BaseActivity {
    private static final String TAG = "OrderDetailActivity";
    private ActivityOrderConfirmationBinding binding;
    private static final String ORDERS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/orders";
    private static final String ORDER_ITEMS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/order_items";
    private static final String ADDRESSES_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/addresses";
    private static final String USER_PROFILES_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/user_profiles";
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderConfirmationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("order_id");
        if (orderId == null) {
            Toast.makeText(this, "Invalid order ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.backButton.setOnClickListener(v -> finish());

        loadOrderDetails();
    }

    private void loadOrderDetails() {
        // Bước 1: Lấy thông tin đơn hàng từ bảng orders
        String orderUrl = ORDERS_URL + "?order_id=eq." + orderId;
        Request orderRequest = getRequestBuilder()
                .url(orderUrl)
                .get()
                .build();

        getOkHttpClient().newCall(orderRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, "Failed to load order details", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Type orderType = new TypeToken<List<Order>>(){}.getType();
                    List<Order> orders = gson.fromJson(responseData, orderType);

                    if (orders == null || orders.isEmpty()) {
                        runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, "Order not found", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    Order order = orders.get(0);
                    String userId = order.getUserId();
                    String addressId = order.getAddressId();

                    // Bước 2: Lấy thông tin người dùng từ bảng user_profiles
                    String userUrl = USER_PROFILES_URL + "?user_id=eq." + userId;
                    Request userRequest = getRequestBuilder()
                            .url(userUrl)
                            .get()
                            .build();

                    getOkHttpClient().newCall(userRequest).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, "Failed to load user info", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onResponse(Call call, Response userResponse) throws IOException {
                            String userResponseData = userResponse.body().string();
                            if (userResponse.isSuccessful()) {
                                Type userProfileType = new TypeToken<List<UserProfile>>(){}.getType();
                                List<UserProfile> userProfiles = gson.fromJson(userResponseData, userProfileType);
                                UserProfile userProfile = userProfiles != null && !userProfiles.isEmpty() ? userProfiles.get(0) : null;

                                // Bước 3: Lấy địa chỉ giao hàng từ bảng addresses
                                String addressUrl = ADDRESSES_URL + "?address_id=eq." + addressId;
                                Request addressRequest = getRequestBuilder()
                                        .url(addressUrl)
                                        .get()
                                        .build();

                                getOkHttpClient().newCall(addressRequest).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, "Failed to load address", Toast.LENGTH_SHORT).show());
                                    }

                                    @Override
                                    public void onResponse(Call call, Response addressResponse) throws IOException {
                                        String addressResponseData = addressResponse.body().string();
                                        if (addressResponse.isSuccessful()) {
                                            Type addressType = new TypeToken<List<Address>>(){}.getType();
                                            List<Address> addresses = gson.fromJson(addressResponseData, addressType);
                                            Address address = addresses != null && !addresses.isEmpty() ? addresses.get(0) : null;

                                            // Bước 4: Lấy danh sách sản phẩm từ bảng order_items
                                            String orderItemsUrl = ORDER_ITEMS_URL + "?order_id=eq." + orderId + "&select=*,products(name)";
                                            Request orderItemsRequest = getRequestBuilder()
                                                    .url(orderItemsUrl)
                                                    .get()
                                                    .build();

                                            getOkHttpClient().newCall(orderItemsRequest).enqueue(new Callback() {
                                                @Override
                                                public void onFailure(Call call, IOException e) {
                                                    runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, "Failed to load order items", Toast.LENGTH_SHORT).show());
                                                }

                                                @Override
                                                public void onResponse(Call call, Response orderItemsResponse) throws IOException {
                                                    String orderItemsResponseData = orderItemsResponse.body().string();
                                                    if (orderItemsResponse.isSuccessful()) {
                                                        Type orderItemType = new TypeToken<List<OrderItem>>(){}.getType();
                                                        List<OrderItem> orderItems = gson.fromJson(orderItemsResponseData, orderItemType);

                                                        // Hiển thị thông tin hóa đơn
                                                        runOnUiThread(() -> {
                                                            binding.orderIdText.setText("Order ID: " + order.getOrderId());
                                                            binding.orderDateText.setText("Order Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(order.getCreatedAtAsDate()));
                                                            if (userProfile != null) {
                                                                binding.customerNameText.setText("Name: " + userProfile.getUsername());
                                                                binding.customerEmailText.setText("Email: " + userProfile.getEmail());
                                                                binding.customerPhoneText.setText("Phone: " + userProfile.getPhoneNumber());
                                                            }
                                                            if (address != null) {
                                                                binding.shippingAddressText.setText("Address: " + address.getAddressLine() + ", " + address.getCity() + ", " + address.getCountry());
                                                            }

                                                            // Hiển thị danh sách sản phẩm
                                                            binding.orderItemsContainer.removeAllViews();
                                                            for (OrderItem item : orderItems) {
                                                                View itemView = LayoutInflater.from(OrderDetailActivity.this).inflate(R.layout.order_item_layout, binding.orderItemsContainer, false);
                                                                TextView itemName = itemView.findViewById(R.id.item_name);
                                                                TextView itemDetails = itemView.findViewById(R.id.item_details);
                                                                itemName.setText(item.getProduct().getName() + " (Size: " + item.getSize() + ")");
                                                                itemDetails.setText("Quantity: " + item.getQuantity() + " | Price: " + String.format("%.2f₫", item.getPrice()));
                                                                binding.orderItemsContainer.addView(itemView);
                                                            }

                                                            // Hiển thị tóm tắt đơn hàng
                                                            binding.subtotalText.setText("Subtotal: " + String.format("%.2f₫", order.getSubtotal()));
                                                            binding.deliveryFeeText.setText("Delivery Fee: " + String.format("%.2f₫", order.getDeliveryFee()));
                                                            binding.taxText.setText("Tax: " + String.format("%.2f₫", order.getTax()));
                                                            binding.discountText.setText("Discount: " + String.format("%.2f₫", order.getDiscountAmount()) + (order.getDiscountCode() != null ? " (" + order.getDiscountCode() + ")" : ""));
                                                            binding.totalAmountText.setText("Total Amount: " + String.format("%.2f₫", order.getTotalAmount()));
                                                        });
                                                    }
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }
}