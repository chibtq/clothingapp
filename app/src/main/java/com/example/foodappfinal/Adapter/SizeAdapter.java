package com.example.foodappfinal.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.foodappproject.R;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SizeAdapter extends RecyclerView.Adapter<SizeAdapter.ViewHolder> {
    private ArrayList<String> allSizes; // Tất cả size có thể có (S, M, L, XL, XXL)
    private Set<String> availableSizes; // Các size khả dụng (từ dữ liệu sản phẩm)
    private int selectedPosition = -1; // Vị trí của size được chọn, -1 nghĩa là chưa chọn

    public SizeAdapter(ArrayList<String> allSizes, ArrayList<String> availableSizes) {
        this.allSizes = allSizes;
        this.availableSizes = new HashSet<>(availableSizes);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_size, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String size = allSizes.get(position);
        holder.sizeText.setText(size);

        // Kiểm tra size có khả dụng không
        boolean isAvailable = availableSizes.contains(size);
        holder.sizeText.setEnabled(isAvailable);

        // Cập nhật màu nền và màu chữ dựa trên trạng thái
        if (position == selectedPosition && isAvailable) {
            holder.sizeText.setBackgroundResource(R.drawable.size_selected_background);
        } else {
            holder.sizeText.setBackgroundResource(R.drawable.size_background);
        }
        holder.sizeText.setTextColor(Color.parseColor("#212121")); // Màu chữ luôn là đen

        // Xử lý sự kiện click (chỉ khi size khả dụng)
        if (isAvailable) {
            holder.sizeText.setOnClickListener(v -> {
                int previousPosition = selectedPosition;
                selectedPosition = position;

                // Cập nhật lại giao diện cho ô trước đó (nếu có) và ô vừa chọn
                if (previousPosition != -1) {
                    notifyItemChanged(previousPosition);
                }
                notifyItemChanged(selectedPosition);
            });
        } else {
            holder.sizeText.setOnClickListener(null); // Không cho phép click nếu size không khả dụng
        }
    }

    @Override
    public int getItemCount() {
        return allSizes.size();
    }

    // Phương thức để lấy size được chọn
    public String getSelectedSize() {
        if (selectedPosition != -1) {
            return allSizes.get(selectedPosition);
        }
        return null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView sizeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            sizeText = itemView.findViewById(R.id.sizeText);
        }
    }
}