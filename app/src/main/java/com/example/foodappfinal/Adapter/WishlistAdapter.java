package com.example.foodappfinal.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.foodappfinal.Model.WishlistItem;
import com.example.foodappproject.R;

import java.util.ArrayList;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.ViewHolder> {
    private ArrayList<WishlistItem> items;
    private OnItemClickListener clickListener;
    private OnRemoveClickListener removeListener;

    public interface OnItemClickListener {
        void onItemClick(WishlistItem item);
    }

    public interface OnRemoveClickListener {
        void onRemoveClick(WishlistItem item);
    }

    public WishlistAdapter(ArrayList<WishlistItem> items, OnItemClickListener clickListener, OnRemoveClickListener removeListener) {
        this.items = items;
        this.clickListener = clickListener;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_wishlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WishlistItem item = items.get(position);

        holder.title.setText(item.getProductName());
        holder.price.setText(String.format("$%.2f", item.getProductPrice()));

        Glide.with(holder.itemView.getContext())
                .load(item.getProductImageUrl())
                .placeholder(android.R.drawable.stat_sys_download)
                .error(android.R.drawable.stat_notify_error)
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(item));
        holder.removeButton.setOnClickListener(v -> removeListener.onRemoveClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, price;
        ImageButton removeButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.productImage);
            title = itemView.findViewById(R.id.productName);
            price = itemView.findViewById(R.id.productPrice);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }
}