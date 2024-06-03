package com.example.etherapyii;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Objects;

public class TherapyDescriptionFragment extends Fragment {
    final String HOTT = "Head Orientation Therapy Tool";
    final String HOTT_description = "The user will move their hand to a desired location, which will become the target location on the screen. After the hand/sensor is in the desired spot, there will be a green indicator for the sensor on the users head, to which the user will rotate to fit inside of the target. Once the two circles coincide for a short period of time a repetition will be counted, and the sensor on the users hand will vibrate.";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_therapy_description, container, false);


        // Variable Declaration
        Button back = view.findViewById(R.id.back);
        Button startActivity = view.findViewById(R.id.start_activity);
        TextView titleTV = view.findViewById(R.id.description_title);
        TextView descriptionTV = view.findViewById(R.id.description_body);
        String therapy;

        // Getting Metric From Therapy Selection
        assert getArguments() != null;
        therapy = getArguments().getString("therapy");

        // Setting Images on Therapy
        switch (Objects.requireNonNull(therapy)) {
            case "HOTT":
                titleTV.setText(HOTT);
                descriptionTV.setText(HOTT_description);
                break;
        }

        back.setOnClickListener(view2 -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.therapyContainer, new TherapySelectionFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        startActivity.setOnClickListener(view2 -> {
            Bundle bundle = new Bundle();
            bundle.putString("therapy", "HOTT");

            ConnectionFragment connectionFragment = new ConnectionFragment();
            connectionFragment.setArguments(bundle);

            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.therapyContainer, connectionFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Inflate the layout for this fragment
        return view;
    }
}