package com.example.etherapyii;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Objects;

public class TherapyDescription extends AppCompatActivity {
    final String HOTT = "Head Orientation Therapy Tool";
    final String HOTT_description = "The user will move their hand to a desired location, which will become the target location on the screen. After the hand/sensor is in the desired spot, there will be a green indicator for the sensor on the users head, to which the user will rotate to fit inside of the target. Once the two circles coincide for a short period of time a repetition will be counted, and the sensor on the users hand will vibrate.";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_therapy_description);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Variable Declaration
        Button back = findViewById(R.id.back);
        Button startActivity = findViewById(R.id.start_activity);
        TextView titleTV = findViewById(R.id.description_title);
        TextView descriptionTV = findViewById(R.id.description_body);
        String therapy;

        // Getting Metric From Therapy Selection
        Intent intent = getIntent();
        therapy = intent.getExtras().getString("Therapy");

        // Setting Images on Therapy
        switch (Objects.requireNonNull(therapy)) {
            case "Hott":
                titleTV.setText(HOTT);
                descriptionTV.setText(HOTT_description);
                break;
        }

        back.setOnClickListener(view -> {
            Intent intentNav = new Intent(TherapyDescription.this, TherapySelection.class);
            startActivity(intentNav);
        });
        startActivity.setOnClickListener(view -> {
            Intent intentNav = new Intent(TherapyDescription.this, SensorPlacement.class);
            intentNav.putExtra("Therapy", therapy);
            startActivity(intentNav);
        });
    }
}