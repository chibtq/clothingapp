package com.example.foodappfinal.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.foodappfinal.Model.Review;
import com.example.foodappproject.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    private ArrayList<Review> reviews;
    private Context context;

    public ReviewAdapter(ArrayList<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);

        // Hiển thị tên người dùng
        holder.userName.setText(review.getUsername());

        // Hiển thị ngày tạo
        if (review.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            holder.createdAt.setText(sdf.format(review.getCreatedAt()));
        } else {
            holder.createdAt.setText("N/A");
        }

        // Hiển thị bình luận
        holder.userComment.setText(review.getComment());

        // Hiển thị đánh giá
        holder.userRating.setText(String.format(Locale.getDefault(), "%.1f", review.getRating()));

        // Tải và hiển thị ảnh đại diện
        String avatarUrl = review.getUserProfile() != null ? review.getUserProfile().getAvatarUrl() : null;
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.user_1) // Hình ảnh mặc định nếu không tải được
                    .error(R.drawable.user_1) // Hình ảnh mặc định nếu có lỗi
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Bỏ qua disk cache
                    .skipMemoryCache(true) // Bỏ qua memory cache
                    .circleCrop() // Tùy chọn: làm hình ảnh thành hình tròn
                    .into(holder.userAvatar);
        } else {
            holder.userAvatar.setImageResource(R.drawable.user_1); // Hình ảnh mặc định nếu không có avatarUrl
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userAvatar, starIcon;
        TextView userName, createdAt, userComment, userRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            createdAt = itemView.findViewById(R.id.createdAt);
            userComment = itemView.findViewById(R.id.userComment);
            userRating = itemView.findViewById(R.id.userRating);
            starIcon = itemView.findViewById(R.id.starIcon);
        }
    }
}