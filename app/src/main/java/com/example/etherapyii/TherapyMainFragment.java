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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

//import com.mbientlab.metawear.MetaWearBoard;
//import com.mbientlab.metawear.Route;
//import com.mbientlab.metawear.Subscriber;
//import com.mbientlab.metawear.android.BtleService;
//import com.mbientlab.metawear.module.Led;
//import com.mbientlab.metawear.module.SensorFusionBosch;

import com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble;
import com.wit.witsdk.modular.sensor.example.ble5.interfaces.IBwt901bleRecordObserver;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothSPP;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.WitBluetoothManager;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.exceptions.BluetoothBLEException;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.interfaces.IBluetoothFoundObserver;
import com.wit.witsdk.modular.sensor.device.exceptions.OpenDeviceException;
import com.wit.witsdk.modular.sensor.modular.processor.constant.WitSensorKey;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bolts.Continuation;


public class TherapyMainFragment extends Fragment {
    //private BtleService.LocalBinder serviceBinder;
    private boolean isClockRunning = false, started = false, repStarted = false;
    SharedViewModel viewModel;
    private String time;
    private Handler handler;
    private long startTime;
    //private MetaWearBoard board, board2;
    private CountDownTimer repCountdown;
    private Quaternion s1CurrentQuat, s2CurrentQuat, s1Pose, s2Pose, RelativeRotationPose, RelativeRotationCurrent;
    private Boolean posing = false, therapyActive = false;
    private int timeLeft;
    private DoublyLinkedList s1PoseList = new DoublyLinkedList();
    private DoublyLinkedList s2PoseList = new DoublyLinkedList();
    private final int RUNNING_AVG_SIZE = 5;
    private final float ACCURACY_THRESHOLD = 10F;
    private Quaternion[] s1RunningAverage = new Quaternion[RUNNING_AVG_SIZE];
    private Quaternion[] s2RunningAverage = new Quaternion[RUNNING_AVG_SIZE];
    private int s1Index = 0, s2Index = 0;
    private float currentDistance;
    private Thread S1PoseThread = new Thread(() -> sensorFusion(1));
    private Thread S2PoseThread = new Thread(() -> sensorFusion(2));
    private String intent = "pose";
    private TextView distanceTV, HoldTV, dataDisplay;
    private ImageView circleUserWithNotch, circleGoalWithNotch;
    private int HOLD_TIME;
    private View view;
    private Handler uiHandler = new Handler();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private double[] s1PoseAngles = new double[3];
    private double[] s2PoseAngles = new double[3];
    private double[] s1Angles = new double[3];
    private double[] s2Angles = new double[3];
    private float initialX, initialY;
    //private Route s1Route, s2Route;
    private Bwt901ble sensor1, sensor2;
    private boolean destroyed = true;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_therapy_main, container, false);

        viewModel.getSensor1().observe(getViewLifecycleOwner(), new Observer<Bwt901ble>(){
            public void onChanged(@Nullable final Bwt901ble newSensor) {
                // Update the UI with the new sensor data
                sensor1 = newSensor;
                // Example: Log the sensor data
                Log.d("SensorFragment", "Sensor1 received: " + sensor1);
            }
        });

        viewModel.getSensor2().observe(getViewLifecycleOwner(), new Observer<Bwt901ble>(){
            // Update the UI with the new sensor data
            public void onChanged(@Nullable final Bwt901ble newSensor) {
                // Update the UI with the new sensor data
                sensor2 = newSensor;
                // Example: Log the sensor data
                Log.d("SensorFragment", "Sensor1 received: " + sensor2);
            }
        });

        // Variable Declaration
        TextView titleTV = view.findViewById(R.id.titleTV);
        TextView repsTV = view.findViewById(R.id.repsTV);
        TextView timeTV = view.findViewById(R.id.timeTV);
        Button beginButton = view.findViewById(R.id.beginButton);
        Button stopButton = view.findViewById(R.id.btn_stop);
        dataDisplay = view.findViewById(R.id.dataDisplay);
        circleUserWithNotch = view.findViewById(R.id.circle_user_with_notch);
        circleGoalWithNotch = view.findViewById(R.id.circle_goal_with_notch);
        int[] userCoordinates = new int[2];
        String therapyType;
        int reps, repsCompleted = 0;
        String repsText;

        circleGoalWithNotch.post(() -> {
            Log.i("TherapyMainFragment", "circleUserWithNotch (X, Y): X - " + circleUserWithNotch.getX() + " Y - " + circleUserWithNotch.getY());
            initialX = circleGoalWithNotch.getX() + circleGoalWithNotch.getWidth() / 2 - circleUserWithNotch.getWidth() / 2;
            initialY = circleGoalWithNotch.getY() + circleGoalWithNotch.getHeight() / 2 - circleUserWithNotch.getHeight() / 2;
            circleUserWithNotch.setX(initialX);
            circleUserWithNotch.setY(initialY);
            circleUserWithNotch.getLocationOnScreen(userCoordinates);
            Log.i("TherapyMainFragment", "userCoordinates (X, Y): X - " + userCoordinates[0] + " Y - " + userCoordinates[1]);
        });

        // Get Intent
        // Getting Metric From Connection Fragment
        assert getArguments() != null;
        therapyType = getArguments().getString("therapy");
        reps = getArguments().getInt("reps");
        HOLD_TIME = getArguments().getInt("holdTime");
        Log.i("TherapyMainFragment", "therapyType: " + therapyType + " reps: " + reps + " holdTime: " + HOLD_TIME);

        // Set Title
        switch (Objects.requireNonNull(therapyType)) {
            case "HOTT":
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
      //  requireActivity().getApplicationContext().bindService(new Intent(getActivity(), BtleService.class), this, Context.BIND_AUTO_CREATE);

        handler = new Handler(Looper.getMainLooper());

        // Button Listeners
        beginButton.setOnClickListener(view2 -> {
            if (!started) {


                started = true;
                startCountdown();
            }
        });

        stopButton.setOnClickListener(view2 -> {
            isClockRunning = false;
            therapyActive = false;
            destroyed = true;
            S1PoseThread.interrupt();
            S2PoseThread.interrupt();


            Bundle bundle = new Bundle();
            // TODO: Add bundle extras here when needed

            // Create the new fragment and set the bundle as its arguments
            SummaryFragment summaryFragment = new SummaryFragment();
            summaryFragment.setArguments(bundle);

            // Replace the current fragment with the new one
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.therapyContainer, summaryFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Allows sensor fusion to run
        destroyed = false;

        // Inflate the layout for this fragment
        return view;
    }

    private void refreshDataTh() {

        while (!destroyed) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            StringBuilder text = new StringBuilder();
//            for (int i = 0; i < bwt901bleList.size(); i++) {
//                // 让所有设备进行加计校准
//                // Make all devices accelerometer calibrated
//                Bwt901ble bwt901ble = bwt901bleList.get(i);
//                String deviceData = getDeviceData(bwt901ble);
//                text.append(deviceData);
//            }

        }
    }



    // Pose Countdown
    public void startCountdown() {
        long countdownTime = 6000; // This is the duration of pose
        final long[] countdownDuration = {countdownTime};
        Button poseButton = view.findViewById(R.id.beginButton);

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
                } else if (timeLeft == (countdownTime / 1000) - 3) {
                    posing = true;
                }
            }

            @Override
            public void onFinish() {
                TextView timeTV = view.findViewById(R.id.timeTV);
                Button beginButton = view.findViewById(R.id.beginButton);
                Button stopButton = view.findViewById(R.id.btn_stop);

                // Stop Pose
                posing = false;

                // Calculate Pose Averages
                s1Pose = s1PoseList.averageQuaternions();
                s1Pose = normalize(s1Pose);
                s2Pose = s2PoseList.averageQuaternions();
                s2Pose = normalize(s2Pose);

                s1PoseAngles = quaternionToEulerAngles(s1Pose, "zyx");
                s2PoseAngles = quaternionToEulerAngles(s2Pose, "xyz");

                Log.i("TherapyActivity", "S1 Pose Angles - X = " + s1PoseAngles[0] + " Y = " + s1PoseAngles[1] + " Z = " + s1PoseAngles[2]);
                Log.i("TherapyActivity", "S2 Pose Angles - Z = " + s2PoseAngles[0] + " Y = " + s2PoseAngles[1] + " X = " + s2PoseAngles[2]);


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
                    distanceTV = view.findViewById(R.id.distanceTV);
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
        if (sensor1 != null && sensor2 != null) {
            S1PoseThread.start();
            S2PoseThread.start();
        } else {
            Log.wtf("Error", "Boards Not Connected - Sensor Fusion not executed - will crash at the end of the countdown");
        }
    }


    private void updateCirclePosition(double[] s1Angles, double[] s2Angles) {
        executorService.execute(() -> {
            // Calculate the position difference based on angles
            double dx = (s2Angles[1] - s1Angles[1]) * 100; // Pitch difference
            double dy = (s2Angles[0] - s1Angles[0]) * 100; // Yaw difference

            uiHandler.post(() -> {
                // Update UI with new position
                double newX = initialX + dx - (double) circleUserWithNotch.getWidth() / 2;
                double newY = initialY + dy - (double) circleUserWithNotch.getHeight() / 2;

                // Ensure the circles stay within the screen bounds
                newX = Math.max(0, Math.min(newX, view.getWidth() - circleUserWithNotch.getWidth()));
                newY = Math.max(0, Math.min(newY, view.getHeight() - circleUserWithNotch.getHeight()));

                circleUserWithNotch.setX((float) newX);
                circleUserWithNotch.setY((float) newY);
            });
        });
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

        // Preventing a divide by zero error
        if (dotProduct > .999 && dotProduct < 1.001) return 0;

        return (float) (Math.acos(2 * dotProduct * dotProduct - 1) * (180 / 3.14159));
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


    public double[] threeAxisRotation(double r11, double r12, double r21, double r31, double r32) {
        double[] angles = new double[3];

        angles[0] = Math.atan2(r31, r32) * (180 / Math.PI);
        angles[1] = Math.asin(r21) * (180 / Math.PI);
        angles[2] = Math.atan2(r11, r12) * (180 / Math.PI);

        return angles;
    }

    public double[] quaternionToEulerAngles(Quaternion q, String seq) {
        if (q == null){
            return null;
        }
        
        double[] rotations = new double[3];

        switch (seq) {
            case "zyx":
                rotations = threeAxisRotation(2 * (q.x() * q.y() + q.w() * q.z()),
                        q.w() * q.w() + q.x() * q.x() - q.y() * q.y() - q.z() * q.z(),
                        -2 * (q.x() * q.z() - q.w() * q.y()),
                        2 * (q.y() * q.z() + q.w() * q.x()),
                        q.w() * q.w() - q.x() * q.x() - q.y() * q.y() + q.z() * q.z());
                break;

            case "zxy":
                rotations = threeAxisRotation(-2 * (q.x() * q.y() - q.w() * q.z()),
                        q.w() * q.w() - q.x() * q.x() + q.y() * q.y() - q.z() * q.z(),
                        2 * (q.y() * q.z() + q.w() * q.x()),
                        -2 * (q.x() * q.z() - q.w() * q.y()),
                        q.w() * q.w() - q.x() * q.x() - q.y() * q.y() + q.z() * q.z());
                break;

            case "yxz":
                rotations = threeAxisRotation(2 * (q.x() * q.z() + q.w() * q.y()),
                        q.w() * q.w() - q.x() * q.x() - q.y() * q.y() + q.z() * q.z(),
                        -2 * (q.y() * q.z() - q.w() * q.x()),
                        2 * (q.x() * q.y() + q.w() * q.z()),
                        q.w() * q.w() - q.x() * q.x() + q.y() * q.y() - q.z() * q.z());
                break;

            case "yzx":
                rotations = threeAxisRotation(-2 * (q.x() * q.z() - q.w() * q.y()),
                        q.w() * q.w() + q.x() * q.x() - q.y() * q.y() - q.z() * q.z(),
                        2 * (q.x() * q.y() + q.w() * q.z()),
                        -2 * (q.y() * q.z() - q.w() * q.x()),
                        q.w() * q.w() - q.x() * q.x() + q.y() * q.y() - q.z() * q.z());
                break;

            case "xyz":
                rotations = threeAxisRotation(-2 * (q.y() * q.z() - q.w() * q.x()),
                        q.w() * q.w() - q.x() * q.x() - q.y() * q.y() + q.z() * q.z(),
                        2 * (q.x() * q.z() + q.w() * q.y()),
                        -2 * (q.x() * q.y() - q.w() * q.z()),
                        q.w() * q.w() + q.x() * q.x() - q.y() * q.y() - q.z() * q.z());
                break;

            case "xzy":
                rotations = threeAxisRotation(2 * (q.y() * q.z() + q.w() * q.x()),
                        q.w() * q.w() - q.x() * q.x() + q.y() * q.y() - q.z() * q.z(),
                        -2 * (q.x() * q.y() - q.w() * q.z()),
                        2 * (q.x() * q.z() + q.w() * q.y()),
                        q.w() * q.w() + q.x() * q.x() - q.y() * q.y() - q.z() * q.z());
                break;

            default:
                Log.i("EulerConversion", "Improper Sequence Entered");
                break;
        }
        return rotations;
    }


    //function to gather data from the wit motion sensors
    //returns the data in the type of a float array where the first
    // four values are the first quaternion and the second four are
    //the second quaternion from the sensors
    float w, i, j, k;
    private float[] getDeviceData(Bwt901ble sensor) {



        if(sensor!=null){
            if(sensor.getDeviceData(WitSensorKey.Q0) != null) {
                w = Float.parseFloat(sensor.getDeviceData(WitSensorKey.Q0));
                i = Float.parseFloat(sensor.getDeviceData(WitSensorKey.Q1));
                j = Float.parseFloat(sensor.getDeviceData(WitSensorKey.Q2));
                k = Float.parseFloat(sensor.getDeviceData(WitSensorKey.Q3));
            }else{
                return null;
            }
        }

        float[] data = {w, i, j, k};
        return data;
    }


    //function to get the data in the type of Quaternion for the first sensor
    private Quaternion dataToQuaternion(float[] data){
        if(data == null){
            return null;
        }
        return new Quaternion(data[0], data[1], data[2], data[3]);
    }


    //sensor fusion for new witmotion sensors
    private void sensorFusion(int sensorNum) {
        while (!destroyed) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            switch (intent) {
                case "pose":
                    if (posing) {
                        if (sensorNum == 1) {
                            s1CurrentQuat = dataToQuaternion(getDeviceData(sensor1));
                            Log.i("TherapyActivity", "Pose Route Executing - sensorNum: " + sensorNum + " - data: " + s1CurrentQuat);

                            s1PoseList.insert(s1CurrentQuat);
                        } else {
                            s2CurrentQuat = dataToQuaternion(getDeviceData(sensor2));
                            Log.i("TherapyActivity", "Pose Route Executing - sensorNum: " + sensorNum + " - data: " + s2CurrentQuat);
                            s2PoseList.insert(s2CurrentQuat);
                        }
                    }
                    break;
                case "therapy":
                    if (therapyActive) {
//                                Log.i("TherapyActivity", "Therapy Route Executing");
                        if (sensorNum == 1) {
                            Log.i("TherapyActivity", "Sensor 1: " + dataToQuaternion(getDeviceData(sensor1)));
                            s1RunningAverage[s1Index] = dataToQuaternion(getDeviceData(sensor1));
                            s1Index = (s1Index + 1) % RUNNING_AVG_SIZE;
                        } else {
                            Log.i("TherapyActivity", "Sensor 2: " + dataToQuaternion(getDeviceData(sensor2)));
                            s2RunningAverage[s2Index] = dataToQuaternion(getDeviceData(sensor2));
                            s2Index = (s2Index + 1) % RUNNING_AVG_SIZE;
                        }

                        // Computing Running Averages
                        s1CurrentQuat = avgQuaternionArray(s1RunningAverage);
                        s2CurrentQuat = avgQuaternionArray(s2RunningAverage);

                        // Compute Euler Angles From Averages
                        s1Angles = quaternionToEulerAngles(s1CurrentQuat, "zyx");
                        s2Angles = quaternionToEulerAngles(s2CurrentQuat, "xyz");

                        Log.i("TherapyActivity", "s1Angles: X = " + s1Angles[0] + " Y = " + s1Angles[1] + " Z = " + s1Angles[2]);
                        Log.i("TherapyActivity", "s2Angles: Z = " + s2Angles[0] + " Y = " + s2Angles[1] + " X = " + s2Angles[2]);
                    }
                    break;
            }
            getActivity().runOnUiThread(() -> {
                if (s1CurrentQuat != null && s2CurrentQuat != null) {
                    dataDisplay.setText("s1: " + s1CurrentQuat + "\ns2: " + s2CurrentQuat);
                }
            });
        }
    }



}






