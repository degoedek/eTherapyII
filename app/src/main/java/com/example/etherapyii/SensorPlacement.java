package com.example.etherapyii;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Objects;

public class SensorPlacement extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sensor_placement);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Variable Declarations
        String therapy;
        ImageView hand_image;
        ImageView human_image;
        Button start_activity;

        // Getting Metric From Therapy Selection
        Intent intent = getIntent();
        therapy = intent.getExtras().getString("Therapy");

        // Setting Images on Therapy
        switch (Objects.requireNonNull(therapy)) {
            case "Hott":
                Log.i("Hott", "Switch enterred - case 'Hott'");
                hand_image = findViewById(R.id.hott_hand);
                human_image = findViewById(R.id.hott_human);
                hand_image.setImageResource(R.drawable.hott_hand);
                human_image.setImageResource(R.drawable.hott_human);
                break;
        }

        // Next Activity Navigation
        start_activity = findViewById(R.id.start_activity);
        start_activity.setOnClickListener(v -> {
            Intent intent2 = new Intent(SensorPlacement.this, TherapyActivity.class);
            intent2.putExtra("Therapy" , therapy);
            startActivity(intent2);
        });

//        SeekBar seekBar = findViewById(R.id.seekBar);
//        TextView valueLabel = findViewById(R.id.valueLabel);




    }
}