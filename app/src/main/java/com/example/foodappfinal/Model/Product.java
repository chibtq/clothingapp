package com.example.foodappfinal.Model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Product {
    @SerializedName("product_id")
    public String product_id;

    @SerializedName("name")
    public String name;

    @SerializedName("description")
    public String description;

    @SerializedName("price")
    public double price;

    @SerializedName("stock")
    public int stock;

    @SerializedName("sold")
    public int sold;

    @SerializedName("brand_id")
    public String brand_id;

    @SerializedName("category_id")
    public String category_id;

    @SerializedName("size")
    public String size;

    @SerializedName("average_rating")
    public double average_rating;

    @SerializedName("number_of_reviews")
    public int number_of_reviews;

    @SerializedName("number_of_comments")
    public int number_of_comments; // Added field

    @SerializedName("created_at")
    public String created_at;

    @SerializedName("brands")
    public Brand brands;

    @SerializedName("product_reviews")
    public List<Review> product_reviews;

    public static class Brand {
        @SerializedName("name")
        public String name;
    }

    public static class Review {
        @SerializedName("rating")
        public int rating;
    }

    // Phương thức để lấy danh sách size
    public List<String> getSizes() {
        if (size == null || size.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(size.split(","));
    }
}