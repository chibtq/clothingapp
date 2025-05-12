package com.example.foodappfinal.Domain;

public class ReviewDomain {
    public String review_id;
    public String product_id;
    public String user_id;
    public int rating;
    public String comment;
    public String created_at;
    public UserProfile user_profiles;

    public static class UserProfile {
        public String username;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public String getUsername() {
        return user_profiles != null ? user_profiles.username : "Anonymous";
    }
}
