package com.example.foodappfinal.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.foodappfinal.Adapter.WishlistAdapter;
import com.example.foodappfinal.Domain.ItemsDomain;
import com.example.foodappfinal.Model.Product;
import com.example.foodappfinal.Model.WishlistItem;
import com.example.foodappproject.R;
import com.example.foodappproject.databinding.ActivityWishlistBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class WishlistActivity extends BaseActivity {
    private static final String TAG = "WishlistActivity";
    private ActivityWishlistBinding binding;
    private ArrayList<WishlistItem> wishlistItems;
    private WishlistAdapter wishlistAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWishlistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Thiết lập nút quay lại
        binding.backButton.setOnClickListener(v -> finish());

        // Khởi tạo RecyclerView
        wishlistItems = new ArrayList<>();
        wishlistAdapter = new WishlistAdapter(wishlistItems, item -> {
            // Xử lý khi nhấn vào sản phẩm trong Wishlist
            Intent intent = new Intent(WishlistActivity.this, DetailActivity.class);
            intent.putExtra("object", convertToItemsDomain(item));
            startActivity(intent);
        }, item -> {
            // Xử lý khi nhấn nút xóa
            removeFromWishlist(item);
        });
        binding.wishlistRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.wishlistRecyclerView.setAdapter(wishlistAdapter);

        // Tải dữ liệu Wishlist từ Supabase
        loadWishlist();
    }

    private void loadWishlist() {
        String userId = getCurrentUserId();
        if (userId == null) {
            runOnUiThread(() -> {
                Toast.makeText(this, "You must be logged in to view wishlist", Toast.LENGTH_SHORT).show();
                logout();
            });
            return;
        }

        // Hiển thị ProgressBar khi bắt đầu tải
        runOnUiThread(() -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.wishlistRecyclerView.setVisibility(View.GONE);
            binding.emptyWishlistText.setVisibility(View.GONE);
        });

        String url = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/wishlist?user_id=eq." + userId + "&select=*,products(*,product_reviews(rating))";
        Request request = getRequestBuilder()
                .url(url)
                .get()
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "loadWishlist: Failed: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(WishlistActivity.this, "Failed to load wishlist", Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Gson gson = new Gson();
                    Type wishlistType = new TypeToken<List<WishlistItem>>(){}.getType();
                    List<WishlistItem> wishlistData = gson.fromJson(responseData, wishlistType);

                    wishlistItems.clear();
                    if (wishlistData != null) {
                        wishlistItems.addAll(wishlistData);
                    }

                    runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        wishlistAdapter.notifyDataSetChanged();
                        binding.wishlistRecyclerView.setVisibility(View.VISIBLE);
                        binding.emptyWishlistText.setVisibility(wishlistItems.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(WishlistActivity.this, "Failed to load wishlist", Toast.LENGTH_SHORT).show();
                        binding.progressBar.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void removeFromWishlist(WishlistItem item) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "You must be logged in to remove from wishlist", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        // Hiển thị ProgressBar khi xóa
        runOnUiThread(() -> binding.progressBar.setVisibility(View.VISIBLE));

        String url = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/wishlist?wishlist_id=eq." + item.getWishlistId();
        Request request = getRequestBuilder()
                .url(url)
                .delete()
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "removeFromWishlist: Failed: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(WishlistActivity.this, "Failed to remove from wishlist", Toast.LENGTH_SHORT).show();
                    binding.progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        wishlistItems.remove(item);
                        wishlistAdapter.notifyDataSetChanged();
                        binding.progressBar.setVisibility(View.GONE);
                        binding.emptyWishlistText.setVisibility(wishlistItems.isEmpty() ? View.VISIBLE : View.GONE);
                        Toast.makeText(WishlistActivity.this, "Removed from wishlist", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(WishlistActivity.this, "Failed to remove from wishlist", Toast.LENGTH_SHORT).show();
                        binding.progressBar.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private ItemsDomain convertToItemsDomain(WishlistItem wishlistItem) {
        ItemsDomain domainItem = new ItemsDomain();
        Product product = wishlistItem.products;

        domainItem.setTitle(product.name);
        domainItem.setDescription(product.description != null ? product.description : "No description");
        domainItem.setPrice(product.price);
        domainItem.setStock(product.stock);
        domainItem.setSold(product.sold);
        domainItem.setProductId(product.product_id);
        domainItem.setCategoryId(product.category_id != null ? product.category_id : "unknown");
        domainItem.setSize(product.size);
        domainItem.setRating(product.average_rating);
        domainItem.setNumberOfReviews(product.number_of_reviews);
        domainItem.setNumberOfComments(0); // Set to 0 since comments are not supported yet

        ArrayList<String> picUrls = new ArrayList<>();
        String imageUrl = wishlistItem.getProductImageUrl();
        picUrls.add(imageUrl);
        domainItem.setPicUrl(picUrls);

        return domainItem;
    }
}