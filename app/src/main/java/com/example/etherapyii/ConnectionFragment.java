package com.example.etherapyii;

import static android.service.controls.ControlsProviderService.TAG;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.nfc.Tag;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.os.CountDownTimer;
import android.os.IBinder;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConnectionFragment extends Fragment implements IBluetoothFoundObserver, IBwt901bleRecordObserver{
    String therapy;
    private List<Bwt901ble> bwt901bleList = new ArrayList<>();
    private boolean destroyed = true;
    private SharedViewModel viewModel;
    int connectionColor = Color.parseColor("#FF26FF00");
    TextView sensorConnectionTV1;
    TextView sensorConnectionTV2;
    Button next;
    Button s1CalibrateBtn;
    Button s2CalibrateBtn;
    View view;
    Boolean magnetCalibrating = false;
    private final String TAG = "ConnectionFragment";


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
//        Button reset = view.findViewById(R.id.resetButton);
        Button connect = view.findViewById(R.id.connect);
        sensorConnectionTV1 = view.findViewById(R.id.SensorConnection1);
        sensorConnectionTV2 = view.findViewById(R.id.SensorConnection2);
        next = view.findViewById(R.id.next_btn);
        s1CalibrateBtn = view.findViewById(R.id.s1Calibrate);
        s2CalibrateBtn = view.findViewById(R.id.s2Calibrate);

        // Getting Metric From Therapy Description
        assert getArguments() != null;
        therapy = getArguments().getString("therapy");

        try {
            WitBluetoothManager.requestPermissions(getActivity());
            // 初始化蓝牙管理器，这里会申请蓝牙权限
            // Initialize the Bluetooth manager, here will apply for Bluetooth permissions
            WitBluetoothManager.initInstance(getActivity());
        }catch (Exception e){
            Log.e("", Objects.requireNonNull(e.getMessage()));
            e.printStackTrace();
        }

        // onClickListeners
        connect.setOnClickListener(view2 -> {
            Thread connectThread = new Thread(() -> {
                Log.i("ConnectionPage", "Connect thread started");

                startDiscovery();
            });
            connectThread.start();
        });

        next.setOnClickListener(view -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.therapyContainer, new SensorPlacementFragment());
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
//        String deviceData = getDeviceData(bwt901ble);
//        Log.d(TAG, "device data [ " + bwt901ble.getDeviceName() + "] = " + deviceData);
    }

    @Override
    public void onFoundBle(BluetoothBLE bluetoothBLE) {
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

        // Sensor Specific Changes
        if (bwt901bleList.size() == 1) {
            viewModel.setSensor1(bwt901ble);
            sensorConnectionTV1.setText("Connected");
            sensorConnectionTV1.setBackgroundColor(connectionColor);
            s1CalibrateBtn.setVisibility(View.VISIBLE);
        } else {
            viewModel.setSensor2(bwt901ble);
            sensorConnectionTV2.setText("Connected");
            sensorConnectionTV2.setBackgroundColor(connectionColor);
            s2CalibrateBtn.setVisibility(View.VISIBLE);
        }

    }

    // Required blank method
    @Override
    public void onFoundSPP(BluetoothSPP bluetoothSPP) {

    }

    public void s1CalibrationInflater() {
        // TODO: Add a retry calibration
        //Variable Declarations:
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
        s1Calibrate.setOnClickListener(view -> {
            s1CalibrationScreen.show();
        });

        accelCal.setOnClickListener(view -> {
            // 1. Complete calibration
            //  a. Make sensor call accelerometer calibration
            //   i. TODO: Depending on how we differentiate might alter how this is done
            //  b. Make button do a 3 second count down
            // 2. Make the accelLL and accelInstructions invisible or *gone*
            // 3. Make the magnetLL and magnetInstructions visible
            // 4. Create a boolean variable called accelCalStatus, initialize to false but set true here

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
            // TODO:
            // 1. Complete calibration
            //  a. Make button text say "Stop Calibrating"
            //  b. Make sensors call the magnetic field calibration
            //   i. TODO: Depending on how we differentiate might alter how this is done
            // 2. Make the magnetLL invisible or *gone*
            // 3. Change the magnetInstructions to say "Sensor Successfully Calibrated"
            // 4. Make the close button visible

            // First Press
            if (!magnetCalibrating) {
                magnetCalibrating = true;

                // Step 1a: Button Text
                magnetCal.setText("Stop Calibrating");

                // Step 1b: Magnetic Calibration
                magneticCalibrationStart(bwt901bleList.get(0));
            }
            // Second Press
            else {
                magnetCalibrating = false;
                magneticCalibrationEnd(bwt901bleList.get(0));

                // Step 2: Hiding UI Elements
                magnetLL.setVisibility(View.GONE);

                // Step 3: Alter magnetInstructions Text
                magnetInstructions.setText("Sensor Successfully Calibrated");
                magnetInstructions.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 35); // TODO: Test this size since I don't really know how this setTextSize works

                // Step 4: Make UI Elements Visible
                close.setVisibility(View.VISIBLE);
            }

        });

        escape.setOnClickListener(view -> {
            s1CalibrationScreen.dismiss();
        });

        close.setOnClickListener(view -> {
            s1CalibrationScreen.dismiss();
            sensorConnection1.setText("Connected\nCalibrated");
        });

    }

    public void s2CalibrationInflater() {
        // TODO: Add a retry calibration
        //Variable Declarations:
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
        s2Calibrate.setOnClickListener(view -> {
            s2CalibrationScreen.show();
        });

        accelCal.setOnClickListener(view -> {
            // 1. Complete calibration
            //  a. Make sensor call accelerometer calibration
            //   i. TODO: Depending on how we differentiate might alter how this is done
            //  b. Make button do a 3 second count down
            // 2. Make the accelLL and accelInstructions invisible or *gone*
            // 3. Make the magnetLL and magnetInstructions visible
            // 4. Create a boolean variable called accelCalStatus, initialize to false but set true here

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
            // TODO:
            // 1. Complete calibration
            //  a. Make button text say "Stop Calibrating"
            //  b. Make sensors call the magnetic field calibration
            //   i. TODO: Depending on how we differentiate might alter how this is done
            // 2. Make the magnetLL invisible or *gone*
            // 3. Change the magnetInstructions to say "Sensor Successfully Calibrated"
            // 4. Make the close button visible

            // First Press
            if (!magnetCalibrating) {
                magnetCalibrating = true;

                // Step 1a: Button Text
                magnetCal.setText("Stop Calibrating");

                // Step 1b: Magnetic Calibration
                magneticCalibrationStart(bwt901bleList.get(1));
            }
            // Second Press
            else {
                magnetCalibrating = false;
                magneticCalibrationEnd(bwt901bleList.get(1));

                // Step 2: Hiding UI Elements
                magnetLL.setVisibility(View.GONE);

                // Step 3: Alter magnetInstructions Text
                magnetInstructions.setText("Sensor Successfully Calibrated");
                magnetInstructions.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 35); // TODO: Test this size since I don't really know how this setTextSize works

                // Step 4: Make UI Elements Visible
                close.setVisibility(View.VISIBLE);
            }

        });

        escape.setOnClickListener(view -> {
            s2CalibrationScreen.dismiss();
        });

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

}