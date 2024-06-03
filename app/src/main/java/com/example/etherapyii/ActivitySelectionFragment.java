package com.example.etherapyii;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;


public class ActivitySelectionFragment extends Fragment {
Button therapy, stretching;
    /*public ActivitySelectionFragment() {
        // Required empty public constructor
    }*/


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_activity_selection, container, false);
        therapy = view.findViewById(R.id.btn_therapy);
        stretching = view.findViewById(R.id.btn_stretching);

        //Therapy Button
        therapy.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), TherapyContainer.class);
                startActivity(intent);
            //getActivity().finish();
        });

        // Stretch Button
        stretching.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), WIPActivity.class);
            startActivity(intent);
        });


        return view;
    }
}