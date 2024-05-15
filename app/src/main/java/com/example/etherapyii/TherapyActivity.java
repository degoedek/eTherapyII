package com.example.etherapyii;

import static java.lang.System.currentTimeMillis;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.atomic.AtomicLong;

public class TherapyActivity extends AppCompatActivity {

    public boolean isClockRunning = false;
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

        // Thread Creation
        Thread timeThread = new Thread() {
            public void run() {
                startClock();
            }
        };

        // Button Listeners
        beginButton.setOnClickListener(view -> {
            timeThread.start();
            stopButton.setVisibility(View.VISIBLE);
        });

        stopButton.setOnClickListener(view -> {
            timeThread.interrupt();
        });

    }

    private void startClock() {
        TextView timeTV = findViewById(R.id.timeTV);
        long startTime = currentTimeMillis();

        runOnUiThread(() -> {
            int minutes = 0;
            int seconds = 0;
            String time;

            while (!Thread.currentThread().isInterrupted()) {
                time = minutes + ":" + seconds;
                timeTV.setText(time);
                seconds = (int) ((currentTimeMillis() - startTime) / 1000);

                if (seconds >= 60) {
                    minutes++;
                    seconds = 0;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Handle interruption and terminate gracefully
                    break;
                }
            }
        });
    }
}