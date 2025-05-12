package com.example.foodappfinal.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.foodappfinal.Activity.DetailActivity;
import com.example.foodappfinal.Domain.ItemsDomain;
import com.example.foodappproject.R;

import java.util.ArrayList;

public class PopularAdapter extends RecyclerView.Adapter<PopularAdapter.ViewHolder> {
    private ArrayList<ItemsDomain> items;
    private Context context;

    public PopularAdapter(Context context, ArrayList<ItemsDomain> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_popular, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemsDomain item = items.get(position);

        holder.titleText.setText(item.getTitle());
        holder.priceText.setText(String.format("%.2fđ", item.getPrice()));
        holder.ratingBar.setRating((float) item.getRating());
        holder.numberOfReviewsText.setText("(" + item.getNumberOfReviews() + ")");
        holder.numberOfCommentsText.setText(String.valueOf(item.getNumberOfComments()));

        if (item.getPicUrl() != null && !item.getPicUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getPicUrl().get(0))
                    .placeholder(R.drawable.placeholder_image)
                    .into(holder.pic);
        }

        // Handle item click to go to DetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("object", item);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView pic, commentIcon;
        TextView titleText, priceText, numberOfReviewsText, numberOfCommentsText;
        RatingBar ratingBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            pic = itemView.findViewById(R.id.pic);
            titleText = itemView.findViewById(R.id.titleText);
            priceText = itemView.findViewById(R.id.priceText);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            numberOfReviewsText = itemView.findViewById(R.id.numberOfReviewsText);
            commentIcon = itemView.findViewById(R.id.commentIcon);
            numberOfCommentsText = itemView.findViewById(R.id.numberOfCommentsText);
        }
    }
}