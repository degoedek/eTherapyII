package com.example.etherapyii;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble;

public class TherapyContainer extends AppCompatActivity {
    private SharedViewModel viewModel;
    Bwt901ble sensor1;
    Bwt901ble sensor2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_therapy_container);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        // Set some initial data
        viewModel.setSensor1(sensor1);
        viewModel.setSensor2(sensor2);

        // Set initial fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.therapyContainer, new TherapySelectionFragment())
                    .commit();
        }



    }
}