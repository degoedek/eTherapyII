package com.example.etherapyii;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.Objects;

public class SummaryFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_summary, container, false);
        Button exitButton = view.findViewById(R.id.exitApp);

        exitButton.setOnClickListener(v -> {
            requireActivity().finishAffinity();
        });

        // Inflate the layout for this fragment
        return view;
    }
}