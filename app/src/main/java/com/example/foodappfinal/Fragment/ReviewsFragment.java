package com.example.foodappfinal.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.foodappfinal.Activity.BaseActivity;
import com.example.foodappfinal.Activity.DetailActivity;
import com.example.foodappfinal.Adapter.ReviewAdapter;
import com.example.foodappfinal.Model.Review;
import com.example.foodappproject.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewsFragment extends Fragment {
    private static final String ARG_RATING = "rating";
    private static final String ARG_REVIEW_COUNT = "reviewCount";
    private static final String ARG_PRODUCT_ID = "productId";
    private double rating;
    private int reviewCount;
    private String productId;
    private ArrayList<Review> reviews;
    private ReviewAdapter reviewAdapter;
    private RecyclerView reviewsRecyclerView;
    private EditText commentInput;

    public ReviewsFragment() {
    }

    public static ReviewsFragment newInstance(String productId, double rating, int reviewCount) {
        ReviewsFragment fragment = new ReviewsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_ID, productId);
        args.putDouble(ARG_RATING, rating);
        args.putInt(ARG_REVIEW_COUNT, reviewCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString(ARG_PRODUCT_ID);
            rating = getArguments().getDouble(ARG_RATING);
            reviewCount = getArguments().getInt(ARG_REVIEW_COUNT);
        }

        reviews = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviews);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_review, container, false);

        // Set up RecyclerView
        reviewsRecyclerView = view.findViewById(R.id.reviewsRecyclerView);
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reviewsRecyclerView.setAdapter(reviewAdapter);

        // Set up RatingBar, EditText, and Button
        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        commentInput = view.findViewById(R.id.commentInput);
        Button submitReviewButton = view.findViewById(R.id.submitReviewButton);

        // Prevent layout pass from affecting ViewPager2 by delaying focus handling
        commentInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Post a delayed action to ensure layout pass is complete
                v.post(() -> {
                    if (getActivity() instanceof DetailActivity) {
                        // Ensure the Reviews tab remains active
                        ((DetailActivity) getActivity()).updateRatingAndReviewCount(reviews);
                    }
                });
            }
        });

        submitReviewButton.setOnClickListener(v -> {
            float userRating = ratingBar.getRating();
            String comment = commentInput.getText().toString().trim();
            if (comment.isEmpty()) {
                Toast.makeText(getContext(), "Please write a review", Toast.LENGTH_SHORT).show();
                return;
            }
            if (userRating == 0) {
                Toast.makeText(getContext(), "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            // Submit review to Supabase
            submitReviewToSupabase(comment, userRating, ratingBar, commentInput);
        });

        // Load reviews from Supabase
        loadReviewsFromSupabase();

        return view;
    }

    private void loadReviewsFromSupabase() {
        BaseActivity activity = (BaseActivity) getActivity();
        // Modified URL to include avatar_url in user_profiles
        String url = activity.getSupabaseUrl() + "/rest/v1/product_reviews?product_id=eq." + productId + "&select=*,user_profiles(username,avatar_url)&order=created_at.desc";
        Request request = activity.getRequestBuilder()
                .url(url)
                .get()
                .build();

        activity.getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to load reviews: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d("ReviewsFragment", "Response Data: " + responseData); // Log dữ liệu để kiểm tra
                    Gson gson = new GsonBuilder()
                            .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> {
                                try {
                                    String dateStr = json.getAsString();
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
                                    return sdf.parse(dateStr);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            })
                            .create();
                    Type reviewListType = new TypeToken<List<Review>>() {}.getType();
                    List<Review> loadedReviews = gson.fromJson(responseData, reviewListType);
                    for (Review review : loadedReviews) {
                        Log.d("ReviewsFragment", "Review: " + review.getComment() + ", Username: " + review.getUsername() + ", Avatar URL: " + (review.getUserProfile() != null ? review.getUserProfile().getAvatarUrl() : "null"));
                    }
                    getActivity().runOnUiThread(() -> {
                        reviews.clear();
                        reviews.addAll(loadedReviews);
                        reviewAdapter.notifyDataSetChanged();
                        reviewsRecyclerView.scrollToPosition(0);

                        if (getActivity() instanceof DetailActivity) {
                            ((DetailActivity) getActivity()).updateRatingAndReviewCount(reviews);
                        }
                    });
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to load reviews: HTTP " + response.code() + " - " + errorBody, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void submitReviewToSupabase(String comment, float userRating, RatingBar ratingBar, EditText commentInput) {
        BaseActivity activity = (BaseActivity) getActivity();
        String userId = activity.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "You must be logged in to submit a review", Toast.LENGTH_SHORT).show();
            activity.logout();
            return;
        }

        String accessToken = activity.getAccessToken();
        if (accessToken == null) {
            Toast.makeText(getContext(), "Authentication token missing", Toast.LENGTH_SHORT).show();
            activity.logout();
            return;
        }

        String json = String.format(
                "{\"product_id\":\"%s\",\"user_id\":\"%s\",\"rating\":%d,\"comment\":\"%s\"}",
                productId, userId, Math.round(userRating), comment.replace("\"", "\\\"")
        );
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        String url = activity.getSupabaseUrl() + "/rest/v1/product_reviews";

        // Initial request to submit the review
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", activity.getSupabaseKey())
                .build();

        activity.getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to submit review: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    getActivity().runOnUiThread(() -> {
                        // Clear the input fields
                        commentInput.setText("");
                        ratingBar.setRating(0);
                        Toast.makeText(getContext(), "Review submitted", Toast.LENGTH_SHORT).show();

                        // Reload reviews from Supabase to get the latest data
                        loadReviewsFromSupabase();
                    });
                } else if (response.code() == 401) {
                    // Token might be expired, attempt to refresh
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Session expired, refreshing token...", Toast.LENGTH_SHORT).show());
                    activity.refreshToken(new BaseActivity.RefreshTokenCallback() {
                        @Override
                        public void onSuccess(String newAccessToken) {
                            // Retry the request with the new token
                            Request retryRequest = new Request.Builder()
                                    .url(url)
                                    .post(body)
                                    .addHeader("Authorization", "Bearer " + newAccessToken)
                                    .addHeader("apikey", activity.getSupabaseKey())
                                    .build();

                            activity.getOkHttpClient().newCall(retryRequest).enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "Failed to submit review after refresh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    if (response.isSuccessful()) {
                                        getActivity().runOnUiThread(() -> {
                                            // Clear the input fields
                                            commentInput.setText("");
                                            ratingBar.setRating(0);
                                            Toast.makeText(getContext(), "Review submitted", Toast.LENGTH_SHORT).show();

                                            // Reload reviews from Supabase to get the latest data
                                            loadReviewsFromSupabase();
                                        });
                                    } else {
                                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "Failed to submit review after refresh: HTTP " + response.code() + " - " + errorBody, Toast.LENGTH_LONG).show();
                                            activity.logout();
                                        });
                                    }
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Failed to refresh token: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                activity.logout();
                            });
                        }
                    });
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to submit review: HTTP " + response.code() + " - " + errorBody, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
}