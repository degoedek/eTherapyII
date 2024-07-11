package com.example.etherapyii;

import static java.lang.Math.toRadians;
import static java.lang.System.currentTimeMillis;

import android.graphics.PointF;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble;
import com.wit.witsdk.modular.sensor.modular.processor.constant.WitSensorKey;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TherapyMainFragment extends Fragment {
    private boolean positionChanged = false;
    private boolean isClockRunning = false, started = false;
    SharedViewModel viewModel;
    private String time;
    private Handler handler;
    private long startTime;
    private Quaternion s1CurrentQuat, s2CurrentQuat, s1Pose, s2Pose, RelativeRotationPose, RelativeRotationCurrent;
    private Boolean posing = false, therapyActive = false;
    private int timeLeft;
    private DoublyLinkedList s1PoseList = new DoublyLinkedList();
    private DoublyLinkedList s2PoseList = new DoublyLinkedList();
    private final int RUNNING_AVG_SIZE = 5;
    private float ACCURACY_THRESHOLD;
    private Quaternion[] s1RunningAverage = new Quaternion[RUNNING_AVG_SIZE];
    private Quaternion[] s2RunningAverage = new Quaternion[RUNNING_AVG_SIZE];
    private int s1Index = 0, s2Index = 0;
    private float currentDistance;
    private Thread S1PoseThread = new Thread(() -> {
        sensorFusion(1);
    });
    private Thread S2PoseThread = new Thread(() -> {
        sensorFusion(2);
    });
    private Thread trackThread = new Thread(this::trackHold);
    private String intent = "pose";
    private TextView distanceTV, HoldTV;
    private ImageView circleUserWithNotch, circleGoalWithNotch;
    private int HOLD_TIME, timeHeld;
    private View view;
    private Handler uiHandler = new Handler();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private double[] s1PoseAngles = new double[3];
    private double[] s2PoseAngles = new double[3];
    private double[] s1Angles = new double[3];
    private double[] s2Angles = new double[3];
    private float initialX, initialY;
    private Bwt901ble sensor1, sensor2;
    private boolean destroyed = true;
    int reps, repsCompleted = 0;
    MediaPlayer player;
    boolean repsCompletedB = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_therapy_main, container, false);

        viewModel.getSensor1().observe(getViewLifecycleOwner(), newSensor -> {
            // Update the UI with the new sensor data
            sensor1 = newSensor;
            // Example: Log the sensor data
            Log.d("SensorFragment", "Sensor1 received: " + sensor1);
        });

        // Update the UI with the new sensor data
        viewModel.getSensor2().observe(getViewLifecycleOwner(), newSensor -> {
            // Update the UI with the new sensor data
            sensor2 = newSensor;
            // Example: Log the sensor data
            Log.d("SensorFragment", "Sensor1 received: " + sensor2);
        });

        // Variable Declaration
        TextView titleTV = view.findViewById(R.id.titleTV);
        TextView repsTV = view.findViewById(R.id.repsTV);
        TextView timeTV = view.findViewById(R.id.timeTV);
        Button beginButton = view.findViewById(R.id.beginButton);
        Button stopButton = view.findViewById(R.id.btn_stop);
        HoldTV = view.findViewById(R.id.HoldTV);
        circleUserWithNotch = view.findViewById(R.id.circle_user_with_notch);
        circleGoalWithNotch = view.findViewById(R.id.circle_goal_with_notch);
        int[] userCoordinates = new int[2];
        String therapyType;
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
        ACCURACY_THRESHOLD = getArguments().getInt("threshold");
        Log.i("TherapyMainFragment", "Accuracy set " + ACCURACY_THRESHOLD);
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
            trackThread.interrupt();
            if (player != null) {
                player.release();
                player = null;
            }

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
                Log.i("TherapyMainFragment", "s1Pose Not Normalized: " + s1Pose);
                s1Pose = normalize(s1Pose);
                Log.i("TherapyMainFragment", "s1Pose Normalized: " + s1Pose);
                s2Pose = s2PoseList.averageQuaternions();
                s2Pose = normalize(s2Pose);
                Log.i("TherapyMainFragment", "s2Pose Normalized: " + s2Pose);


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

                trackThread.start();

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
                    distanceTV.setText(String.format("Current Angle:\n%.3f", currentDistance));
                    TextView repsTV = view.findViewById(R.id.repsTV);

                    repsTV.setText(repsCompleted + "/" + reps);

                    handler.postDelayed(this, 500); // Update every half second
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

        w3 = w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2;
        x3 = w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2;
        y3 = w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2;
        z3 = w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2;

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

    public static Quaternion applyCombinedRotation(Quaternion inputQuaternion) {
        // Define the 90-degree rotation around the Y-axis and Z-axis in radians
        float angleY = (float) toRadians(90);
        float angleZ = (float)toRadians(90);

        // Rotate first around Y-axis, then around Z-axis
        Quaternion rotatedQuaternion = inputQuaternion.rotateY(angleY).rotateZ(angleZ);

        return rotatedQuaternion;
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

    // Function to gather data from the wit motion sensors
    // Returns the data in the type of a float array where the first
    // four values are the first quaternion and the second four are
    // the second quaternion from the sensors
    float w, i, j, k;
    private synchronized float[] getDeviceData(Bwt901ble sensor) {
        if(sensor!=null){
            if(sensor.getDeviceData(WitSensorKey.Q0) != null) {
                w = Float.parseFloat(sensor.getDeviceData(WitSensorKey.Q0));
                i = Float.parseFloat(sensor.getDeviceData(WitSensorKey.Q1));
                j = Float.parseFloat(sensor.getDeviceData(WitSensorKey.Q2));
                k = Float.parseFloat(sensor.getDeviceData(WitSensorKey.Q3));
            } else {
                return null;
            }
        }
        return new float[]{w, i, j, k};
    }

    // Function to get the data in the type of Quaternion for the first sensor
    private Quaternion dataToQuaternion(float[] data){
        if(data == null){
            return null;
        }
        return new Quaternion(data[0], data[1], data[2], data[3]);
    }

    private void sensorFusion(int sensorNum) {
        try {
            while (!destroyed) {
                Thread.sleep(200);

                switch (intent) {
                    case "pose":
                        if (posing) {
                            if (sensorNum == 1) {
                                s1CurrentQuat = dataToQuaternion(getDeviceData(sensor1));
                                s1PoseList.insert(s1CurrentQuat);
                            } else {
                                s2CurrentQuat = dataToQuaternion(getDeviceData(sensor2));
                                s2PoseList.insert(s2CurrentQuat);
                            }
                        }
                        break;
                    case "therapy":
                        if (therapyActive) {
                            if (sensorNum == 1) {
                                s1RunningAverage[s1Index] = dataToQuaternion(getDeviceData(sensor1));
                                s1Index = (s1Index + 1) % RUNNING_AVG_SIZE;
                            } else {
                                s2RunningAverage[s2Index] = dataToQuaternion(getDeviceData(sensor2));
                                s2Index = (s2Index + 1) % RUNNING_AVG_SIZE;
                            }

                            s1CurrentQuat = avgQuaternionArray(s1RunningAverage);
                            s2CurrentQuat = avgQuaternionArray(s2RunningAverage);

                            RelativeRotationPose = findRelativeRotation(s1Pose, s2Pose);
                            RelativeRotationCurrent = findRelativeRotation(s1CurrentQuat, s2CurrentQuat);

                            currentDistance = quaternionDistance(RelativeRotationPose, RelativeRotationCurrent);

                            // Update the UI with the new circle positions
//                            updateCirclePosition();
                        }
                        break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e("TherapyActivity", "Sensor Fusion interrupted", e);
        }
    }

    private PointF initialCirclePosition(ImageView circle, Quaternion q, boolean isRedSensor) {
        if (q != null) {
            PointF position;

            position = quaternionTo2D(q, isRedSensor);


            return position;
        } else {
            Log.e("initialCirclePosition", "Error: Quaternion null");
            return null;
        }
    }

    private void updateCirclePosition() {
        if (RelativeRotationCurrent != null) {
            PointF redPosition = quaternionTo2D(s1CurrentQuat, true);
            PointF bluePosition = quaternionTo2D(s2CurrentQuat, false);

            // Convert the normalized position to screen coordinates
            float screenXRed = initialX + redPosition.x * 100; // Adjust the multiplier as needed
            float screenYRed = initialY + redPosition.y * 100; // Adjust the multiplier as needed

            float screenXBlue = initialX + bluePosition.x * 100; // Adjust the multiplier as needed
            float screenYBlue = initialY + bluePosition.y * 100; // Adjust the multiplier as needed

            uiHandler.post(() -> {
                circleUserWithNotch.setX(screenXRed);
                circleUserWithNotch.setY(screenYRed);
                // Update the goal circle or other UI elements as needed
                circleGoalWithNotch.setX(screenXBlue);
                circleGoalWithNotch.setY(screenYBlue);
            });
        }
    }



    public double optimalAngularDifference(double pose, double current) {
        double result;

        result = Math.abs(pose) + Math.abs(current);

        if (result > 180) {
            result = 360 - result;
        }

        return result;
    }

    private void updateHoldTimer(long startTime) {
        long elapsedMillis = System.currentTimeMillis() - startTime;
        int seconds = (int) (elapsedMillis / 1000);
        int remainingSeconds = HOLD_TIME - seconds;

        if (remainingSeconds >= 0) {
            postUpdateHoldTextView(remainingSeconds);
        }
    }

    private void postUpdateHoldTextView(int remainingSeconds) {
        uiHandler.post(() -> {
            String holdText = "Hold: " + remainingSeconds + "s";
            HoldTV.setText(holdText);
        });
    }

    public void trackHold() {
        Log.i("TherapyActivity", "Track Hold running");

        long startTime = System.currentTimeMillis();
        boolean holdStarted = false, posed = true;
        double DISTANCE_CHANGE_THRESHOLD = 1.25 * ACCURACY_THRESHOLD;

        try {
            while (true) { // Use a condition to exit the loop
                if (currentDistance >= DISTANCE_CHANGE_THRESHOLD) {
                    positionChanged = true;
                    posed = false;
                }

                synchronized (this) { // Ensure thread-safe read of currentDistance
                    if (currentDistance < ACCURACY_THRESHOLD) {
                        if (!holdStarted) {
                            startTime = System.currentTimeMillis();
                            holdStarted = true;
                            player = MediaPlayer.create(requireActivity(), R.raw.ping_sound);
                            player.start();
                        }

                        if (!posed && positionChanged) {
                            updateHoldTimer(startTime); // Continuously update the hold timer while holding
                        }

                        if (System.currentTimeMillis() - startTime >= (HOLD_TIME * 1000)) {
                            if (positionChanged) {
                                repsCompleted++;
                                holdStarted = false;
                                positionChanged = false;
                                if (repsCompleted == reps && !repsCompletedB) {
                                    player = MediaPlayer.create(requireActivity(), R.raw.exercise_complete_sound);
                                    player.start();
                                    repsCompletedB = true;
                                } else {
                                    player = MediaPlayer.create(requireActivity(), R.raw.rep_complete_sound);
                                    player.start();
                                }

                                postUpdateHoldTextView(""); // Clear the hold text
                                Thread.sleep(2 * 1000);
                            }
                        }
                    } else {
                        startTime = System.currentTimeMillis();
                        if (holdStarted) {
                            player = MediaPlayer.create(requireActivity(), R.raw.fail_sound);
                            player.start();
                            postUpdateHoldTextView(""); // Clear the hold text
                        }
                        holdStarted = false;
                    }
                }
                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e("TherapyActivity", "Track Hold interrupted", e);
        }
    }

    private void postUpdateHoldTextView(String text) {
        uiHandler.post(() -> HoldTV.setText(text));
    }

    private synchronized PointF quaternionTo2D(Quaternion q, boolean isRedSensor) {
        // TODO: FIX
        // Red sensor must be connected first for this to work
        float x, y;

        if (isRedSensor) {
            // For red sensor: Use y (i) and z (k) components
            y = (float) Math.asin(q.y() / Math.sqrt(q.w() * q.w() + q.y() * q.y()));
            x = (float) -(Math.asin(q.z() / Math.sqrt(q.w() * q.w() + q.z() * q.z())));
        } else { // TODO: Fix
            // For blue sensor: Use x (i) and y (j) components
            x = (float) -(Math.asin(q.x() / Math.sqrt(q.w() * q.w() + q.x() * q.x())));
            y = (float) -(Math.asin(q.y() / Math.sqrt(q.w() * q.w() + q.y() * q.y())));
        }

        Log.i("QuaternionTo2D", "Quaternion: " + q + " -> PointF: x=" + x + ", y=" + y);
        return new PointF(x, y);
    }

}

