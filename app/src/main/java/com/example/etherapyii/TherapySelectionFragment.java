package com.example.etherapyii;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class TherapySelectionFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_therapy_selection, container, false);

        Button Hott = view.findViewById(R.id.HOTT);

        Hott.setOnClickListener(v -> {
//            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
//            transaction.replace(R.id.fragment_container, new FragmentTwo());
//            transaction.addToBackStack(null);
//            transaction.commit();
        });

        return view;
    }
}