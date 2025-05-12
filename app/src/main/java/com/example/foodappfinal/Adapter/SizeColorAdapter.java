package com.example.foodappfinal.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.foodappproject.R;
import java.util.ArrayList;

public class SizeColorAdapter extends RecyclerView.Adapter<SizeColorAdapter.ViewHolder> {
    private ArrayList<String> items;
    private int selectedPosition = -1; // Track the selected item

    public SizeColorAdapter(ArrayList<String> items) {
        this.items = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_size_color, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String item = items.get(position);
        holder.textView.setText(item);

        // Update UI based on selection
        if (selectedPosition == position) {
            holder.textView.setBackgroundResource(R.drawable.size_selected_background);
            holder.textView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        } else {
            holder.textView.setBackgroundResource(R.drawable.size_default_background);
            holder.textView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
        }

        // Handle click event
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousPosition); // Update previous item
            notifyItemChanged(selectedPosition); // Update current item
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.sizeColorText);
        }
    }

    // Method to get the selected size
    public String getSelectedSize() {
        if (selectedPosition != -1) {
            return items.get(selectedPosition);
        }
        return null;
    }
}