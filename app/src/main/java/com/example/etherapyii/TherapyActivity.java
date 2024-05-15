package com.example.etherapyii;

import static java.lang.System.currentTimeMillis;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TherapyActivity extends AppCompatActivity {

    private boolean isClockRunning = false;
    private String time;
    private Handler handler;
    private long startTime;

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
        Button beginButton = findViewById(R.id.beginButton);
        Button stopButton = findViewById(R.id.btn_stop);
        String therapyType;
        int reps, repsCompleted = 0;
        String repsText;

        // Get Intent
        Intent intent = getIntent();
        therapyType = intent.getExtras().getString("Therapy");
        reps = intent.getExtras().getInt("Reps");

        // Set Title
        switch (therapyType) {
            case "Hott":
                titleTV.setText("Head Orientation\nTherapy Tool");
                break;
            default:
                titleTV.setText("Placeholder: Add title");
        }

        // Set Reps
        repsText = repsCompleted + "/" + reps;
        repsTV.setText(repsText);

        handler = new Handler(Looper.getMainLooper());

        // Button Listeners
        beginButton.setOnClickListener(view -> {
            isClockRunning = true;
            startTime = currentTimeMillis();
            startClock(timeTV);
            stopButton.setVisibility(View.VISIBLE);
        });

        stopButton.setOnClickListener(view -> {
            isClockRunning = false;
            stopButton.setVisibility(View.GONE);
        });
    }

    private void startClock(TextView timeTV) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isClockRunning) {
                    long elapsedMillis = currentTimeMillis() - startTime;
                    int seconds = (int) (elapsedMillis / 1000) % 60;
                    int minutes = (int) ((elapsedMillis / (1000 * 60)) % 60);

                    time = String.format("%d:%02d", minutes, seconds);
                    timeTV.setText(time);

                    handler.postDelayed(this, 1000); // Update every second
                }
            }
        });
    }
}
