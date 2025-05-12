package com.example.foodappfinal.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import com.example.foodappfinal.Adapter.CategoryAdapter;
import com.example.foodappfinal.Adapter.NotificationAdapter;
import com.example.foodappfinal.Adapter.PopularAdapter;
import com.example.foodappfinal.Adapter.SliderAdapter;
import com.example.foodappfinal.Domain.CategoryDomain;
import com.example.foodappfinal.Domain.ItemsDomain;
import com.example.foodappfinal.Domain.NotificationDomain;
import com.example.foodappfinal.Model.FileItem;
import com.example.foodappfinal.Model.Product;
import com.example.foodappfinal.Model.SliderItems;
import com.example.foodappproject.R;
import com.example.foodappproject.databinding.ActivityDashboardBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DashboardActivity extends BaseActivity {
    private static final String TAG = "DashboardActivity";
    private ActivityDashboardBinding binding;
    private static final String BANNER_LIST_URL = "https://fmheidqpephjpsgjuupf.supabase.co/storage/v1/object/list/banner";
    private static final String BANNER_BASE_IMAGE_URL = "https://fmheidqpephjpsgjuupf.supabase.co/storage/v1/object/public/banner/";
    private static final String CAT_LIST_URL = "https://fmheidqpephjpsgjuupf.supabase.co/storage/v1/object/list/cat";
    private static final String CAT_BASE_IMAGE_URL = "https://fmheidqpephjpsgjuupf.supabase.co/storage/v1/object/public/cat/";
    private static final String PRODUCTS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/products?select=*,brands(name),product_reviews(rating)";
    private static final String ITEM_BASE_IMAGE_URL = "https://fmheidqpephjpsgjuupf.supabase.co/storage/v1/object/public/item/";
    private static final String NOTIFICATIONS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/notifications?user_id=eq.%s&order=created_at.desc";
    private static final String UPDATE_NOTIFICATIONS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/notifications?user_id=eq.%s";
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private SliderAdapter sliderAdapter;
    private PopularAdapter popularAdapter;
    private NotificationAdapter notificationAdapter;
    private ArrayList<ItemsDomain> popularItems;
    private ArrayList<ItemsDomain> originalPopularItems;
    private ArrayList<NotificationDomain> notifications;
    private PopupWindow notificationPopup;
    private BroadcastReceiver reviewAddedReceiver;
    private BroadcastReceiver notificationAddedReceiver;
    private String selectedCategoryId;
    private int unreadCount;
    private Map<String, Boolean> notificationReadStatus;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting DashboardActivity");
        setContentView(R.layout.activity_dashboard);
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        popularItems = new ArrayList<>();
        originalPopularItems = new ArrayList<>();
        notifications = new ArrayList<>();
        notificationReadStatus = new HashMap<>();
        client = getOkHttpClient();
        popularAdapter = new PopularAdapter(this, popularItems);
        notificationAdapter = new NotificationAdapter(this, notifications, notification -> onNotificationRead(notification));
        Log.d(TAG, "onCreate: NotificationAdapter initialized with listener");
        binding.recyclerViewPopular.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerViewPopular.setAdapter(popularAdapter);

        initBanner();
        initCategory();
        initPopular();
        initNotifications();
        initBottomNavigation();
        setupSearchView();

        binding.seeAllText.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, AllProductsActivity.class));
        });

        binding.notificationLayout.setOnClickListener(v -> {
            Log.d(TAG, "Notification layout clicked, current dropdown visibility: " + (notificationPopup != null ? notificationPopup.isShowing() : "null"));
            if (notificationPopup == null || !notificationPopup.isShowing()) {
                showNotificationPopup();
            } else {
                notificationPopup.dismiss();
                Log.d(TAG, "Notification popup dismissed");
            }
        });

        reviewAddedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: Received broadcast for review added");
                initPopular();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(reviewAddedReceiver, new IntentFilter("com.example.foodappfinal.REVIEW_ADDED"));

        notificationAddedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: Received broadcast for notification added");
                initNotifications();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationAddedReceiver, new IntentFilter("com.example.foodappfinal.NOTIFICATION_ADDED"));

        Log.d(TAG, "onCreate: DashboardActivity initialization completed");
    }

    private void setupSearchView() {
        SearchView searchView = binding.searchView;
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterProducts(query, selectedCategoryId);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterProducts(newText, selectedCategoryId);
                return true;
            }
        });
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "SearchView focus changed: hasFocus=" + hasFocus);
            if (hasFocus && notificationPopup != null && notificationPopup.isShowing()) {
                notificationPopup.dismiss();
            }
        });
    }

    private void filterProducts(String query, String categoryId) {
        if ((query == null || query.trim().isEmpty()) && categoryId == null) {
            popularItems.clear();
            popularItems.addAll(originalPopularItems);
            popularAdapter.notifyDataSetChanged();
            return;
        }

        ArrayList<ItemsDomain> filteredItems = new ArrayList<>();
        String lowerQuery = query != null ? query.toLowerCase(Locale.getDefault()) : "";

        for (ItemsDomain item : originalPopularItems) {
            boolean matchesQuery = query == null || query.trim().isEmpty() || item.getTitle().toLowerCase(Locale.getDefault()).contains(lowerQuery);
            boolean matchesCategory = categoryId == null || item.getCategoryId() != null && categoryId.equals(item.getCategoryId());

            if (matchesQuery && matchesCategory) {
                filteredItems.add(item);
                continue;
            }

            if (matchesCategory && query != null && !query.trim().isEmpty()) {
                try {
                    double price = Double.parseDouble(lowerQuery);
                    if (item.getPrice() <= price) {
                        filteredItems.add(item);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        popularItems.clear();
        popularItems.addAll(filteredItems);
        popularAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(reviewAddedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationAddedReceiver);
        if (sliderRunnable != null) sliderHandler.removeCallbacks(sliderRunnable);
        if (notificationPopup != null) notificationPopup.dismiss();
        Log.d(TAG, "onDestroy: Unregistered receivers and dismissed popup");
    }

    private void initBottomNavigation() {
        Log.d(TAG, "initBottomNavigation: Setting up bottom navigation");
        binding.bottomAppBar.setVisibility(View.VISIBLE);
        binding.navHomeIcon.setOnClickListener(v -> resetDashboard());
        binding.navHomeText.setOnClickListener(v -> resetDashboard());
        binding.navWishlistIcon.setOnClickListener(v -> startActivity(new Intent(this, WishlistActivity.class)));
        binding.navWishlistText.setOnClickListener(v -> startActivity(new Intent(this, WishlistActivity.class)));
        binding.navCartIcon.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        binding.navCartText.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        binding.navProfileIcon.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        binding.navProfileText.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        Log.d(TAG, "initBottomNavigation: Bottom navigation setup completed");
    }

    private void resetDashboard() {
        Log.d(TAG, "resetDashboard: Resetting dashboard to initial state");
        binding.searchView.setQuery("", false);
        binding.searchView.clearFocus();
        selectedCategoryId = null;
        initBanner();
        initCategory();
        initPopular();
        initNotifications();
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.dismiss();
        }
        Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "resetDashboard: Dashboard refresh completed");
    }

    private void initPopular() {
        Log.d(TAG, "initPopular: Starting to load popular products");
        binding.progressBarPopular.setVisibility(View.VISIBLE);
        popularItems.clear();
        originalPopularItems.clear();
        OkHttpClient client = getOkHttpClient();
        String supabaseKey = getSupabaseKey();
        Log.d(TAG, "initPopular: Supabase key retrieved: " + (supabaseKey != null ? "valid" : "null"));

        Log.d(TAG, "initPopular: Fetching products from URL: " + PRODUCTS_URL);
        Request productRequest = new Request.Builder()
                .url(PRODUCTS_URL)
                .get()
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("apikey", supabaseKey)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(productRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "initPopular: Products fetch failed: " + e.getMessage());
                runOnUiThread(() -> {
                    binding.progressBarPopular.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "initPopular: Products fetch response received, code: " + response.code());
                if (response.isSuccessful()) {
                    String productData = response.body().string();
                    Log.d(TAG, "initPopular: Product data received: " + productData);
                    Gson gson = new Gson();
                    Type productListType = new TypeToken<List<Product>>(){}.getType();
                    List<Product> productList = gson.fromJson(productData, productListType);

                    if (productList == null || productList.isEmpty()) {
                        Log.w(TAG, "initPopular: No products found in response");
                        runOnUiThread(() -> {
                            binding.progressBarPopular.setVisibility(View.GONE);
                        });
                        return;
                    }

                    Log.d(TAG, "initPopular: Found " + productList.size() + " products");
                    for (Product product : productList) {
                        Log.d(TAG, "initPopular: Processing product ID: " + product.product_id);
                        ItemsDomain item = new ItemsDomain();
                        item.setTitle(product.name);
                        item.setDescription(product.description != null ? product.description : "No description");
                        item.setPrice(product.price);
                        item.setStock(product.stock);
                        item.setSold(product.sold);
                        item.setProductId(product.product_id);
                        item.setCategoryId(product.category_id != null ? product.category_id : "cat" + (productList.indexOf(product) % 5 + 1));
                        item.setNumberOfComments(0);

                        List<Product.Review> reviews = product.product_reviews;
                        double avgRating = 0.0;
                        int numberOfReviews = 0;
                        if (reviews != null && !reviews.isEmpty()) {
                            numberOfReviews = reviews.size();
                            double totalRating = 0.0;
                            for (Product.Review review : reviews) {
                                totalRating += review.rating;
                            }
                            avgRating = totalRating / numberOfReviews;
                            Log.d(TAG, "initPopular: Calculated for product " + product.product_id + ": avgRating=" + avgRating + ", numberOfReviews=" + numberOfReviews);
                        }

                        item.setRating(avgRating);
                        item.setNumberOfReviews(numberOfReviews);

                        ArrayList<String> picUrls = new ArrayList<>();
                        String formattedProductId = product.product_id;
                        String imageUrl = ITEM_BASE_IMAGE_URL + "item_" + formattedProductId + "/" + formattedProductId + "_1.png";
                        Log.d(TAG, "initPopular: Constructed image URL for product " + product.product_id + ": " + imageUrl);
                        picUrls.add(imageUrl);

                        item.setPicUrl(picUrls);
                        popularItems.add(item);
                        originalPopularItems.add(item);
                        Log.d(TAG, "initPopular: Added item for product " + product.product_id + " with image: " + picUrls.get(0));
                    }

                    runOnUiThread(() -> {
                        popularAdapter.notifyDataSetChanged();
                        binding.progressBarPopular.setVisibility(View.GONE);
                        Log.d(TAG, "initPopular: Popular products updated, total items: " + popularItems.size());
                    });
                } else {
                    Log.e(TAG, "initPopular: Products fetch failed, code: " + response.code() + ", message: " + response.message());
                    runOnUiThread(() -> {
                        binding.progressBarPopular.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void initBanner() {
        Log.d(TAG, "initBanner: Starting to load banners");
        binding.progressBarBanner.setVisibility(View.VISIBLE);
        ArrayList<SliderItems> items = new ArrayList<>();
        OkHttpClient client = getOkHttpClient();
        String supabaseKey = getSupabaseKey();
        Log.d(TAG, "initBanner: Supabase key retrieved: " + (supabaseKey != null ? "valid" : "null"));
        String json = "{\"prefix\": \"\"}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Log.d(TAG, "initBanner: Fetching banners from URL: " + BANNER_LIST_URL);
        Request request = new Request.Builder()
                .url(BANNER_LIST_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "initBanner: Banners fetch failed: " + e.getMessage());
                runOnUiThread(() -> {
                    binding.progressBarBanner.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "initBanner: Banners fetch response received, code: " + response.code());
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d(TAG, "initBanner: Banner data received: " + responseData);
                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<FileItem>>(){}.getType();
                    List<FileItem> fileList = gson.fromJson(responseData, listType);
                    if (fileList == null || fileList.isEmpty()) {
                        Log.w(TAG, "initBanner: No banners found in response");
                        runOnUiThread(() -> {
                            binding.progressBarBanner.setVisibility(View.GONE);
                        });
                        return;
                    }
                    Log.d(TAG, "initBanner: Found " + fileList.size() + " banner files");
                    for (FileItem file : fileList) {
                        if (file.name.equals(".emptyFolderPlaceholder")) continue;
                        String url = BANNER_BASE_IMAGE_URL + file.name;
                        SliderItems sliderItem = new SliderItems(url);
                        items.add(sliderItem);
                        Log.d(TAG, "initBanner: Added banner with URL: " + url);
                    }
                    runOnUiThread(() -> {
                        if (!items.isEmpty()) setupBanners(items);
                        binding.progressBarBanner.setVisibility(View.GONE);
                        Log.d(TAG, "initBanner: Banner loading completed");
                    });
                } else {
                    Log.e(TAG, "initBanner: Banners fetch failed, code: " + response.code() + ", message: " + response.message());
                    runOnUiThread(() -> {
                        binding.progressBarBanner.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void initCategory() {
        Log.d(TAG, "initCategory: Starting to load categories");
        binding.progressBarCategories.setVisibility(View.VISIBLE);
        ArrayList<CategoryDomain> items = new ArrayList<>();
        OkHttpClient client = getOkHttpClient();
        String supabaseKey = getSupabaseKey();
        Log.d(TAG, "initCategory: Supabase key retrieved: " + (supabaseKey != null ? "valid" : "null"));
        String json = "{\"prefix\": \"\"}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Log.d(TAG, "initCategory: Fetching categories from URL: " + CAT_LIST_URL);
        Request request = new Request.Builder()
                .url(CAT_LIST_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "initCategory: Categories fetch failed: " + e.getMessage());
                runOnUiThread(() -> {
                    binding.progressBarCategories.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "initCategory: Categories fetch response received, code: " + response.code());
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d(TAG, "initCategory: Category data received: " + responseData);
                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<FileItem>>(){}.getType();
                    List<FileItem> fileList = gson.fromJson(responseData, listType);
                    if (fileList == null || fileList.isEmpty()) {
                        Log.w(TAG, "initCategory: No categories found in response");
                        runOnUiThread(() -> {
                            binding.progressBarCategories.setVisibility(View.GONE);
                        });
                        return;
                    }
                    Log.d(TAG, "initCategory: Found " + fileList.size() + " category files");
                    for (FileItem file : fileList) {
                        if (file.name.equals(".emptyFolderPlaceholder")) continue;
                        CategoryDomain category = new CategoryDomain();
                        String title = file.name.replace(".png", "");
                        category.setTitle(title);
                        category.setCategoryId(title);
                        String picUrl = CAT_BASE_IMAGE_URL + file.name;
                        category.setPicUrl(picUrl);
                        items.add(category);
                        Log.d(TAG, "initCategory: Added category: " + title + " with URL: " + picUrl);
                    }
                    runOnUiThread(() -> {
                        if (!items.isEmpty()) {
                            binding.recyclerViewCategories.setLayoutManager(new GridLayoutManager(DashboardActivity.this, 5));
                            binding.recyclerViewCategories.setAdapter(new CategoryAdapter(items, categoryId -> {
                                selectedCategoryId = categoryId;
                                filterProducts(binding.searchView.getQuery().toString(), selectedCategoryId);
                            }));
                        }
                        binding.progressBarCategories.setVisibility(View.GONE);
                        Log.d(TAG, "initCategory: Category loading completed");
                    });
                } else {
                    Log.e(TAG, "initCategory: Categories fetch failed, code: " + response.code() + ", message: " + response.message());
                    runOnUiThread(() -> {
                        binding.progressBarCategories.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void setupBanners(ArrayList<SliderItems> items) {
        Log.d(TAG, "setupBanners: Setting up banner slider with " + items.size() + " items");
        sliderAdapter = new SliderAdapter(items, binding.viewPagerBanner);
        binding.viewPagerBanner.setAdapter(sliderAdapter);
        binding.viewPagerBanner.setClipToPadding(true);
        binding.viewPagerBanner.setClipChildren(true);
        binding.viewPagerBanner.setOffscreenPageLimit(1);
        binding.viewPagerBanner.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer(new MarginPageTransformer(16));
        binding.viewPagerBanner.setPageTransformer(compositePageTransformer);
        if (sliderRunnable != null) sliderHandler.removeCallbacks(sliderRunnable);
        sliderRunnable = () -> {
            int currentItem = binding.viewPagerBanner.getCurrentItem();
            int totalItems = items.size();
            if (totalItems > 0) {
                int nextItem = (currentItem + 1) % totalItems;
                binding.viewPagerBanner.setCurrentItem(nextItem, true);
                sliderHandler.postDelayed(sliderRunnable, 3000);
            }
        };
        sliderHandler.postDelayed(sliderRunnable, 3000);
        Log.d(TAG, "setupBanners: Banner slider setup completed");
    }

    private void initNotifications() {
        Log.d(TAG, "initNotifications: Starting to load notifications");
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "initNotifications: User ID is null, skipping notification fetch");
            runOnUiThread(() -> {
                notifications.clear();
                binding.notificationCount.setText("0");
                binding.notificationCount.setVisibility(View.GONE);
                notificationAdapter.updateNotifications(notifications);
                Log.d(TAG, "initNotifications: Cleared notifications due to null user ID");
            });
            return;
        }

        Log.d(TAG, "initNotifications: User ID: " + userId);
        String url = String.format(NOTIFICATIONS_URL, userId);
        Log.d(TAG, "initNotifications: Fetching notifications from URL: " + url);
        Request request = getRequestBuilder().url(url).get().build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "initNotifications: Notifications fetch failed: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this, "Failed to load notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "initNotifications: Response received, code: " + response.code());
                Log.d(TAG, "initNotifications: Response body: " + responseBody);

                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<NotificationDomain>>(){}.getType();
                    List<NotificationDomain> tempNotifications = gson.fromJson(responseBody, listType);

                    if (tempNotifications == null) {
                        Log.w(TAG, "initNotifications: Failed to parse notifications, tempNotifications is null");
                        runOnUiThread(() -> {
                            Toast.makeText(DashboardActivity.this, "Failed to parse notifications", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    runOnUiThread(() -> {
                        Map<String, NotificationDomain> existingNotifications = new HashMap<>();
                        for (NotificationDomain n : notifications) {
                            existingNotifications.put(n.getId(), n);
                        }

                        notifications.clear();
                        if (!tempNotifications.isEmpty()) {
                            for (NotificationDomain newNotif : tempNotifications) {
                                NotificationDomain existing = existingNotifications.get(newNotif.getId());
                                if (existing != null && existing.isRead()) {
                                    newNotif.setRead(true);
                                }
                                if (notificationReadStatus.containsKey(newNotif.getId())) {
                                    newNotif.setRead(notificationReadStatus.get(newNotif.getId()));
                                }
                                notifications.add(newNotif);
                            }
                            Log.d(TAG, "initNotifications: Loaded " + notifications.size() + " notifications");
                        } else {
                            Log.d(TAG, "initNotifications: No notifications found in response");
                        }
                        updateUnreadCount();
                        Log.d(TAG, "initNotifications: Before update adapter, notifications size: " + notifications.size());
                        notificationAdapter.updateNotifications(new ArrayList<>(notifications));
                        Log.d(TAG, "initNotifications: Updated notifications, total count: " + notifications.size() + ", unread count: " + unreadCount);
                    });
                } else {
                    Log.e(TAG, "initNotifications: Notifications fetch failed, code: " + response.code() + ", message: " + response.message());
                    Log.e(TAG, "initNotifications: Response body: " + responseBody);
                    runOnUiThread(() -> {
                        Toast.makeText(DashboardActivity.this, "Failed to load notifications: " + response.message(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void updateUnreadCount() {
        unreadCount = (int) notifications.stream().filter(n -> !n.isRead()).count();
        binding.notificationCount.setText(String.valueOf(unreadCount));
        binding.notificationCount.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
        Log.d(TAG, "updateUnreadCount: Updated unread count to " + unreadCount);
    }

    private void showNotificationPopup() {
        Log.d(TAG, "showNotificationPopup: Attempting to show notification popup");
        Log.d(TAG, "showNotificationPopup: Current notifications size: " + notifications.size());
        View popupView = LayoutInflater.from(this).inflate(R.layout.notification_dropdown, null);
        RecyclerView recyclerView = popupView.findViewById(R.id.notificationRecyclerView);
        TextView noNotificationsText = popupView.findViewById(R.id.noNotificationsText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(notificationAdapter);
        recyclerView.setHasFixedSize(true);

        if (notifications.isEmpty()) {
            noNotificationsText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            Log.d(TAG, "showNotificationPopup: No notifications, showing 'No notifications' text");
        } else {
            noNotificationsText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            Log.d(TAG, "showNotificationPopup: Showing " + Math.min(notifications.size(), 5) + " notifications");
        }

        int popupWidth = 1100;
        int popupHeight = notifications.isEmpty() ? 100 : Math.min(Math.min(notifications.size(), 5) * 200, 1000);

        notificationPopup = new PopupWindow(popupView, popupWidth, popupHeight, true);
        notificationPopup.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame));
        notificationPopup.setElevation(24f);
        notificationPopup.setOutsideTouchable(true);
        notificationPopup.setFocusable(true);

        int[] location = new int[2];
        binding.notificationIcon.getLocationOnScreen(location);
        int x = location[0] - popupWidth + binding.notificationIcon.getWidth();
        int y = location[1] + binding.notificationIcon.getHeight();
        notificationPopup.showAtLocation(binding.notificationLayout, Gravity.TOP | Gravity.START, x, y);

        Log.d(TAG, "showNotificationPopup: Popup shown at x=" + x + ", y=" + y + ", width=" + popupWidth + ", height=" + popupHeight);
    }

    private void onNotificationRead(NotificationDomain notification) {
        Log.d(TAG, "onNotificationRead: Marking notification as read, ID: " + notification.getId());
        markAsRead(notification, success -> {
            if (success) {
                notification.setRead(true);
                notificationReadStatus.put(notification.getId(), true);
                updateUnreadCount();
                Log.d(TAG, "onNotificationRead: Updated notification state and unread count");
            } else {
                Log.w(TAG, "onNotificationRead: Failed to mark notification as read on server, not updating local state");
            }
        });
    }

    private void markAsRead(NotificationDomain notification, CallbackResult callback) {
        if (notification.getId() != null) {
            String userId = getCurrentUserId();
            if (userId == null) {
                Log.w(TAG, "markAsRead: User ID is null, cannot update notification");
                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this, "User not authenticated. Please log in again.", Toast.LENGTH_LONG).show();
                    callback.onResult(false);
                });
                return;
            }

            String url = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/notifications?notification_id=eq." + notification.getId() + "&user_id=eq." + userId;
            String json = "{\"is_read\": true, \"user_id\": \"" + userId + "\"}";
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

            Log.d(TAG, "markAsRead: Sending PATCH request to URL: " + url);
            Log.d(TAG, "markAsRead: Request body: " + json);
            Log.d(TAG, "markAsRead: Current user ID: " + userId);

            // Sử dụng access token từ getRequestBuilder
            Request request;
            try {
                request = getRequestBuilder()
                        .url(url)
                        .patch(body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .build();
            } catch (IllegalStateException e) {
                Log.e(TAG, "markAsRead: Access token is missing, attempting to refresh: " + e.getMessage());
                refreshToken(new RefreshTokenCallback() {
                    @Override
                    public void onSuccess(String newAccessToken) {
                        Log.d(TAG, "markAsRead: Token refreshed successfully, retrying request");
                        Request retryRequest = getRequestBuilder()
                                .url(url)
                                .patch(body)
                                .addHeader("Content-Type", "application/json")
                                .addHeader("Prefer", "return=representation")
                                .build();
                        sendPatchRequest(retryRequest, notification, callback);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "markAsRead: Failed to refresh token: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(DashboardActivity.this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                            callback.onResult(false);
                        });
                    }
                });
                return;
            }

            sendPatchRequest(request, notification, callback);
        } else {
            Log.w(TAG, "markAsRead: Notification ID is null, cannot update");
            callback.onResult(false);
        }
    }

    private void sendPatchRequest(Request request, NotificationDomain notification, CallbackResult callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "markAsRead: Failed to update notification: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this, "Failed to mark notification as read: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    callback.onResult(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                Log.d(TAG, "markAsRead: Response received, code: " + response.code());
                Log.d(TAG, "markAsRead: Response body: " + responseBody);

                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<NotificationDomain>>(){}.getType();
                    List<NotificationDomain> updatedRecords = gson.fromJson(responseBody, listType);

                    if (updatedRecords != null && !updatedRecords.isEmpty()) {
                        Log.d(TAG, "markAsRead: Successfully updated notification ID: " + notification.getId());
                        runOnUiThread(() -> callback.onResult(true));
                    } else {
                        Log.w(TAG, "markAsRead: No records updated for notification ID: " + notification.getId());
                        runOnUiThread(() -> {
                            Toast.makeText(DashboardActivity.this, "Failed to mark notification as read: No records updated", Toast.LENGTH_SHORT).show();
                            callback.onResult(false);
                        });
                    }
                } else {
                    Log.e(TAG, "markAsRead: Failed, code: " + response.code() + ", message: " + response.message());
                    Log.e(TAG, "markAsRead: Response body: " + responseBody);
                    runOnUiThread(() -> {
                        Toast.makeText(DashboardActivity.this, "Failed to mark notification as read: " + response.message(), Toast.LENGTH_SHORT).show();
                        callback.onResult(false);
                    });
                }
            }
        });
    }

    private interface CallbackResult {
        void onResult(boolean success);
    }

    private void markNotificationsAsRead() {
        Log.d(TAG, "markNotificationsAsRead: No action needed, handled individually");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity resumed");
        if (sliderRunnable != null) sliderHandler.postDelayed(sliderRunnable, 3000);
        initNotifications();
        Log.d(TAG, "onResume: Resumed slider callbacks and reloaded notifications");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sliderRunnable != null) sliderHandler.removeCallbacks(sliderRunnable);
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.dismiss();
        }
        Log.d(TAG, "onPause: Removed slider callbacks and dismissed popup");
    }
}