package com.example.foodappfinal.Model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class Review {
    @SerializedName("review_id")
    private String reviewId;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("user_profiles")
    private UserProfile userProfile;

    private String comment;
    private float rating;

    @SerializedName("created_at")
    private Date createdAt;

    public Review() {
    }

    public Review(String comment, float rating) {
        this.comment = comment;
        this.rating = rating;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return userProfile != null ? userProfile.getUsername() : "Anonymous";
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}