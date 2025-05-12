package com.example.foodappfinal.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.foodappfinal.Adapter.SizeAdapter;
import com.example.foodappfinal.Adapter.SliderAdapter;
import com.example.foodappfinal.Adapter.TabPagerAdapter;
import com.example.foodappfinal.Domain.ItemsDomain;
import com.example.foodappfinal.Model.CartItem;
import com.example.foodappfinal.Model.Product;
import com.example.foodappfinal.Model.SliderItems;
import com.example.foodappfinal.Model.WishlistItem;
import com.example.foodappfinal.Model.Review;
import com.example.foodappproject.R;
import com.example.foodappproject.databinding.ActivityDetailBinding;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DetailActivity extends BaseActivity {
    private static final String TAG = "DetailActivity";
    private ActivityDetailBinding binding;
    private static final String PRODUCTS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/products?select=*,brands(name),product_reviews(rating)";
    private static final String WISHLIST_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/wishlist";
    private static final String CART_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/cart_items";
    private ItemsDomain item;
    private ArrayList<String> availableSizes; // Store updated sizes from server
    private boolean isInWishlist = false;
    private static final ArrayList<String> ALL_SIZES = new ArrayList<>(Arrays.asList("S", "M", "L", "XL", "XXL"));
    private Button selectedSizeButton = null;
    private int currentTabPosition = 0;
    private TabPagerAdapter tabPagerAdapter;
    private boolean isRestoringTab = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        item = (ItemsDomain) getIntent().getSerializableExtra("object");
        if (item == null) {
            Log.e(TAG, "onCreate: Item is null");
            Toast.makeText(this, "Product not found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "onCreate: Received productId: " + item.getProductId());

        binding.titleText.setText(item.getTitle());
        binding.priceText.setText(String.format("%.0f₫", item.getPrice()));
        binding.ratingText.setText(String.format("%.1f Rating", item.getRating()));
        binding.ratingBar.setRating((float) item.getRating());

        if (item.getPicUrl() != null && !item.getPicUrl().isEmpty()) {
            ArrayList<SliderItems> sliderItems = new ArrayList<>();
            for (String url : item.getPicUrl()) {
                sliderItems.add(new SliderItems(url));
            }
            SliderAdapter sliderAdapter = new SliderAdapter(sliderItems, binding.productImageViewPager);
            binding.productImageViewPager.setAdapter(sliderAdapter);
        }

        setupSizeButtons();

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        checkWishlistStatus();
        binding.wishlistButton.setOnClickListener(v -> {
            if (isInWishlist) {
                removeFromWishlist();
            } else {
                addToWishlist();
            }
        });

        binding.cartButton.setOnClickListener(v -> addToCart());

        setupTabLayout();

        if (savedInstanceState != null) {
            currentTabPosition = savedInstanceState.getInt("currentTabPosition", 0);
            binding.viewPager.setCurrentItem(currentTabPosition, false);
        }

        loadProductDetails();
    }

    private void addToCart() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "You must be logged in to add to cart", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        if (selectedSizeButton == null) {
            Toast.makeText(this, "Please select a size before adding to cart", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedSize = selectedSizeButton.getText().toString();
        if (availableSizes == null || !availableSizes.contains(selectedSize)) {
            Toast.makeText(this, "Selected size is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log giá trị stock và sold trước khi kiểm tra
        Log.d(TAG, "addToCart: Checking stock for productId: " + item.getProductId() + ", stock: " + item.getStock() + ", sold: " + item.getSold());

        // Kiểm tra tồn kho trước khi thêm vào giỏ hàng
        if (item.getStock() <= 0) {
            Log.w(TAG, "addToCart: Product is out of stock, stock: " + item.getStock());
            Toast.makeText(this, "Product is out of stock", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "addToCart: Checking cart for userId: " + userId + ", productId: " + item.getProductId() + ", size: " + selectedSize);
        String checkUrl = CART_URL + "?user_id=eq." + userId + "&product_id=eq." + item.getProductId() + "&size=eq." + selectedSize;
        Request checkRequest = getRequestBuilder()
                .url(checkUrl)
                .get()
                .build();

        getOkHttpClient().newCall(checkRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "addToCart: Check cart failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Failed to check cart: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "addToCart: Check cart response: " + responseData + ", HTTP code: " + response.code());
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Type cartItemType = new TypeToken<List<CartItem>>(){}.getType();
                    List<CartItem> existingItems = gson.fromJson(responseData, cartItemType);

                    if (existingItems != null && !existingItems.isEmpty()) {
                        // Sản phẩm đã có trong giỏ hàng, tăng số lượng
                        CartItem existingItem = existingItems.get(0);
                        int currentQuantity = existingItem.getQuantity();
                        int newQuantity = currentQuantity + 1;

                        // Log số lượng hiện tại và số lượng mới
                        Log.d(TAG, "addToCart: Product already in cart, currentQuantity: " + currentQuantity + ", newQuantity: " + newQuantity + ", stock: " + item.getStock());

                        // Kiểm tra tổng số lượng so với tồn kho
                        if (newQuantity > item.getStock()) {
                            Log.w(TAG, "addToCart: Cannot add more, stock limit reached. newQuantity: " + newQuantity + ", stock: " + item.getStock());
                            runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Cannot add more, only " + item.getStock() + " items left in stock", Toast.LENGTH_SHORT).show());
                            return;
                        }
                        updateCartItem(existingItem.getCartItemId(), newQuantity);
                    } else {
                        // Sản phẩm chưa có trong giỏ hàng, thêm mới với số lượng 1
                        Log.d(TAG, "addToCart: Product not in cart, adding new item with quantity 1, stock: " + item.getStock());
                        if (1 > item.getStock()) {
                            Log.w(TAG, "addToCart: Cannot add to cart, stock limit reached. Requested: 1, stock: " + item.getStock());
                            runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Cannot add to cart, only " + item.getStock() + " items left in stock", Toast.LENGTH_SHORT).show());
                            return;
                        }
                        insertNewCartItem(userId, selectedSize);
                    }
                } else if (response.code() == 401) {
                    // Token might be expired, try refreshing
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess(String newAccessToken) {
                            // Retry the request
                            addToCart();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "addToCart: Failed to refresh token: " + e.getMessage());
                            runOnUiThread(() -> {
                                Toast.makeText(DetailActivity.this, "Session expired, please log in again", Toast.LENGTH_SHORT).show();
                                logout();
                            });
                        }
                    });
                } else {
                    Log.e(TAG, "addToCart: Check cart failed: HTTP " + response.code() + ", response: " + responseData);
                    runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Failed to check cart: HTTP " + response.code(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void insertNewCartItem(String userId, String selectedSize) {
        String token = getAccessToken();
        Log.d(TAG, "insertNewCartItem: Token: " + token + ", userId: " + userId);

        String json = String.format(
                "{\"user_id\":\"%s\",\"product_id\":\"%s\",\"size\":\"%s\",\"quantity\":%d}",
                userId, item.getProductId(), selectedSize, 1
        );
        Log.d(TAG, "insertNewCartItem: Sending JSON: " + json);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = getRequestBuilder()
                .url(CART_URL)
                .post(body)
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "insertNewCartItem: Failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Failed to add to cart: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "insertNewCartItem: Response: " + responseData + ", HTTP code: " + response.code());
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Added to cart successfully", Toast.LENGTH_SHORT).show());
                } else if (response.code() == 401) {
                    // Token might be expired, try refreshing
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess(String newAccessToken) {
                            // Retry the request
                            insertNewCartItem(userId, selectedSize);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "insertNewCartItem: Failed to refresh token: " + e.getMessage());
                            runOnUiThread(() -> {
                                Toast.makeText(DetailActivity.this, "Session expired, please log in again", Toast.LENGTH_SHORT).show();
                                logout();
                            });
                        }
                    });
                } else {
                    Log.e(TAG, "insertNewCartItem: Failed: HTTP " + response.code() + ", response: " + responseData);
                    runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Failed to add to cart: HTTP " + response.code(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void updateCartItem(String cartItemId, int newQuantity) {
        String json = String.format("{\"quantity\":%d}", newQuantity);
        Log.d(TAG, "updateCartItem: Sending JSON: " + json + " for cartItemId: " + cartItemId);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = getRequestBuilder()
                .url(CART_URL + "?cart_item_id=eq." + cartItemId)
                .patch(body)
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "updateCartItem: Failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Failed to update cart: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "updateCartItem: Response: " + responseData + ", HTTP code: " + response.code());
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Added to cart successfully", Toast.LENGTH_SHORT).show());
                } else if (response.code() == 401) {
                    // Token might be expired, try refreshing
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess(String newAccessToken) {
                            // Retry the request
                            updateCartItem(cartItemId, newQuantity);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "updateCartItem: Failed to refresh token: " + e.getMessage());
                            runOnUiThread(() -> {
                                Toast.makeText(DetailActivity.this, "Session expired, please log in again", Toast.LENGTH_SHORT).show();
                                logout();
                            });
                        }
                    });
                } else {
                    Log.e(TAG, "updateCartItem: Failed: HTTP " + response.code() + ", response: " + responseData);
                    runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Failed to update cart: HTTP " + response.code(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentTabPosition", binding.viewPager.getCurrentItem());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void setupSizeButtons() {
        Button[] sizeButtons = {binding.sizeS, binding.sizeM, binding.sizeL, binding.sizeXL, binding.sizeXXL};
        if (item.getSizes() != null && !item.getSizes().isEmpty()) {
            for (Button button : sizeButtons) {
                String size = button.getText().toString();
                if (item.getSizes().contains(size)) {
                    button.setEnabled(true);
                    button.setBackgroundResource(R.drawable.size_button_rounded_enabled);
                } else {
                    button.setEnabled(false);
                    button.setBackgroundResource(R.drawable.size_button_rounded);
                    button.setAlpha(0.7f);
                }
                button.setSelected(false);
            }
        } else {
            for (Button button : sizeButtons) {
                button.setEnabled(true);
                button.setBackgroundResource(R.drawable.size_button_rounded_enabled);
                button.setSelected(false);
            }
        }

        for (Button button : sizeButtons) {
            button.setOnClickListener(v -> {
                if (!button.isEnabled()) return;
                if (selectedSizeButton != null) {
                    selectedSizeButton.setSelected(false);
                    selectedSizeButton.setBackgroundResource(R.drawable.size_button_rounded_enabled);
                    selectedSizeButton.setTextColor(getResources().getColor(android.R.color.black));
                }
                button.setSelected(true);
                button.setBackgroundResource(R.drawable.size_button_rounded_selected);
                button.setTextColor(getResources().getColor(android.R.color.white));
                selectedSizeButton = button;
            });
        }
    }

    private void checkWishlistStatus() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "You must be logged in to manage wishlist", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = WISHLIST_URL + "?user_id=eq." + userId + "&product_id=eq." + item.getProductId();
        Request request = getRequestBuilder()
                .url(url)
                .get()
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "checkWishlistStatus: Failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Failed to check wishlist status", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Gson gson = new Gson();
                    Type wishlistType = new TypeToken<List<WishlistItem>>(){}.getType();
                    List<WishlistItem> wishlistItems = gson.fromJson(responseData, wishlistType);

                    isInWishlist = wishlistItems != null && !wishlistItems.isEmpty();
                    runOnUiThread(() -> {
                        binding.wishlistButton.setImageResource(isInWishlist ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite);
                    });
                } else if (response.code() == 401) {
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess(String newAccessToken) {
                            checkWishlistStatus();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "checkWishlistStatus: Failed to refresh token: " + e.getMessage());
                            runOnUiThread(() -> {
                                Toast.makeText(DetailActivity.this, "Session expired, please log in again", Toast.LENGTH_SHORT).show();
                                logout();
                            });
                        }
                    });
                }
            }
        });
    }

    private void addToWishlist() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "You must be logged in to add to wishlist", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        String json = String.format(
                "{\"user_id\":\"%s\",\"product_id\":\"%s\"}",
                userId, item.getProductId()
        );
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = getRequestBuilder()
                .url(WISHLIST_URL)
                .post(body)
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "addToWishlist: Failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Failed to add to wishlist", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        isInWishlist = true;
                        binding.wishlistButton.setImageResource(R.drawable.ic_favorite_filled);
                    });
                } else if (response.code() == 401) {
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess(String newAccessToken) {
                            addToWishlist();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "addToWishlist: Failed to refresh token: " + e.getMessage());
                            runOnUiThread(() -> {
                                Toast.makeText(DetailActivity.this, "Session expired, please log in again", Toast.LENGTH_SHORT).show();
                                logout();
                            });
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Failed to add to wishlist", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void removeFromWishlist() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "You must be logged in to remove from wishlist", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        String url = WISHLIST_URL + "?user_id=eq." + userId + "&product_id=eq." + item.getProductId();
        Request request = getRequestBuilder()
                .url(url)
                .delete()
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "removeFromWishlist: Failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Failed to remove from wishlist", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        isInWishlist = false;
                        binding.wishlistButton.setImageResource(R.drawable.ic_favorite);
                    });
                } else if (response.code() == 401) {
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess(String newAccessToken) {
                            removeFromWishlist();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "removeFromWishlist: Failed to refresh token: " + e.getMessage());
                            runOnUiThread(() -> {
                                Toast.makeText(DetailActivity.this, "Session expired, please log in again", Toast.LENGTH_SHORT).show();
                                logout();
                            });
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Failed to remove from wishlist", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void setupTabLayout() {
        tabPagerAdapter = new TabPagerAdapter(
                this,
                item.getDescription() != null ? item.getDescription() : "No description",
                item.getProductId(),
                item.getRating(),
                item.getNumberOfReviews(),
                item.getStock(),
                item.getSold()
        );
        binding.viewPager.setAdapter(tabPagerAdapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            tab.setText(tabPagerAdapter.getTabTitle(position));
        }).attach();

        binding.viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (!isRestoringTab) {
                    currentTabPosition = position;
                    Log.d(TAG, "ViewPager2 page selected: " + position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE && binding.viewPager.getCurrentItem() != currentTabPosition) {
                    isRestoringTab = true;
                    binding.viewPager.setCurrentItem(currentTabPosition, false);
                    isRestoringTab = false;
                    Log.d(TAG, "Restored tab position to: " + currentTabPosition);
                }
            }
        });

        binding.viewPager.setOffscreenPageLimit(2);
    }

    private void loadProductDetails() {
        Toast.makeText(this, "Loading product details...", Toast.LENGTH_SHORT).show();

        OkHttpClient client = getOkHttpClient();
        String supabaseKey = getSupabaseKey();

        String url = PRODUCTS_URL + "&product_id=eq." + item.getProductId();
        Log.d(TAG, "loadProductDetails: Fetching product from URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("apikey", supabaseKey)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "loadProductDetails: Failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Failed to load product details from server", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "loadProductDetails: Response code: " + response.code());
                if (response.isSuccessful()) {
                    String productData = response.body().string();
                    Log.d(TAG, "loadProductDetails: Product data: " + productData);
                    Gson gson = new Gson();
                    Type productListType = new TypeToken<List<Product>>(){}.getType();
                    List<Product> productList = gson.fromJson(productData, productListType);

                    if (productList == null || productList.isEmpty()) {
                        Log.w(TAG, "loadProductDetails: Product not found for productId: " + item.getProductId());
                        runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Product not found on server", Toast.LENGTH_LONG).show());
                        return;
                    }

                    Product product = productList.get(0);
                    // Log giá trị stock và sold từ server
                    Log.d(TAG, "loadProductDetails: Product loaded - productId: " + product.product_id + ", stock: " + product.stock + ", sold: " + product.sold);

                    runOnUiThread(() -> {
                        binding.titleText.setText(product.name);
                        binding.priceText.setText(String.format("%.0f₫", product.price));
                        binding.ratingText.setText(String.format("%.1f Rating", product.average_rating));
                        binding.ratingBar.setRating((float) product.average_rating);

                        ArrayList<String> picUrls = new ArrayList<>();
                        String imageUrl = "https://fmheidqpephjpsgjuupf.supabase.co/storage/v1/object/public/item/item_" + product.product_id + "/" + product.product_id + "_1.png";
                        picUrls.add(imageUrl);
                        ArrayList<SliderItems> sliderItems = new ArrayList<>();
                        for (String url : picUrls) {
                            sliderItems.add(new SliderItems(url));
                        }
                        SliderAdapter sliderAdapter = new SliderAdapter(sliderItems, binding.productImageViewPager);
                        binding.productImageViewPager.setAdapter(sliderAdapter);

                        // Update available sizes from server
                        if (product.size != null && !product.size.isEmpty()) {
                            availableSizes = new ArrayList<>(product.getSizes());
                            Button[] sizeButtons = {binding.sizeS, binding.sizeM, binding.sizeL, binding.sizeXL, binding.sizeXXL};
                            for (Button button : sizeButtons) {
                                String size = button.getText().toString();
                                if (availableSizes.contains(size)) {
                                    button.setEnabled(true);
                                    if (button == selectedSizeButton) {
                                        button.setBackgroundResource(R.drawable.size_button_rounded_selected);
                                        button.setTextColor(getResources().getColor(android.R.color.white));
                                    } else {
                                        button.setBackgroundResource(R.drawable.size_button_rounded_enabled);
                                        button.setTextColor(getResources().getColor(android.R.color.black));
                                    }
                                    button.setAlpha(1.0f);
                                } else {
                                    button.setEnabled(false);
                                    button.setBackgroundResource(R.drawable.size_button_rounded);
                                    button.setTextColor(getResources().getColor(android.R.color.black));
                                    button.setAlpha(0.7f);
                                }
                            }
                        } else {
                            // If no sizes available from server, enable all buttons as fallback
                            availableSizes = new ArrayList<>(ALL_SIZES);
                            Button[] sizeButtons = {binding.sizeS, binding.sizeM, binding.sizeL, binding.sizeXL, binding.sizeXXL};
                            for (Button button : sizeButtons) {
                                button.setEnabled(true);
                                if (button == selectedSizeButton) {
                                    button.setBackgroundResource(R.drawable.size_button_rounded_selected);
                                    button.setTextColor(getResources().getColor(android.R.color.white));
                                } else {
                                    button.setBackgroundResource(R.drawable.size_button_rounded_enabled);
                                    button.setTextColor(getResources().getColor(android.R.color.black));
                                }
                                button.setAlpha(1.0f);
                            }
                        }

                        // Convert availableSizes to a comma-separated string and update item
                        String sizeString = String.join(",", availableSizes);
                        item.setSize(sizeString);
                        item.setPrice(product.price);
                        item.setStock(product.stock);
                        item.setRating(product.average_rating);
                        item.setNumberOfReviews(product.number_of_reviews);
                        item.setSold(product.sold);

                        // Log giá trị stock và sold sau khi cập nhật vào item
                        Log.d(TAG, "loadProductDetails: Updated item - productId: " + item.getProductId() + ", stock: " + item.getStock() + ", sold: " + item.getSold());

                        // Disable cart button if stock is 0
                        if (item.getStock() <= 0) {
                            Log.w(TAG, "loadProductDetails: Disabling cart button, stock is " + item.getStock());
                            binding.cartButton.setEnabled(false);
                            binding.cartButton.setAlpha(0.5f);
                        } else {
                            Log.d(TAG, "loadProductDetails: Enabling cart button, stock is " + item.getStock());
                            binding.cartButton.setEnabled(true);
                            binding.cartButton.setAlpha(1.0f);
                        }

                        if (tabPagerAdapter != null) {
                            tabPagerAdapter.updateProductDetails(
                                    product.description != null ? product.description : "No description",
                                    product.product_id,
                                    product.average_rating,
                                    product.number_of_reviews,
                                    product.stock,
                                    product.sold
                            );
                        }

                        isRestoringTab = true;
                        binding.viewPager.setCurrentItem(currentTabPosition, false);
                        isRestoringTab = false;
                        Log.d(TAG, "Restored tab position after loadProductDetails: " + currentTabPosition);
                    });
                } else {
                    Log.e(TAG, "loadProductDetails: Failed, code: " + response.code() + ", message: " + response.message());
                    runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Product not found on server", Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    public void updateRatingAndReviewCount(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            binding.ratingText.setText("0.0 Rating");
            binding.ratingBar.setRating(0);
            return;
        }

        double totalRating = 0;
        for (Review review : reviews) {
            totalRating += review.getRating();
        }
        double averageRating = totalRating / reviews.size();

        binding.ratingText.setText(String.format("%.1f Rating", averageRating));
        binding.ratingBar.setRating((float) averageRating);

        isRestoringTab = true;
        binding.viewPager.setCurrentItem(1, false);
        currentTabPosition = 1;
        isRestoringTab = false;
        Log.d(TAG, "Restored tab position after updateRatingAndReviewCount: " + currentTabPosition);
    }
}