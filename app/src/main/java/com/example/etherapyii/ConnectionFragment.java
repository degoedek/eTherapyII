package com.example.etherapyii;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wit.witsdk.modular.sensor.device.exceptions.OpenDeviceException;
import com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble;
import com.wit.witsdk.modular.sensor.example.ble5.interfaces.IBwt901bleRecordObserver;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothSPP;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.WitBluetoothManager;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.exceptions.BluetoothBLEException;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.interfaces.IBluetoothFoundObserver;
import com.wit.witsdk.modular.sensor.modular.processor.constant.WitSensorKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConnectionFragment extends Fragment implements IBluetoothFoundObserver, IBwt901bleRecordObserver {
    final String blueSensorMAC = "WT901BLE68(FD:2D:5E:7D:FA:10)";
    final String redSensorMAC = "WT901BLE68(CB:FB:0F:C4:D3:FB)";
    String therapy;
    private List<Bwt901ble> bwt901bleList = new ArrayList<>();
    private boolean destroyed = true;
    private SharedViewModel viewModel;
    int connectionColor = Color.parseColor("#FF26FF00");
    int redSensorColor = R.color.red;
    int blueSensorColor = R.color.eRunnerBlue;
    TextView sensorConnectionTV1;
    TextView sensorConnectionTV2;
    TextView sensor1Title;
    TextView sensor2Title;
    Button next;
    Button s1CalibrateBtn;
    Button s2CalibrateBtn;
    View view;
    Boolean magnetCalibrating = false;
    private final String TAG = "ConnectionFragment";
    private BatteryChecker batteryChecker1, batteryChecker2;
    private Thread thread1, thread2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_connection, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        //Variable Declarations
        //Buttons
        Button connect = view.findViewById(R.id.connect);
        sensorConnectionTV1 = view.findViewById(R.id.SensorConnection1);
        sensorConnectionTV2 = view.findViewById(R.id.SensorConnection2);
        next = view.findViewById(R.id.next_btn);
        s1CalibrateBtn = view.findViewById(R.id.s1Calibrate);
        s2CalibrateBtn = view.findViewById(R.id.s2Calibrate);
        sensor1Title = view.findViewById(R.id.Sensor1Title);
        sensor2Title = view.findViewById(R.id.Sensor2Title);

        // Getting Metric From Therapy Description
        assert getArguments() != null;
        therapy = getArguments().getString("therapy");

        try {
            WitBluetoothManager.requestPermissions(getActivity());
            // Initialize the Bluetooth manager, here will apply for Bluetooth permissions
            WitBluetoothManager.initInstance(getActivity());
        } catch (Exception e) {
            Log.e("BTCheck1", Objects.requireNonNull(e.getMessage()));
            e.printStackTrace();
        }

        // onClickListeners
        connect.setOnClickListener(view2 -> {
            Thread connectThread = new Thread(() -> {
                Log.i("ConnectionPage", "Connect thread started");

                // Re-attempting to initialize bluetooth manager - only necessary on the very first app usage
                try {
                    WitBluetoothManager.requestPermissions(getActivity());
                    // Initialize the Bluetooth manager, here will apply for Bluetooth permissions
                    WitBluetoothManager.initInstance(getActivity());
                } catch (Exception e) {
                    Log.e("BTCheck2", Objects.requireNonNull(e.getMessage()));
                    e.printStackTrace();
                }

                startDiscovery();
            });
            connectThread.start();
        });

        next.setOnClickListener(view -> {
            Bundle bundle = new Bundle();
            bundle.putString("therapy", therapy);

            SensorPlacementFragment sensorPlacementFragment = new SensorPlacementFragment();
            sensorPlacementFragment.setArguments(bundle);

            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.therapyContainer, sensorPlacementFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        s1CalibrationInflater();
        s2CalibrationInflater();

        // Inflate the layout for this fragment
        return view;
    }

    public void clearBluetooth() {
        // Turn off all devices
        for (int i = 0; i < bwt901bleList.size(); i++) {
            Bwt901ble bwt901ble = bwt901bleList.get(i);
            bwt901ble.removeRecordObserver(this);
            bwt901ble.close();
        }

        // Erase all devices
        bwt901bleList.clear();
    }

    public void startDiscovery() {
        // Start searching for bluetooth
        try {
            // get bluetooth manager
            WitBluetoothManager bluetoothManager = WitBluetoothManager.getInstance();
            // Monitor communication signals
            bluetoothManager.registerObserver(this);

            // start search
            bluetoothManager.startDiscovery();
        } catch (BluetoothBLEException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRecord(Bwt901ble bwt901ble) {
        // Maybe TODO: getDeviceData Method from test app - also might not be necessary in ConnectionFragment
        // String deviceData = getDeviceData(bwt901ble);
        // Log.d(TAG, "device data [ " + bwt901ble.getDeviceName() + "] = " + deviceData);
    }

    @Override
    public synchronized void onFoundBle(BluetoothBLE bluetoothBLE) {
        // Create a Bluetooth 5.0 sensor connection object
        Bwt901ble bwt901ble = new Bwt901ble(bluetoothBLE);

        // Avoid duplicate connections
        for (int i = 0; i < bwt901bleList.size(); i++) {
            if (Objects.equals(bwt901bleList.get(i).getDeviceName(), bwt901ble.getDeviceName())) {
                return;
            }
        }

        // add to device list
        bwt901bleList.add(bwt901ble);

        // Registration data record
        bwt901ble.registerRecordObserver(this);

        // Turn on the device
        try {
            bwt901ble.open();
        } catch (OpenDeviceException e) {
            // Failed to open device
            e.printStackTrace();
        }

        Log.i("ConnectionFragment", "bwt901ble list size: " + bwt901bleList.size());

        String currentMAC = bwt901ble.getDeviceName();
        Log.i("ConnectionFragment", "Sensor " + bwt901bleList.size() + " MAC: " + currentMAC);

        // Sensor Specific Changes
        if (bwt901bleList.size() == 1) {
            viewModel.setSensor1(bwt901ble);
            sensorConnectionTV1.setText("Connected");
            sensorConnectionTV1.setBackgroundColor(connectionColor);
            s1CalibrateBtn.setVisibility(View.VISIBLE);
            startBatteryCheck(bwt901ble, 1);

            // Check MAC address to know which sensor it is
            if (Objects.equals(currentMAC, redSensorMAC)) {
                sensor1Title.setBackgroundColor(Color.RED);
                sensor1Title.setTextColor(Color.WHITE);
            } else if (Objects.equals(currentMAC, blueSensorMAC)) {
                sensor1Title.setBackgroundColor(Color.BLUE);
                sensor1Title.setTextColor(Color.WHITE);
            } else {
                Log.i("ConnectionFragment", "EROROROROROOROOROROROORORORORO");
            }
        } else {
            viewModel.setSensor2(bwt901ble);
            sensorConnectionTV2.setText("Connected");
            sensorConnectionTV2.setBackgroundColor(connectionColor);
            s2CalibrateBtn.setVisibility(View.VISIBLE);
            startBatteryCheck(bwt901ble, 2);

            if (Objects.equals(currentMAC, redSensorMAC)) {
                sensor2Title.setBackgroundColor(Color.RED);
                sensor2Title.setTextColor(Color.WHITE);
            } else if (Objects.equals(currentMAC, blueSensorMAC)) {
                sensor2Title.setBackgroundColor(Color.BLUE);
                sensor2Title.setTextColor(Color.WHITE);
            } else {
                Log.i("ConnectionFragment", "EROROROROROOROOROROROORORORORO");
            }
        }

        // Make the next button visible
        if (bwt901bleList.size() == 2) {
            next.setVisibility(View.VISIBLE);
        }
    }

    // Required blank method
    @Override
    public void onFoundSPP(BluetoothSPP bluetoothSPP) {
    }

    private void startBatteryCheck(Bwt901ble sensor, int sensorNum) {
        BatteryChecker batteryChecker = new BatteryChecker(sensor, sensorNum, view, new Handler(Looper.getMainLooper()));
        Thread thread = new Thread(batteryChecker);
        thread.start();

        if (sensorNum == 1) {
            batteryChecker1 = batteryChecker;
            thread1 = thread;
        } else if (sensorNum == 2) {
            batteryChecker2 = batteryChecker;
            thread2 = thread;
        }
    }

    private String getMAC(Bwt901ble sensor) {
        return sensor.getDeviceData(WitSensorKey.VersionNumber);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (batteryChecker1 != null) {
            batteryChecker1.stop();
            thread1.interrupt();
        }
        if (batteryChecker2 != null) {
            batteryChecker2.stop();
            thread2.interrupt();
        }
    }

    public void s1CalibrationInflater() {
        // TODO: Add a retry calibration
        // Variable Declarations:
        AlertDialog s1CalibrationScreen;
        Button s1Calibrate = view.findViewById(R.id.s1Calibrate);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setCancelable(false);
        View view2 = getLayoutInflater().inflate(R.layout.popup_s1_calibration_screen, null);

        TextView sensorConnection1 = view.findViewById(R.id.SensorConnection1);
        TextView accelInstructions = view2.findViewById(R.id.accelCalibrationInstructions);
        TextView magnetInstructions = view2.findViewById(R.id.magneticCalibrationInstructions);
        LinearLayout accelLL = view2.findViewById(R.id.accelerometerCalLinearLayout);
        LinearLayout magnetLL = view2.findViewById(R.id.magneticCalLinearLayout);

        ImageButton escape = view2.findViewById(R.id.emergencyExit);
        Button close = view2.findViewById(R.id.close);
        Button accelCal = view2.findViewById(R.id.accelCalibrationBtn);
        Button magnetCal = view2.findViewById(R.id.magneticCalibrationBtn);

        builder.setView(view2);
        s1CalibrationScreen = builder.create();
        s1Calibrate.setOnClickListener(view -> s1CalibrationScreen.show());

        accelCal.setOnClickListener(view -> {
            // Step 1a: Accelerometer Calibration
            accelCalibration(bwt901bleList.get(0));

            // Step 1b: Button Countdown
            new CountDownTimer(3000, 1000) {

                @Override
                public void onTick(long l) {
                    accelCal.setText("Calibrating...\n" + (l / 1000 + 1));
                }

                @Override
                public void onFinish() {
                    accelCal.setText("Accelerometer Calibration");

                    // Step 2: Hiding UI Elements
                    accelLL.setVisibility(View.GONE);
                    accelInstructions.setVisibility(View.GONE);

                    // Step 3: Display UI Elements
                    magnetLL.setVisibility(View.VISIBLE);
                    magnetInstructions.setVisibility(View.VISIBLE);
                }
            }.start();
        });

        magnetCal.setOnClickListener(view -> {
            if (!magnetCalibrating) {
                magnetCalibrating = true;

                // Step 1a: Button Text
                magnetCal.setText("Stop Calibrating");

                // Step 1b: Magnetic Calibration
                magneticCalibrationStart(bwt901bleList.get(0));
            } else {
                magnetCalibrating = false;
                magneticCalibrationEnd(bwt901bleList.get(0));

                // Step 2: Hiding UI Elements
                magnetLL.setVisibility(View.GONE);

                // Step 3: Alter magnetInstructions Text
                magnetInstructions.setText("Sensor Successfully Calibrated");
                magnetInstructions.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 35);

                // Step 4: Make UI Elements Visible
                close.setVisibility(View.VISIBLE);
            }
        });

        escape.setOnClickListener(view -> s1CalibrationScreen.dismiss());
        close.setOnClickListener(view -> {
            s1CalibrationScreen.dismiss();
            sensorConnection1.setText("Connected\nCalibrated");
        });
    }

    public void s2CalibrationInflater() {
        // TODO: Add a retry calibration
        // Variable Declarations:
        AlertDialog s2CalibrationScreen;
        Button s2Calibrate = view.findViewById(R.id.s2Calibrate);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setCancelable(false);
        View view2 = getLayoutInflater().inflate(R.layout.popup_s2_calibration_screen, null);

        TextView sensorConnection2 = view.findViewById(R.id.SensorConnection2);
        TextView accelInstructions = view2.findViewById(R.id.accelCalibrationInstructions);
        TextView magnetInstructions = view2.findViewById(R.id.magneticCalibrationInstructions);
        LinearLayout accelLL = view2.findViewById(R.id.accelerometerCalLinearLayout);
        LinearLayout magnetLL = view2.findViewById(R.id.magneticCalLinearLayout);

        ImageButton escape = view2.findViewById(R.id.emergencyExit);
        Button close = view2.findViewById(R.id.close);
        Button accelCal = view2.findViewById(R.id.accelCalibrationBtn);
        Button magnetCal = view2.findViewById(R.id.magneticCalibrationBtn);

        builder.setView(view2);
        s2CalibrationScreen = builder.create();
        s2Calibrate.setOnClickListener(view -> s2CalibrationScreen.show());

        accelCal.setOnClickListener(view -> {
            // Step 1a: Accelerometer Calibration
            accelCalibration(bwt901bleList.get(1));

            // Step 1b: Button Countdown
            new CountDownTimer(3000, 1000) {

                @Override
                public void onTick(long l) {
                    accelCal.setText("Calibrating...\n" + (l / 1000 + 1));
                }

                @Override
                public void onFinish() {
                    accelCal.setText("Accelerometer Calibration");

                    // Step 2: Hiding UI Elements
                    accelLL.setVisibility(View.GONE);
                    accelInstructions.setVisibility(View.GONE);

                    // Step 3: Display UI Elements
                    magnetLL.setVisibility(View.VISIBLE);
                    magnetInstructions.setVisibility(View.VISIBLE);
                }
            }.start();
        });

        magnetCal.setOnClickListener(view -> {
            if (!magnetCalibrating) {
                magnetCalibrating = true;

                // Step 1a: Button Text
                magnetCal.setText("Stop Calibrating");

                // Step 1b: Magnetic Calibration
                magneticCalibrationStart(bwt901bleList.get(1));
            } else {
                magnetCalibrating = false;
                magneticCalibrationEnd(bwt901bleList.get(1));

                // Step 2: Hiding UI Elements
                magnetLL.setVisibility(View.GONE);

                // Step 3: Alter magnetInstructions Text
                magnetInstructions.setText("Sensor Successfully Calibrated");
                magnetInstructions.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 35);

                // Step 4: Make UI Elements Visible
                close.setVisibility(View.VISIBLE);
            }
        });

        escape.setOnClickListener(view -> s2CalibrationScreen.dismiss());
        close.setOnClickListener(view -> {
            s2CalibrationScreen.dismiss();
            sensorConnection2.setText("Connected\nCalibrated");
        });
    }

    private void accelCalibration(Bwt901ble sensor) {
        // unlock register
        sensor.unlockReg();

        // send command
        sensor.appliedCalibration();

        Toast.makeText(getActivity(), "Accelerometer Calibrating", Toast.LENGTH_SHORT).show();
    }

    private void magneticCalibrationStart(Bwt901ble sensor) {
        // unlock register
        sensor.unlockReg();

        // send command
        sensor.startFieldCalibration();

        Toast.makeText(getActivity(), "Magnetometer Calibrating", Toast.LENGTH_SHORT).show();
    }

    private void magneticCalibrationEnd(Bwt901ble sensor) {
        // unlock register
        sensor.unlockReg();

        // send command
        sensor.endFieldCalibration();

        Toast.makeText(getActivity(), "Magnetometer Finished Calibrating", Toast.LENGTH_SHORT).show();
    }

    private static class BatteryChecker implements Runnable {
        private final Bwt901ble sensor;
        private final int sensorNum;
        private final Handler uiHandler;
        private final View view;
        private volatile boolean running = true;

        public BatteryChecker(@NonNull Bwt901ble sensor, int sensorNum, View view, Handler uiHandler) {
            this.sensor = sensor;
            this.sensorNum = sensorNum;
            this.view = view;
            this.uiHandler = uiHandler;
        }

        @Override
        public void run() {
            while (running) {
                String batteryPercentage = sensor.getDeviceData(WitSensorKey.ElectricQuantityPercentage);

                if (batteryPercentage != null) {
                    uiHandler.post(() -> updateUI(batteryPercentage));
                    break;
                }

                try {
                    Thread.sleep(1000); // Check every 1 second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    running = false;
                }
            }
        }

        private void updateUI(String batteryPercentage) {
            TextView s1BP = view.findViewById(R.id.s1batteryPercent);
            TextView s2BP = view.findViewById(R.id.s2batteryPercent);

            ImageView s1B = view.findViewById(R.id.s1battery);
            ImageView s2B = view.findViewById(R.id.s2battery);

            if (sensorNum == 1) {
                String stringS1BP = "      " + batteryPercentage + "%";
                s1BP.setText(stringS1BP);

                //Updating Imaging
                updateBatteryImage(s1B, batteryPercentage);
            } else if (sensorNum == 2) {
                String stringS2BP = "      " + batteryPercentage + "%";
                s2BP.setText(stringS2BP);

                //Updating Imaging
                updateBatteryImage(s2B, batteryPercentage);
            } else {
                Log.wtf("BatteryChecker", "Wrong sensor number!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
        }

        private void updateBatteryImage(ImageView batteryImage, String batteryPercentage) {
            double percentage = Double.parseDouble(batteryPercentage);

            if (75 < percentage && percentage <= 100) {
                batteryImage.setImageResource(R.drawable.battery_full);
            } else if (50 < percentage && percentage <= 75) {
                batteryImage.setImageResource(R.drawable.battery_medium);
            } else if (25 < percentage && percentage <= 50) {
                batteryImage.setImageResource(R.drawable.battery_low);
            } else if (0 <= percentage && percentage <= 25) {
                batteryImage.setImageResource(R.drawable.battery_dead);
            }
        }

        public void stop() {
            running = false;
        }
    }

}
