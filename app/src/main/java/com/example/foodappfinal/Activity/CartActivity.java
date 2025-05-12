package com.example.foodappfinal.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.foodappfinal.Adapter.CartAdapter;
import com.example.foodappfinal.Model.Address;
import com.example.foodappfinal.Model.CartItem;
import com.example.foodappfinal.Model.DiscountCode;
import com.example.foodappfinal.Model.RawCartItem;
import com.example.foodappproject.R;
import com.example.foodappproject.databinding.ActivityCartBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CartActivity extends BaseActivity implements CartAdapter.OnCartItemChangeListener {
    private static final String TAG = "CartActivity";
    private ActivityCartBinding binding;
    private List<CartItem> cartItems;
    private CartAdapter cartAdapter;
    private List<Address> addressList;
    private ArrayAdapter<String> addressAdapter;
    private String selectedAddressId;
    private static final String CART_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/cart_items";
    private static final String ORDERS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/orders";
    private static final String ORDER_ITEMS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/order_items";
    private static final String ADDRESSES_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/addresses";
    private static final String PRODUCTS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/products";
    private static final double DELIVERY_FEE = 2.00;
    private static final double TAX_RATE = 0.10;
    private double subtotal = 0.0;
    private double discountAmount = 0.0;
    private String appliedCouponCode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cartItems = new ArrayList<>();
        cartAdapter = new CartAdapter(this, cartItems, this);
        binding.cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.cartRecyclerView.setAdapter(cartAdapter);

        addressList = new ArrayList<>();
        addressAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        addressAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.addressSpinner.setAdapter(addressAdapter);
        binding.addressSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAddressId = addressList.get(position).getAddressId();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedAddressId = null;
            }
        });

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Giỏ hàng");
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.applyCouponButton.setOnClickListener(v -> applyCoupon());
        binding.checkOutBtn.setOnClickListener(v -> checkout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartItems();
        loadAddresses();
    }

    private void loadCartItems() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "You must be logged in to view cart", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        String url = CART_URL + "?user_id=eq." + userId + "&select=*,products(name,price,stock,size)";
        Log.d(TAG, "loadCartItems: Fetching cart items from URL: " + url);
        Request request = getRequestBuilder()
                .url(url)
                .get()
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "loadCartItems: Failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to load cart: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "loadCartItems: Response: " + responseData + ", HTTP code: " + response.code());
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Type cartItemType = new TypeToken<List<RawCartItem>>(){}.getType();
                    List<RawCartItem> rawItems = gson.fromJson(responseData, cartItemType);

                    cartItems.clear();
                    if (rawItems != null && !rawItems.isEmpty()) {
                        for (RawCartItem rawItem : rawItems) {
                            CartItem cartItem = new CartItem();
                            cartItem.setCartItemId(rawItem.getCart_item_id());
                            cartItem.setProductId(rawItem.getProduct_id());
                            cartItem.setProductName(rawItem.getProducts().getName());
                            cartItem.setPrice(rawItem.getProducts().getPrice());
                            cartItem.setQuantity(rawItem.getQuantity());
                            cartItem.setStock(rawItem.getProducts().getStock());
                            cartItem.setSize(rawItem.getSize());
                            cartItems.add(cartItem);
                        }
                    }

                    runOnUiThread(() -> {
                        binding.emptyTxt.setVisibility(cartItems.isEmpty() ? View.VISIBLE : View.GONE);
                        cartAdapter.notifyDataSetChanged();
                        updateSummary();
                    });
                } else {
                    Log.e(TAG, "loadCartItems: Failed: HTTP " + response.code() + ", response: " + responseData);
                    runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to load cart: HTTP " + response.code(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void loadAddresses() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        String url = ADDRESSES_URL + "?user_id=eq." + userId + "&select=*";
        Request request = getRequestBuilder()
                .url(url)
                .get()
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "loadAddresses: Failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to load addresses", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "loadAddresses: Response: " + responseData + ", HTTP code: " + response.code());
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Type addressType = new TypeToken<List<Address>>(){}.getType();
                    List<Address> addresses = gson.fromJson(responseData, addressType);

                    addressList.clear();
                    List<String> addressStrings = new ArrayList<>();
                    final int defaultAddressIndex;
                    if (addresses != null && !addresses.isEmpty()) {
                        addressList.addAll(addresses);
                        int tempIndex = -1;
                        for (int i = 0; i < addresses.size(); i++) {
                            Address address = addresses.get(i);
                            addressStrings.add(address.getFullAddress());
                            if (address.isDefault()) {
                                tempIndex = i;
                                selectedAddressId = address.getAddressId();
                            }
                        }
                        defaultAddressIndex = tempIndex;
                    } else {
                        defaultAddressIndex = -1;
                    }

                    runOnUiThread(() -> {
                        addressAdapter.clear();
                        addressAdapter.addAll(addressStrings);
                        addressAdapter.notifyDataSetChanged();
                        if (addressStrings.isEmpty()) {
                            binding.addressSpinner.setEnabled(false);
                            selectedAddressId = null;
                        } else {
                            binding.addressSpinner.setEnabled(true);
                            if (defaultAddressIndex != -1) {
                                binding.addressSpinner.setSelection(defaultAddressIndex);
                            }
                        }
                    });
                }
            }
        });
    }

    private void updateSummary() {
        subtotal = 0.0;
        for (CartItem item : cartItems) {
            subtotal += item.getPrice() * item.getQuantity();
        }

        double delivery = DELIVERY_FEE;
        double tax = subtotal * TAX_RATE;
        double total = Math.max(0.0, subtotal + delivery + tax - discountAmount);

        binding.totalFeeTxt.setText(String.format("%.2f₫", subtotal));
        binding.deliveryTxt.setText(String.format("%.2f₫", delivery));
        binding.taxTxt.setText(String.format("%.2f₫", tax));
        binding.totalTxt.setText(String.format("%.2f₫", total));
    }

    @Override
    public void onQuantityChanged(String cartItemId, int newQuantity) {
        String url = CART_URL + "?cart_item_id=eq." + cartItemId;
        String json = String.format("{\"quantity\":%d}", newQuantity);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = getRequestBuilder()
                .url(url)
                .patch(body)
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "updateQuantity: Failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to update quantity: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "updateQuantity: Response: " + responseData + ", HTTP code: " + response.code());
                if (response.isSuccessful()) {
                    for (CartItem item : cartItems) {
                        if (item.getCartItemId().equals(cartItemId)) {
                            item.setQuantity(newQuantity);
                            break;
                        }
                    }
                    runOnUiThread(() -> {
                        cartAdapter.notifyDataSetChanged();
                        updateSummary();
                    });
                } else {
                    Log.e(TAG, "updateQuantity: Failed: HTTP " + response.code() + ", response: " + responseData);
                    runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to update quantity: HTTP " + response.code(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    @Override
    public void onItemRemoved(String cartItemId) {
        String url = CART_URL + "?cart_item_id=eq." + cartItemId;
        Request request = getRequestBuilder()
                .url(url)
                .delete()
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "removeItem: Failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to remove item: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "removeItem: Response: " + responseData + ", HTTP code: " + response.code());
                if (response.isSuccessful()) {
                    CartItem itemToRemove = null;
                    for (CartItem item : cartItems) {
                        if (item.getCartItemId().equals(cartItemId)) {
                            itemToRemove = item;
                            break;
                        }
                    }
                    if (itemToRemove != null) {
                        cartItems.remove(itemToRemove);
                    }
                    runOnUiThread(() -> {
                        binding.emptyTxt.setVisibility(cartItems.isEmpty() ? View.VISIBLE : View.GONE);
                        cartAdapter.notifyDataSetChanged();
                        updateSummary();
                    });
                } else {
                    Log.e(TAG, "removeItem: Failed: HTTP " + response.code() + ", response: " + responseData);
                    runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to remove item: HTTP " + response.code(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void applyCoupon() {
        String couponCode = binding.couponInput.getText().toString().trim();
        if (couponCode.isEmpty()) {
            Toast.makeText(this, "Please enter a coupon code", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/discount_codes?code=eq." + couponCode;
        Request request = getRequestBuilder()
                .url(url)
                .get()
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "applyCoupon: Failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to apply coupon: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "applyCoupon: Response: " + responseData + ", HTTP code: " + response.code());
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Type discountType = new TypeToken<List<DiscountCode>>(){}.getType();
                    List<DiscountCode> discounts = gson.fromJson(responseData, discountType);

                    if (discounts == null || discounts.isEmpty()) {
                        runOnUiThread(() -> Toast.makeText(CartActivity.this, "Invalid coupon code", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    DiscountCode discount = discounts.get(0);
                    Date currentDate = new Date();
                    Date validFromDate = discount.getValidFromDate();
                    Date validUntilDate = discount.getValidUntilDate();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                    Log.d(TAG, "applyCoupon: Current Date: " + sdf.format(currentDate));
                    Log.d(TAG, "applyCoupon: Valid From: " + sdf.format(validFromDate));
                    Log.d(TAG, "applyCoupon: Valid Until: " + sdf.format(validUntilDate));
                    Log.d(TAG, "applyCoupon: Is Active: " + discount.isActive());

                    if (!discount.isActive()) {
                        runOnUiThread(() -> Toast.makeText(CartActivity.this, "Coupon code is not active", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    if (validFromDate.after(currentDate)) {
                        runOnUiThread(() -> Toast.makeText(CartActivity.this, "Coupon code is not yet valid", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    if (validUntilDate.before(currentDate)) {
                        runOnUiThread(() -> Toast.makeText(CartActivity.this, "Coupon code has expired", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    discountAmount = discount.getDiscountValue();
                    appliedCouponCode = couponCode;
                    runOnUiThread(() -> {
                        Toast.makeText(CartActivity.this, "Coupon applied successfully", Toast.LENGTH_SHORT).show();
                        updateSummary();
                    });
                } else {
                    Log.e(TAG, "applyCoupon: Failed: HTTP " + response.code() + ", response: " + responseData);
                    runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to apply coupon: HTTP " + response.code(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void checkout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "You must be logged in to checkout", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        if (addressList.isEmpty()) {
            Toast.makeText(this, "Please add an address in Personal Info before checkout", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(CartActivity.this, PersonalInfoActivity.class);
            startActivity(intent);
            return;
        }

        String addressId = selectedAddressId;
        if (addressId == null) {
            for (Address address : addressList) {
                if (address.isDefault()) {
                    addressId = address.getAddressId();
                    break;
                }
            }
        }
        if (addressId == null) {
            Toast.makeText(this, "Please select an address or set a default address", Toast.LENGTH_LONG).show();
            return;
        }

        final double delivery = DELIVERY_FEE;
        final double tax = subtotal * TAX_RATE;
        final double total = Math.max(0.0, subtotal + delivery + tax - discountAmount);

        final String orderId = UUID.randomUUID().toString();
        String orderJson = String.format(
                "{\"order_id\":\"%s\",\"user_id\":\"%s\",\"address_id\":\"%s\",\"total_amount\":%.2f,\"subtotal\":%.2f,\"delivery_fee\":%.2f,\"tax\":%.2f,\"discount_code\":\"%s\",\"discount_amount\":%.2f,\"status\":\"Confirmed\"}",
                orderId, userId, addressId, total, subtotal, delivery, tax, appliedCouponCode != null ? appliedCouponCode : "", discountAmount
        );
        RequestBody orderBody = RequestBody.create(orderJson, MediaType.parse("application/json"));
        Request orderRequest = getRequestBuilder()
                .url(ORDERS_URL)
                .post(orderBody)
                .build();

        getOkHttpClient().newCall(orderRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "checkout: Failed to create order: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to create order: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response orderResponse) throws IOException {
                String orderResponseData = orderResponse.body().string();
                Log.d(TAG, "checkout: Order response: " + orderResponseData + ", HTTP code: " + orderResponse.code());
                if (!orderResponse.isSuccessful()) {
                    Log.e(TAG, "checkout: Failed to create order: HTTP " + orderResponse.code());
                    runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to create order: HTTP " + orderResponse.code(), Toast.LENGTH_LONG).show());
                    return;
                }

                List<String> orderItemsJsonList = new ArrayList<>();
                for (CartItem item : cartItems) {
                    String orderItemJson = String.format(
                            "{\"order_item_id\":\"%s\",\"order_id\":\"%s\",\"product_id\":\"%s\",\"size\":\"%s\",\"quantity\":%d,\"price\":%.2f}",
                            UUID.randomUUID().toString(), orderId, item.getProductId(), item.getSize(), item.getQuantity(), item.getPrice()
                    );
                    orderItemsJsonList.add(orderItemJson);
                }

                String orderItemsJson = "[" + String.join(",", orderItemsJsonList) + "]";
                RequestBody orderItemsBody = RequestBody.create(orderItemsJson, MediaType.parse("application/json"));
                Request orderItemsRequest = getRequestBuilder()
                        .url(ORDER_ITEMS_URL)
                        .post(orderItemsBody)
                        .build();

                getOkHttpClient().newCall(orderItemsRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "checkout: Failed to create order items: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to create order items: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onResponse(Call call, Response orderItemsResponse) throws IOException {
                        String orderItemsResponseData = orderItemsResponse.body().string();
                        Log.d(TAG, "checkout: Order items response: " + orderItemsResponseData + ", HTTP code: " + orderItemsResponse.code());
                        if (!orderItemsResponse.isSuccessful()) {
                            Log.e(TAG, "checkout: Failed to create order items: HTTP " + orderItemsResponse.code());
                            runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to create order items: HTTP " + orderItemsResponse.code(), Toast.LENGTH_LONG).show());
                            return;
                        }

                        for (CartItem item : cartItems) {
                            int quantity = item.getQuantity();
                            String productId = item.getProductId();
                            int newStock = item.getStock() - quantity;
                            Log.d(TAG, "checkout: Updating stock for productId: " + productId + ", oldStock: " + item.getStock() + ", newStock: " + newStock);

                            String updateStockJson = String.format("{\"stock\":%d}", newStock);
                            RequestBody updateStockBody = RequestBody.create(updateStockJson, MediaType.parse("application/json"));
                            Request updateStockRequest = getRequestBuilder()
                                    .url(PRODUCTS_URL + "?product_id=eq." + productId)
                                    .patch(updateStockBody)
                                    .build();

                            getOkHttpClient().newCall(updateStockRequest).enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    Log.e(TAG, "checkout: Failed to update stock for productId: " + productId + ", error: " + e.getMessage());
                                    runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to update stock for some products", Toast.LENGTH_LONG).show());
                                }

                                @Override
                                public void onResponse(Call call, Response updateStockResponse) throws IOException {
                                    String updateStockResponseData = updateStockResponse.body().string();
                                    Log.d(TAG, "checkout: Stock update response for productId: " + productId + ", response: " + updateStockResponseData + ", HTTP code: " + updateStockResponse.code());
                                    if (!updateStockResponse.isSuccessful()) {
                                        Log.e(TAG, "checkout: Failed to update stock for productId: " + productId + ", HTTP " + updateStockResponse.code());
                                    }
                                }
                            });
                        }

                        String deleteCartUrl = CART_URL + "?user_id=eq." + userId;
                        Request deleteCartRequest = getRequestBuilder()
                                .url(deleteCartUrl)
                                .delete()
                                .build();

                        getOkHttpClient().newCall(deleteCartRequest).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.e(TAG, "checkout: Failed to clear cart: " + e.getMessage());
                                runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to clear cart: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            }

                            @Override
                            public void onResponse(Call call, Response deleteCartResponse) throws IOException {
                                String deleteCartResponseData = deleteCartResponse.body().string();
                                Log.d(TAG, "checkout: Clear cart response: " + deleteCartResponseData + ", HTTP code: " + deleteCartResponse.code());
                                if (!deleteCartResponse.isSuccessful()) {
                                    Log.e(TAG, "checkout: Failed to clear cart: HTTP " + deleteCartResponse.code());
                                    runOnUiThread(() -> Toast.makeText(CartActivity.this, "Failed to clear cart: HTTP " + deleteCartResponse.code(), Toast.LENGTH_LONG).show());
                                    return;
                                }

                                sendNotification("Order " + orderId + " has been successfully placed.");

                                Intent intent = new Intent(CartActivity.this, OrderConfirmationActivity.class);
                                intent.putExtra("order_id", orderId);
                                intent.putExtra("cart_items", new ArrayList<>(cartItems));
                                intent.putExtra("subtotal", subtotal);
                                intent.putExtra("delivery_fee", DELIVERY_FEE);
                                intent.putExtra("tax", tax);
                                intent.putExtra("discount_code", appliedCouponCode);
                                intent.putExtra("discount_amount", discountAmount);
                                intent.putExtra("total_amount", total);
                                startActivity(intent);

                                cartItems.clear();
                                runOnUiThread(() -> {
                                    cartAdapter.notifyDataSetChanged();
                                    binding.emptyTxt.setVisibility(View.VISIBLE);
                                    updateSummary();
                                    Toast.makeText(CartActivity.this, "Checkout successful", Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    }
                });
            }
        });
    }
}