package com.example.etherapyii;

import static java.lang.System.currentTimeMillis;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Quaternion;
import com.mbientlab.metawear.module.SensorFusionBosch;
import com.mbientlab.metawear.module.SensorFusionBosch.*;

import bolts.Continuation;
import bolts.Task;

public class TherapyActivity extends AppCompatActivity implements ServiceConnection {
    private BtleService.LocalBinder serviceBinder;
    private boolean isClockRunning = false;
    private String time;
    private Handler handler;
    private long startTime;
    private MetaWearBoard board, board2;
    Quaternion s1CurrentQuat, s2CurrentQuat, s1Pose, s2Pose, RelativeRotationPose, RelativeRotationCurrent;
    Boolean s1QuatSet = false, s2QuatSet = false;

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

        // Set Reps/Initial time
        repsText = repsCompleted + "/" + reps;
        repsTV.setText(repsText);
        timeTV.setText("0:00");

        // Create Bluetooth Service Binding
        getApplicationContext().bindService(new Intent(this, BtleService.class), this, Context.BIND_AUTO_CREATE);

        handler = new Handler(Looper.getMainLooper());

        // Button Listeners
        beginButton.setOnClickListener(view -> {
            // Start Clock
            isClockRunning = true;
            startTime = currentTimeMillis();
            startClock(timeTV);

            // Sensor Fusion
            sensorFusion(board, 1);
            sensorFusion(board2, 2);

            // Adjusting Button Visibility
            beginButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.VISIBLE);
        });

        stopButton.setOnClickListener(view -> {
            isClockRunning = false;

            // Adjusting Button Visibility
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

    private void sensorFusion(MetaWearBoard board, int sensorNum) {
        SensorFusionBosch sf = board.getModule(SensorFusionBosch.class);


        // use ndof mode with +/-16g acc range and 2000dps gyro range
        sf.configure()
                .mode(SensorFusionBosch.Mode.NDOF)
                .accRange(SensorFusionBosch.AccRange.AR_16G)
                .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                .commit();

        // stream quaternion values from the board
        sf.quaternion().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
//            Log.i("MainActivity", "Board: " + sensorNum + " - Quaternion = " + data.value(Quaternion.class));
            // Assigning quaternion values to respective variables based on sensor
            switch (sensorNum) {
                case 1:
                    s1CurrentQuat = data.value(Quaternion.class);
                    if (!s1QuatSet) {
                        s1Pose = s1CurrentQuat;
                        Log.i("MainActivity", "S1 Pose - " + s1Pose);

                    }
                    s1QuatSet = true;
                    break;
                case 2:
                    s2CurrentQuat = data.value(Quaternion.class);
                    if (!s2QuatSet) {
                        s2Pose = s2CurrentQuat;
                        Log.i("MainActivity", "S2 Pose - " + s2Pose);
                    }
                    s2QuatSet = true;
                    break;
            }

            if (s1QuatSet && s2QuatSet) {
                RelativeRotationPose = findRelativeRotation(normalize(s1Pose), normalize(s2Pose));
//                Log.i("TherapyActivity", "Relative Rotation - " + RelativeRotationPose);
                RelativeRotationCurrent = findRelativeRotation(normalize(s1CurrentQuat), normalize(s2CurrentQuat));
//                Log.i("TherapyActivity", "Relative Rotation Current - " + RelativeRotationCurrent);
                Log.i("TherapyActivity", "Distance - " + quaternionDistance(RelativeRotationPose, RelativeRotationCurrent));

            }


        }))
        .continueWith((Continuation<Route, Void>) task -> {
            sf.resetOrientation();
            sf.quaternion().start();
            sf.start();
            return null;
        });

    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Log.d("TAMeasurement", "Service Connected");
        //Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;
        retrieveBoard();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    /**
     * Uses the MAC addresses of the sensors to make sure that they are connected
     */
    public void retrieveBoard() {
        final BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //Has the haptic coin
        String macAddress1 = "ED:5B:0A:50:14:59";
        BluetoothDevice sensor = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress1);
        String macAddress2 = "FE:C2:4B:10:FB:D5";
        BluetoothDevice sensor2 = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress2);


        // Create a MetaWear board object for the Bluetooth Device
        board = serviceBinder.getMetaWearBoard(sensor);
        board2 = serviceBinder.getMetaWearBoard(sensor2);
    }

    public Quaternion multiplyQuat(Quaternion q1, Quaternion q2) {
        float w3, w2, w1, x3, x2, x1, y3, y2, y1, z3, z2, z1;
        w1 = q1.w();
        x1 = q1.x();
        y1 = q1.y();
        z1 = q1.z();

        w2 = q2.w();
        x2 = q2.x();
        y2 = q2.y();
        z2 = q2.z();

        // TODO: Check this
        w3 = w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2;
        x3 = w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2;
        y3 = w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2;
        z3 = w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2;

        /* From ChatGPT
        public Quaternion multiply(Quaternion q) {
            return new Quaternion(
                w * q.w - x * q.x - y * q.y - z * q.z,
                w * q.x + x * q.w + y * q.z - z * q.y,
                w * q.y - x * q.z + y * q.w + z * q.x,
                w * q.z + x * q.y - y * q.x + z * q.w
            );
        }
         */

        return new Quaternion(w3, x3, y3, z3);
    }

    public Quaternion conjugateQuat(Quaternion q) {
        return new Quaternion(q.w(), -q.x(), -q.y(), -q.z());
    }

    public Quaternion findRelativeRotation(Quaternion q1, Quaternion q2) {
        Quaternion q1Conjugate = conjugateQuat(q1);
        return multiplyQuat(q1Conjugate, q2);
    }

    public Quaternion normalize(Quaternion q) {
        float norm = (float) Math.sqrt(q.w() * q.w() + q.x() * q.x() + q.y() * q.y() + q.z() * q.z());
        if (norm == 0) {
            return new Quaternion(0, 0, 0, 0);
        }
        return new Quaternion(q.w() / norm, q.x() / norm, q.y() / norm, q.z() / norm);
    }

    public float quaternionDistance(Quaternion q1, Quaternion q2) {
        float dotProduct = q1.w() * q2.w() + q1.x() * q2.x() + q1.y() * q2.y() + q1.z() * q2.z();
        return (float) Math.acos(2 * dotProduct * dotProduct - 1);
    }


}
