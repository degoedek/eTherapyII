package com.example.etherapyii;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
Button logout, goniometer;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_home, container, false);
//        logout = v.findViewById(R.id.btn_logout);
        goniometer= v.findViewById(R.id.btn_home_goniometer);
        SharedPreferences preferences = this.getActivity().getSharedPreferences("checkbox",Context.MODE_PRIVATE);
        logout.setText("Logout " + preferences.getString("firstName", ""));
//        logout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                SharedPreferences.Editor editor = preferences.edit();
//                editor.putString("remember","false");
//                editor.apply();
//                getActivity().finish();
//            }
//        });

//        goniometer.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //TODO: Check if the patient (if this is a patient) has assigned exercises
//                if(!(preferences.getBoolean("Is Therapist",true))) {
//                    int id = preferences.getInt("User ID", -1);
//                    PatientDatabase patientDb = PatientDatabase.getInstance(getActivity());
//                    Patient patient = patientDb.patientDao().findUserWithID(id).get(0);
//                    if (patient.getPatientData().split(",").length < 3) { //Indicates no assignments
//                        Toast.makeText(getActivity(),"No Assigned Exercises" ,Toast.LENGTH_SHORT).show();
//                    }
//                    else{
//                        Intent intent = new Intent(getActivity(), ConnectionActivity.class);
//                        startActivity(intent);
//                    }
//                }
//                else {
//                    Intent intent = new Intent(getActivity(), ConnectionActivity.class);
//                    startActivity(intent);
//                }
//                //getActivity().finish();
//            }
//        });

        return v;
    }
}