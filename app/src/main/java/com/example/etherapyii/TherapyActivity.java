package com.example.etherapyii;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TherapyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_therapy);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Variable Declaration
        TextView titleTV = findViewById(R.id.titleTV);
        TextView repsTV = findViewById(R.id.repsTV);
        TextView timeTV = findViewById(R.id.timeTV);
        String therapyType;
        int reps, repsCompleted = 0;
        String repsText;

        // Get Intent
        Intent intent = getIntent();
        therapyType = intent.getExtras().getString("Therapy");
        reps = intent.getExtras().getInt("Reps"); //TODO ONCE SLIDER IS FIXED


        // Set Title
        switch (therapyType) {
            case "Hott":
                titleTV.setText("Head Orientation\nTherapy Tool");
                break;
            default:
                titleTV.setText("Placeholder");
        }

        // Set Reps
        repsText = repsCompleted + "/" + reps;
        repsTV.setText(repsText);


    }
}