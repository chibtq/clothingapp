package com.example.foodappfinal.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import com.example.foodappproject.R;

public class SoldFragment extends Fragment {
    private static final String ARG_STOCK = "stock";
    private static final String ARG_SOLD = "sold";
    private int stock;
    private int sold;

    public SoldFragment() {
    }

    public static SoldFragment newInstance(int stock, int sold) {
        SoldFragment fragment = new SoldFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STOCK, stock);
        args.putInt(ARG_SOLD, sold);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            stock = getArguments().getInt(ARG_STOCK);
            sold = getArguments().getInt(ARG_SOLD);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sold, container, false);
        TextView productStock = view.findViewById(R.id.productStock);
        TextView productSold = view.findViewById(R.id.productSold);
        productStock.setText(String.format("In Stock: %d", stock));
        productSold.setText(String.format("Sold: %d", sold));
        return view;
    }
}