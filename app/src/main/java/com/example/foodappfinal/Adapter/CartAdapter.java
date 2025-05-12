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
import com.example.foodappfinal.Model.CartItem;
import com.example.foodappproject.R;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
    private List<CartItem> cartItems;
    private Context context;
    private OnCartItemChangeListener listener;

    public interface OnCartItemChangeListener {
        void onQuantityChanged(String cartItemId, int newQuantity);
        void onItemRemoved(String cartItemId);
    }

    public CartAdapter(Context context, List<CartItem> cartItems, OnCartItemChangeListener listener) {
        this.context = context;
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);

        holder.title.setText(cartItem.getProductName());
        holder.price.setText(String.format("%.2f₫", cartItem.getPrice()));
        holder.quantity.setText(String.valueOf(cartItem.getQuantity()));

        String imageUrl = "https://fmheidqpephjpsgjuupf.supabase.co/storage/v1/object/public/item/item_" + cartItem.getProductId() + "/" + cartItem.getProductId() + "_1.png";
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(holder.image);

        holder.increaseButton.setOnClickListener(v -> {
            int newQuantity = cartItem.getQuantity() + 1;
            if (newQuantity <= cartItem.getStock()) {
                listener.onQuantityChanged(cartItem.getCartItemId(), newQuantity);
            }
        });

        holder.decreaseButton.setOnClickListener(v -> {
            int newQuantity = cartItem.getQuantity() - 1;
            if (newQuantity >= 1) {
                listener.onQuantityChanged(cartItem.getCartItemId(), newQuantity);
            }
        });

        holder.removeButton.setOnClickListener(v -> listener.onItemRemoved(cartItem.getCartItemId()));
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image, increaseButton, decreaseButton, removeButton;
        TextView title, price, quantity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
            price = itemView.findViewById(R.id.price);
            quantity = itemView.findViewById(R.id.quantity);
            increaseButton = itemView.findViewById(R.id.increaseButton);
            decreaseButton = itemView.findViewById(R.id.decreaseButton);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }
}