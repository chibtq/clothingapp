package com.example.foodappfinal.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.foodappfinal.Fragment.DescriptionFragment;
import com.example.foodappfinal.Fragment.ReviewsFragment;
import com.example.foodappfinal.Fragment.SoldFragment;

public class TabPagerAdapter extends FragmentStateAdapter {
    private String description;
    private String productId;
    private double rating;
    private int reviewCount;
    private int stock;
    private int sold;
    private final String[] tabTitles = {"Description", "Reviews", "Sold"};

    public TabPagerAdapter(@NonNull FragmentActivity fragmentActivity, String description, String productId, double rating, int reviewCount, int stock, int sold) {
        super(fragmentActivity);
        this.description = description;
        this.productId = productId;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.stock = stock;
        this.sold = sold;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return DescriptionFragment.newInstance(description);
            case 1:
                return ReviewsFragment.newInstance(productId, rating, reviewCount);
            case 2:
                return SoldFragment.newInstance(stock, sold);
            default:
                throw new IllegalStateException("Unexpected position " + position);
        }
    }

    @Override
    public int getItemCount() {
        return tabTitles.length;
    }

    public String getTabTitle(int position) {
        return tabTitles[position];
    }

    // Add method to update product details
    public void updateProductDetails(String description, String productId, double rating, int reviewCount, int stock, int sold) {
        this.description = description;
        this.productId = productId;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.stock = stock;
        this.sold = sold;
        notifyDataSetChanged(); // Notify the adapter to refresh fragments
    }
}