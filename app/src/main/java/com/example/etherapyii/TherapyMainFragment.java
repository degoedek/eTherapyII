package com.example.etherapyii;

import static java.lang.System.currentTimeMillis;

import android.graphics.PointF;
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
    private boolean isClockRunning = false, started = false, repStarted = false;
    private final float SCALING_FACTOR = 2f;
    SharedViewModel viewModel;
    private String time;
    private Handler handler;
    private long startTime;
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
    private TextView distanceTV, HoldTV, poseDisplay, dataDisplay, s1AngularDifferenceTV, s2AngularDifferenceTV;
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

        viewModel.getSensor1().observe(getViewLifecycleOwner(), newSensor -> {
            sensor1 = newSensor;
            Log.d("SensorFragment", "Sensor1 received: " + sensor1);
        });

        viewModel.getSensor2().observe(getViewLifecycleOwner(), newSensor -> {
            sensor2 = newSensor;
            Log.d("SensorFragment", "Sensor1 received: " + sensor2);
        });

        TextView titleTV = view.findViewById(R.id.titleTV);
        TextView repsTV = view.findViewById(R.id.repsTV);
        TextView timeTV = view.findViewById(R.id.timeTV);
        Button beginButton = view.findViewById(R.id.beginButton);
        Button stopButton = view.findViewById(R.id.btn_stop);
        dataDisplay = view.findViewById(R.id.dataDisplay);
        poseDisplay = view.findViewById(R.id.poseDisplay);
        s1AngularDifferenceTV = view.findViewById(R.id.s1AngularDifference);
        s2AngularDifferenceTV = view.findViewById(R.id.s2AngularDifference);
        circleUserWithNotch = view.findViewById(R.id.circle_user_with_notch);
        circleGoalWithNotch = view.findViewById(R.id.circle_goal_with_notch);
        int[] userCoordinates = new int[2];
        String therapyType;
        int reps, repsCompleted = 0;
        String repsText;

        circleGoalWithNotch.post(() -> {
            initialX = circleGoalWithNotch.getX() + circleGoalWithNotch.getWidth() / 2 - circleUserWithNotch.getWidth() / 2;
            initialY = circleGoalWithNotch.getY() + circleGoalWithNotch.getHeight() / 2 - circleUserWithNotch.getHeight() / 2;
            circleUserWithNotch.setX(initialX);
            circleUserWithNotch.setY(initialY);
        });

        assert getArguments() != null;
        therapyType = getArguments().getString("therapy");
        reps = getArguments().getInt("reps");
        HOLD_TIME = getArguments().getInt("holdTime");
        Log.i("TherapyMainFragment", "therapyType: " + therapyType + " reps: " + reps + " holdTime: " + HOLD_TIME);

        switch (Objects.requireNonNull(therapyType)) {
            case "HOTT":
                titleTV.setText("Head Orientation\nTherapy Tool");
                break;
            default:
                titleTV.setText("Placeholder: Add title");
        }

        repsText = repsCompleted + "/" + reps;
        repsTV.setText(repsText);
        timeTV.setText("0:00");

        handler = new Handler(Looper.getMainLooper());

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
            SummaryFragment summaryFragment = new SummaryFragment();
            summaryFragment.setArguments(bundle);
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.therapyContainer, summaryFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        destroyed = false;
        return view;
    }

    private void refreshDataTh() {
        while (!destroyed) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

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

                posing = false;

                s1Pose = s1PoseList.averageQuaternions();
                Log.i("TherapyMainFragment", "s1Pose Not Normalized: " + s1Pose);
                s1Pose = normalize(s1Pose);
                Log.i("TherapyMainFragment", "s1Pose Normalized: " + s1Pose);
                s2Pose = s2PoseList.averageQuaternions();
                s2Pose = normalize(s2Pose);

                s1PoseAngles = quaternionToEulerAngles(s1Pose, "zxy");
                s2PoseAngles = quaternionToEulerAngles(s2Pose, "xzy");

                Log.i("TherapyActivity", "S1 Pose Angles - X = " + s1PoseAngles[0] + " Y = " + s1PoseAngles[1] + " Z = " + s1PoseAngles[2]);
                Log.i("TherapyActivity", "S2 Pose Angles - Z = " + s2PoseAngles[0] + " Y = " + s2PoseAngles[1] + " X = " + s2PoseAngles[2]);
                String poseDisplayString = "Pose\nS1: X = " + String.format("%.3f", s1PoseAngles[0]) + " Y = " + String.format("%.3f", s1PoseAngles[1]) + " Z = " + String.format("%.3f", s1PoseAngles[2]) + "\nS2: Z = " + String.format("%.3f", s2PoseAngles[0]) + " Y = " + String.format("%.3f", s2PoseAngles[1]) + " X = " + String.format("%.3f", s2PoseAngles[2]);
                poseDisplay.setText(poseDisplayString);

                isClockRunning = true;
                startTime = currentTimeMillis();
                startClock(timeTV);

                therapyActive = true;
                intent = "therapy";

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

                    handler.postDelayed(this, 500); // Update every half second
                }
            }
        });
    }

    public void getPose() {
        if (sensor1 != null && sensor2 != null) {
            S1PoseThread.start();
            S2PoseThread.start();
        } else {
            Log.wtf("Error", "Boards Not Connected - Sensor Fusion not executed - will crash at the end of the countdown");
        }
    }

    private void updateCirclePosition(Quaternion s1Quaternion, Quaternion s2Quaternion) {
        executorService.execute(() -> {
            PointF point = quaternionTo2D(findRelativeRotation(s1Quaternion, s2Quaternion));
            uiHandler.post(() -> {
                float scaledX = point.x * SCALING_FACTOR;
                float scaledY = point.y * SCALING_FACTOR;

                float newX = initialX + scaledX;
                float newY = initialY + scaledY;

                newX = Math.max(0, Math.min(newX, view.getWidth() - circleUserWithNotch.getWidth()));
                newY = Math.max(0, Math.min(newY, view.getHeight() - circleUserWithNotch.getHeight()));

                circleUserWithNotch.setX(newX);
                circleUserWithNotch.setY(newY);

                Log.i("TherapyMainFragment", "Updated User Circle Position: X - " + newX + " Y - " + newY);
            });
        });
    }

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
                        if (sensorNum == 1) {
                            s1RunningAverage[s1Index] = dataToQuaternion(getDeviceData(sensor1));
                            Log.i("TherapyActivity", "Sensor 1: " + s1RunningAverage[s1Index]);
                            s1Index = (s1Index + 1) % RUNNING_AVG_SIZE;
                        } else {
                            s2RunningAverage[s2Index] = dataToQuaternion(getDeviceData(sensor2));
                            Log.i("TherapyActivity", "Sensor 2: " + s2RunningAverage[s2Index]);
                            s2Index = (s2Index + 1) % RUNNING_AVG_SIZE;
                        }

                        s1CurrentQuat = avgQuaternionArray(s1RunningAverage);
                        s2CurrentQuat = avgQuaternionArray(s2RunningAverage);

                        Log.i("TherapyMainActivity", "s1CurrentQuat Running Average Calculation: " + s1CurrentQuat);
                        Log.i("TherapyMainActivity", "s2CurrentQuat Running Average Calculation: " + s2CurrentQuat);

                        currentDistance = quaternionDistance(s1CurrentQuat, s2CurrentQuat);

                        s1Angles = quaternionToEulerAngles(s1CurrentQuat, "zxy");
                        s2Angles = quaternionToEulerAngles(s2CurrentQuat, "xzy");

                        Log.i("TherapyActivity", "s1Angles: X = " + s1Angles[0] + " Y = " + s1Angles[1] + " Z = " + s1Angles[2]);
                        Log.i("TherapyActivity", "s2Angles: Z = " + s2Angles[0] + " Y = " + s2Angles[1] + " X = " + s2Angles[2]);

                        updateCirclePosition(s1CurrentQuat, s2CurrentQuat);
                    }
                    break;
            }

            double[] s1AngularDifference = new double[3];
            double[] s2AngularDifference = new double[3];

            s1AngularDifference[0] = optimalAngularDifference(s1PoseAngles[0], s1Angles[0]);
            s1AngularDifference[1] = optimalAngularDifference(s1PoseAngles[1], s1Angles[1]);
            s1AngularDifference[2] = optimalAngularDifference(s1PoseAngles[2], s1Angles[2]);

            s2AngularDifference[0] = optimalAngularDifference(s2PoseAngles[0], s2Angles[0]);
            s2AngularDifference[1] = optimalAngularDifference(s2PoseAngles[1], s2Angles[1]);
            s2AngularDifference[2] = optimalAngularDifference(s2PoseAngles[2], s2Angles[2]);

            requireActivity().runOnUiThread(() -> {
                if (s1CurrentQuat != null && s2CurrentQuat != null) {
                    String dataDisplayString = "s1: X = " + String.format("%.3f", s1Angles[0]) + " Y = " + String.format("%.3f", s1Angles[1]) + " Z = " + String.format("%.3f", s1Angles[2]) + "\ns2: Z = " + String.format("%.3f", s2Angles[0]) + " Y = " + String.format("%.3f", s2Angles[1]) + " X = " + String.format("%.3f", s2Angles[2]);
                    dataDisplay.setText(dataDisplayString);

                    String s1AngularDifferenceString = "s1:\nX = " + String.format("%.3f", s1AngularDifference[0]) + "\nY = " + String.format("%.3f", s1AngularDifference[1]) + "\nZ = " + String.format("%.3f", s1AngularDifference[2]);
                    s1AngularDifferenceTV.setText(s1AngularDifferenceString);
                    String s2AngularDifferenceString = "s2:\nZ = " + String.format("%.3f", s2AngularDifference[0]) + "\nY = " + String.format("%.3f", s2AngularDifference[1]) + "\nX = " + String.format("%.3f", s2AngularDifference[2]);
                    s2AngularDifferenceTV.setText(s2AngularDifferenceString);
                }
            });
        }
    }

    public double optimalAngularDifference(double pose, double current) {
        double result;

        result = current - pose;

        result = result % 360;
        if (result < -180) {
            result += 360;
        } else if (result >= 180) {
            result -= 360;
        }

        return result;
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
        if (q == null) {
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

    float w, i, j, k;
    private synchronized float[] getDeviceData(Bwt901ble sensor) {
        if (sensor != null) {
            if (sensor.getDeviceData(WitSensorKey.Q0) != null) {
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

    private Quaternion dataToQuaternion(float[] data) {
        if (data == null) {
            return null;
        }
        return new Quaternion(data[0], data[1], data[2], data[3]);
    }

    private PointF quaternionTo2D(Quaternion q) {
        float angleX = (float) Math.atan2(2.0f * (q.w() * q.x() + q.y() * q.z()), 1.0f - 2.0f * (q.x() * q.x() + q.y() * q.y()));
        float angleY = (float) Math.asin(2.0f * (q.w() * q.y() - q.z() * q.x()));

        // Mapping angles to screen coordinates
        float x = (float) Math.sin(angleX);
        float y = (float) Math.sin(angleY);

        Log.i("QuaternionTo2D", "Quaternion: " + q + " -> PointF: x=" + x + ", y=" + y);
        return new PointF(x, y);
    }
}
