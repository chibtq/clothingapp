package com.example.foodappfinal.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.foodappfinal.Model.Address;
import com.example.foodappproject.R;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {
    private Context context;
    private List<Address> addressList;
    private OnAddressActionListener listener;

    public interface OnAddressActionListener {
        void onSetDefault(Address address);
        void onDelete(Address address);
    }

    public AddressAdapter(Context context, List<Address> addressList, OnAddressActionListener listener) {
        this.context = context;
        this.addressList = addressList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.viewholder_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        Address address = addressList.get(position);
        holder.addressText.setText(address.getFullAddress());
        holder.defaultCheck.setChecked(address.isDefault());
        holder.defaultCheck.setEnabled(!address.isDefault()); // Disable if already default
        holder.defaultCheck.setOnClickListener(v -> {
            if (!address.isDefault()) {
                listener.onSetDefault(address);
            }
        });
        holder.deleteButton.setOnClickListener(v -> listener.onDelete(address));
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    static class AddressViewHolder extends RecyclerView.ViewHolder {
        TextView addressText;
        CheckBox defaultCheck;
        ImageButton deleteButton; // Changed from Button to ImageButton

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            addressText = itemView.findViewById(R.id.addressText);
            defaultCheck = itemView.findViewById(R.id.defaultCheck);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}