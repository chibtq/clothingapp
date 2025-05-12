package com.example.foodappfinal.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.foodappfinal.Domain.CategoryDomain;
import com.example.foodappproject.R;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.function.Consumer;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private ArrayList<CategoryDomain> items;
    private Consumer<String> onCategoryClick;

    public CategoryAdapter(ArrayList<CategoryDomain> items, Consumer<String> onCategoryClick) {
        this.items = items;
        this.onCategoryClick = onCategoryClick;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CategoryDomain category = items.get(position);
        holder.categoryName.setText(category.getTitle());
        Picasso.get().load(category.getPicUrl()).into(holder.categoryImage);

        holder.itemView.setOnClickListener(v -> {
            if (onCategoryClick != null) {
                String categoryId = category.getCategoryId();
                onCategoryClick.accept(categoryId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryImage;
        TextView categoryName;

        public ViewHolder(View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.categoryImage);
            categoryName = itemView.findViewById(R.id.categoryName);
        }
    }
}