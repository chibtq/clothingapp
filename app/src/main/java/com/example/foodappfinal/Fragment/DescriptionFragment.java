package com.example.foodappfinal.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.foodappproject.R;

public class DescriptionFragment extends Fragment {
    private static final String ARG_DESCRIPTION = "description";
    private String description;

    public DescriptionFragment() {
    }

    public static DescriptionFragment newInstance(String description) {
        DescriptionFragment fragment = new DescriptionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DESCRIPTION, description);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            description = getArguments().getString(ARG_DESCRIPTION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_description, container, false);
        TextView descriptionTextView = view.findViewById(R.id.descriptionTextView);
        descriptionTextView.setText(description);
        return view;
    }
}