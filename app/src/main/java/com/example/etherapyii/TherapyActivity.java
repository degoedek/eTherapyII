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
import android.os.CountDownTimer;
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

import java.util.LinkedList;

import bolts.Continuation;
import bolts.Task;

public class TherapyActivity extends AppCompatActivity implements ServiceConnection {
    private BtleService.LocalBinder serviceBinder;
    private boolean isClockRunning = false, started = false, repStarted = false;
    private String time;
    private Handler handler;
    private long startTime;
    private MetaWearBoard board, board2;
    private CountDownTimer repCountdown;
    Quaternion s1CurrentQuat, s2CurrentQuat, s1Pose, s2Pose, RelativeRotationPose, RelativeRotationCurrent;
    Boolean posing = false, therapyActive = false;
    int timeLeft;
    DoublyLinkedList s1PoseList = new DoublyLinkedList();
    DoublyLinkedList s2PoseList = new DoublyLinkedList();
    final int RUNNING_AVG_SIZE = 5;
    final float ACCURACY_THRESHOLD = 10F;
    Quaternion[] s1RunningAverage = new Quaternion[RUNNING_AVG_SIZE];
    Quaternion[] s2RunningAverage = new Quaternion[RUNNING_AVG_SIZE];
    int s1Index = 0, s2Index = 0;
    float currentDistance;
    Thread S1PoseThread = new Thread(() -> sensorFusion(board, 1));
    Thread S2PoseThread = new Thread(() -> sensorFusion(board2, 2));
    String intent = "pose";
    TextView distanceTV, HoldTV;
    int HOLD_TIME;

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
        HOLD_TIME = intent.getExtras().getInt("HoldTime");

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
            if (!started) {
                started = true;
                startCountdown();
            }
        });

        stopButton.setOnClickListener(view -> {
            isClockRunning = false;
            therapyActive = false;

            // Adjusting Button Visibility
            stopButton.setVisibility(View.GONE);
        });
    }

    public void startCountdown() {
        long countdownTime = 3000;
        final long[] countdownDuration = {countdownTime};
        Button poseButton = findViewById(R.id.beginButton);

        CountDownTimer mCountDownTimer = new CountDownTimer(countdownDuration[0], 1000) {
            @Override
            public void onTick(long l) {
                countdownDuration[0] = l;
                timeLeft = ((int) countdownDuration[0] + 100) / 1000;


                String timeRemaining = "Pose\n" + timeLeft;
                poseButton.setText(timeRemaining);


                if (timeLeft == (countdownTime / 1000)) {
                    String tlString = "Time remaining: " + timeLeft + " - Start Sensor Fusion now";
                    Log.i("TherapyActivity", tlString);
                    getPose();
                } else if (timeLeft == (countdownTime / 1000) - 1) {
                    posing = true;
                }
            }

            @Override
            public void onFinish() {
                TextView timeTV = findViewById(R.id.timeTV);
                Button beginButton = findViewById(R.id.beginButton);
                Button stopButton = findViewById(R.id.btn_stop);

                // Stop Sensor Fusion
                posing = false;

                // Calculate Pose Averages
                s1Pose = s1PoseList.averageQuaternions();
                s1Pose = normalize(s1Pose);
                s2Pose = s2PoseList.averageQuaternions();
                s2Pose = normalize(s2Pose);
                RelativeRotationPose = findRelativeRotation(s1Pose, s2Pose);
                Log.i("TherapyActivity", "Relative Rotation Pose: " + RelativeRotationPose);


                // Start Clock
                isClockRunning = true;
                startTime = currentTimeMillis();
                startClock(timeTV);

                // Sensor Fusion
                therapyActive = true;
                intent = "therapy";

                // Adjusting Button Visibility
                beginButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
            }
        }.start();
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
                    distanceTV = findViewById(R.id.distanceTV);
                    if (RelativeRotationCurrent != null && RelativeRotationPose != null) {
                        distanceTV.setText(String.format("Current Angle:\n%.3f", currentDistance));
                    }

                    handler.postDelayed(this, 500); // Update every second
                }
            }
        });
    }

    /**
     * Determines the initial position of a sensor
     */
    public void getPose() {
        if (board != null && board2 != null) {
            S1PoseThread.start();
            S2PoseThread.start();
        } else {
            Log.wtf("Error", "Boards Not Connected - Sensor Fusion not executed - will crash at the end of the countdown");
        }
    }

    private void sensorFusion(MetaWearBoard board, int sensorNum) {
        HoldTV = findViewById(R.id.HoldTV);

        SensorFusionBosch sf = board.getModule(SensorFusionBosch.class);
        sf.resetOrientation();


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
                    switch (intent) {
                        case "pose":
                            if (posing) {
                                Log.i("TherapyActivity", "Pose Route Executing - sensorNum: " + sensorNum + " - data: " + data.value(Quaternion.class));
                                if (sensorNum == 1) {
                                    s1CurrentQuat = data.value(Quaternion.class);
                                    s1PoseList.insert(s1CurrentQuat);
                                } else {
                                    s2CurrentQuat = data.value(Quaternion.class);
                                    s2PoseList.insert(s2CurrentQuat);
                                }
                            }
                            break;
                        case "therapy":
                            if (therapyActive) {
//                                Log.i("TherapyActivity", "Therapy Route Executing");
                                if (sensorNum == 1) {
                                    Log.i("TherapyActivity", "Sensor 1: " + data.value(Quaternion.class));
                                    s1RunningAverage[s1Index] = data.value(Quaternion.class);
                                    s1Index = (s1Index + 1) % RUNNING_AVG_SIZE;
                                } else {
                                    Log.i("TherapyActivity", "Sensor 2: " + data.value(Quaternion.class));
                                    s2RunningAverage[s2Index] = data.value(Quaternion.class);
                                    s2Index = (s2Index + 1) % RUNNING_AVG_SIZE;
                                }

                                // Computing Running Averages
                                s1CurrentQuat = avgQuaternionArray(s1RunningAverage);
                                s2CurrentQuat = avgQuaternionArray(s2RunningAverage);

                                // Computing Relative Rotation and Distance
                                RelativeRotationCurrent = findRelativeRotation(normalize(s1CurrentQuat), normalize(s2CurrentQuat));
                                currentDistance = quaternionDistance(RelativeRotationPose, RelativeRotationCurrent);
                                Log.i("TherapyActivity", "Distance - " + currentDistance);

                                // Checking Rep Completion Status
                                // ChatGPT - this is where I want the timer implemented
                                if (currentDistance <= ACCURACY_THRESHOLD) {
                                    // TODO: Have a timer for this, add haptic feedback on completion, and update reps completed
                                    if (!repStarted) {
                                        repStarted = true;

                                        repCountdown = new CountDownTimer(HOLD_TIME, 1000) {

                                            @Override
                                            public void onTick(long l) {
                                                Log.i("TherapyActivity", "Hold time remaining: " + l / 1000 + " seconds");
                                                // Update the UI to show the remaining hold time
                                                runOnUiThread(() -> HoldTV.setText(String.format("Hold time remaining:\n%.1f", (float) l / 1000)));
                                            }

                                            @Override
                                            public void onFinish() {
                                                Log.i("TherapyActivity", "Hold time finished. Rep completed!");
                                                repStarted = false;
                                                // Add haptic feedback here if desired
                                                // Update the reps completed here
                                            }
                                        }.start();
                                    }
                                } else {
                                    if (repStarted) {
                                        repStarted = false;
                                        // TODO: Stop timer

                                    }
                                }
                            }
                            break;
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
        Log.d("TherapyActivity", "Service Connected");
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
        return (float) (Math.acos(2 * dotProduct * dotProduct - 1) * (180/ 3.14159));
    }

    public Quaternion avgQuaternionArray(Quaternion[] array) {
        float wSum = 0, xSum = 0, ySum = 0, zSum = 0;
        int valueCounter = 0;

        for (Quaternion quaternion : array) {
            if (quaternion != null) {
                wSum += quaternion.w();
                xSum += quaternion.x();
                ySum += quaternion.y();
                zSum += quaternion.z();
                valueCounter++;
            }
        }

        return new Quaternion(wSum / valueCounter, xSum / valueCounter, ySum / valueCounter, zSum / valueCounter);
    }

    public void stopSensorFusion(String intent) {
        switch (intent) {
            case "pose":
                S1PoseThread.interrupt();
                S2PoseThread.interrupt();
                break;
            default:
                if (board != null) {
                    Thread stopThread = new Thread(() -> {
                        final SensorFusionBosch sensorFusion = board.getModule(SensorFusionBosch.class);

                        Log.i("stopSensorFusion", "Sensor 1 should stop sensor fusion");
                        sensorFusion.stop();
                        sensorFusion.quaternion().stop();
                    });
                    stopThread.start();
                }
                if (board2 != null) {
                    Thread stopThread = new Thread(() -> {
                        final SensorFusionBosch sensorFusion2 = board2.getModule(SensorFusionBosch.class);

                        Log.i("stopSensorFusion", "Sensor 2 should stop sensor fusion");
                        sensorFusion2.quaternion().stop();
                        sensorFusion2.stop();
                    });
                    stopThread.start();
                }
        }
    }
}
