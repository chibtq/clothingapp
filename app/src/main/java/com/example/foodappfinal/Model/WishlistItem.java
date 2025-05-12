package com.example.foodappfinal.Model;

public class WishlistItem {
    public String wishlist_id;
    public String user_id;
    public String product_id;
    public Product products;

    public String getProductId() {
        return product_id;
    }

    public String getWishlistId() {
        return wishlist_id;
    }

    public String getProductName() {
        return products != null ? products.name : "Unknown Product";
    }

    public double getProductPrice() {
        return products != null ? products.price : 0.0;
    }

    public String getProductImageUrl() {
        return "https://fmheidqpephjpsgjuupf.supabase.co/storage/v1/object/public/item/item_" + product_id + "/" + product_id + "_1.png";
    }
}