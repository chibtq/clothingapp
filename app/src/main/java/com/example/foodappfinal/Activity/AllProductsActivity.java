package com.example.foodappfinal.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.foodappfinal.Adapter.PopularAdapter;
import com.example.foodappfinal.Domain.ItemsDomain;
import com.example.foodappfinal.Model.Product;
import com.example.foodappproject.R;
import com.example.foodappproject.databinding.ActivityAllProductsBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AllProductsActivity extends BaseActivity {
    private static final String TAG = "AllProductsActivity";
    private ActivityAllProductsBinding binding;
    private static final String PRODUCTS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/products?select=*,brands(name),product_reviews(rating)";
    private static final String ITEM_BASE_IMAGE_URL = "https://fmheidqpephjpsgjuupf.supabase.co/storage/v1/object/public/item/";
    private PopularAdapter adapter;
    private ArrayList<ItemsDomain> items;
    private ArrayList<ItemsDomain> originalItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAllProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        items = new ArrayList<>();
        originalItems = new ArrayList<>();
        adapter = new PopularAdapter(this, items);
        binding.recyclerViewProducts.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerViewProducts.setAdapter(adapter);

        loadProducts();
        setupSearchView();
        setupFiltersAndSort();
    }

    private void loadProducts() {
        binding.progressBar.setVisibility(View.VISIBLE);
        items.clear();
        originalItems.clear();
        OkHttpClient client = getOkHttpClient();
        String supabaseKey = getSupabaseKey();

        Request request = new Request.Builder()
                .url(PRODUCTS_URL)
                .get()
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("apikey", supabaseKey)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "loadProducts: Failed: " + e.getMessage());
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(AllProductsActivity.this, "Failed to load products", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String productData = response.body().string();
                    Gson gson = new Gson();
                    Type productListType = new TypeToken<List<Product>>(){}.getType();
                    List<Product> productList = gson.fromJson(productData, productListType);

                    if (productList == null || productList.isEmpty()) {
                        runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(AllProductsActivity.this, "No products found", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    for (Product product : productList) {
                        ItemsDomain item = new ItemsDomain();
                        item.setTitle(product.name);
                        item.setDescription(product.description != null ? product.description : "No description");
                        item.setPrice(product.price);
                        item.setStock(product.stock);
                        item.setSold(product.sold);
                        item.setProductId(product.product_id);
                        item.setCategoryId(product.category_id);
                        item.setNumberOfComments(0); // Set to 0 since comments are not supported yet

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
                        }
                        item.setRating(avgRating);
                        item.setNumberOfReviews(numberOfReviews);

                        ArrayList<String> picUrls = new ArrayList<>();
                        String formattedProductId = product.product_id;
                        String imageUrl = ITEM_BASE_IMAGE_URL + "item_" + formattedProductId + "/" + formattedProductId + "_1.png";
                        picUrls.add(imageUrl);
                        item.setPicUrl(picUrls);

                        items.add(item);
                        originalItems.add(item);
                    }

                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        binding.progressBar.setVisibility(View.GONE);
                    });
                } else {
                    runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(AllProductsActivity.this, "Failed to load products", Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void setupSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterProducts(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterProducts(newText);
                return true;
            }
        });
    }

    private void filterProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            items.clear();
            items.addAll(originalItems);
            adapter.notifyDataSetChanged();
            return;
        }

        ArrayList<ItemsDomain> filteredItems = new ArrayList<>();
        String lowerQuery = query.toLowerCase(Locale.getDefault());

        for (ItemsDomain item : originalItems) {
            if (item.getTitle().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                filteredItems.add(item);
            }
        }

        items.clear();
        items.addAll(filteredItems);
        adapter.notifyDataSetChanged();
    }

    private void setupFiltersAndSort() {
        binding.filterPriceButton.setOnClickListener(v -> {
            Collections.sort(items, (a, b) -> Double.compare(a.getPrice(), b.getPrice()));
            adapter.notifyDataSetChanged();
        });

        binding.sortRatingButton.setOnClickListener(v -> {
            Collections.sort(items, (a, b) -> Double.compare(b.getRating(), a.getRating()));
            adapter.notifyDataSetChanged();
        });

        binding.sortSoldButton.setOnClickListener(v -> {
            Collections.sort(items, (a, b) -> Integer.compare(b.getSold(), a.getSold()));
            adapter.notifyDataSetChanged();
        });
    }
}