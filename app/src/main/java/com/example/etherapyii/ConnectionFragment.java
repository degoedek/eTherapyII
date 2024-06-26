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
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.DeviceInformation;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.data.Quaternion;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Macro;
import com.mbientlab.metawear.module.SensorFusionBosch;

import com.wit.witsdk.modular.sensor.device.exceptions.OpenDeviceException;
import com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble;
import com.wit.witsdk.modular.sensor.example.ble5.interfaces.IBwt901bleRecordObserver;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothBLE;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.BluetoothSPP;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.WitBluetoothManager;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.exceptions.BluetoothBLEException;
import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.interfaces.IBluetoothFoundObserver;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import bolts.CancellationTokenSource;
import bolts.Continuation;
import bolts.Task;

public class ConnectionFragment extends Fragment implements IBluetoothFoundObserver, IBwt901bleRecordObserver{
    String therapy;
    BtleService.LocalBinder serviceBinder;
    private MetaWearBoard board, board2;
    private List<Bwt901ble> bwt901bleList = new ArrayList<>();
    private boolean destroyed = true;
    private SharedViewModel viewModel;
    boolean s1Connected = false;
    boolean s2Connected = false;
    boolean s1Calibrated = false;
    boolean s2Calibrated = false;
    int connectionColor = Color.parseColor("#FF26FF00");
    TextView sensorConnectionTV1;
    TextView sensorConnectionTV2;
    Button next;
    Button s1CalibrateBtn;
    Button s2CalibrateBtn;
    View view;
    ObjectAnimator imageRotator;
    AnimatorSet animatorSet;
    int s1QuatListIndex = 0;
    int s2QuatListIndex = 0;
    Route s1Route, s2Route;


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
        Button reset = view.findViewById(R.id.resetButton);
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

        // Inflate the layout for this fragment
        return view;
    }

    public void startDiscovery() {
        // Turn off all device
        for (int i = 0; i < bwt901bleList.size(); i++) {
            Bwt901ble bwt901ble = bwt901bleList.get(i);
            bwt901ble.removeRecordObserver(this); // This might be broken
            bwt901ble.close();
        }

        // Erase all devices
        bwt901bleList.clear();

        // Start searching for bluetooth
        try {
            // get bluetooth manager
            WitBluetoothManager bluetoothManager = WitBluetoothManager.getInstance();
            // Monitor communication signals
            bluetoothManager.registerObserver(this); // This might be broken

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
            // TODO:
            // 1. Complete calibration
            //  a. Make button do a 3 second count down
            //  b. Make sensor call accelerometer calibration
            //   i. TODO: Depending on how we differentiate might alter how this is done
            // 2. Make the accelLL and accelInstructions invisible or *gone*
            // 3. Make the magnetLL and magnetInstructions visible
            // 4. Create a boolean variable called accelCalStatus, initialize to false but set true here
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
        //Variable Declarations:
        AlertDialog s2CalibrationScreen;
        Button s2Calibrate = view.findViewById(R.id.s2Calibrate);

        AlertDialog.Builder builder1 = new AlertDialog.Builder(requireActivity());
        builder1.setCancelable(false);
        View view3 = getLayoutInflater().inflate(R.layout.popup_s2_calibration_screen, null);

//        TextView s2AccelStatus = view3.findViewById(R.id.s2AccelStatus);
//        TextView s2GyroStatus = view3.findViewById(R.id.s2GyroStatus);
//        TextView s2MagnetStatus = view3.findViewById(R.id.s2MagnetStatus);
//        TextView sensorConnection2 = view.findViewById(R.id.SensorConnection2);
//
//        ImageView s2AccelImage = view3.findViewById(R.id.s2AccelImage);
//        ImageView s2GyroImage = view3.findViewById(R.id.s2GyroImage);
//        ImageView s2MagnetImage = view3.findViewById(R.id.s2MagnetImage);
//
//        Button close = view3.findViewById(R.id.close);
//        ImageButton escape = view3.findViewById(R.id.emergencyExit);
//
//
//        builder1.setView(view3);
//        s2CalibrationScreen = builder1.create();
//        s2Calibrate.setOnClickListener(view -> {
//            s2CalibrationScreen.show();
//            Led led2;
//            if ((led2 = board2.getModule(Led.class)) != null) {
//                led2.stop(true);
//                led2.editPattern(Led.Color.BLUE, Led.PatternPreset.SOLID)
//                        .commit();
//                led2.play();
//            }
//
//            if (!s2Calibrated) {
//                s2GyroImage.setVisibility(View.VISIBLE);
//                s2GyroStatus.setVisibility(View.VISIBLE);
//                s2MagnetImage.setVisibility(View.VISIBLE);
//                s2MagnetStatus.setVisibility(View.VISIBLE);
//                s2Calibrate.setVisibility(View.VISIBLE);
//                close.setVisibility(View.INVISIBLE);
//            }
//
//            final SensorFusionBosch sensorFusion2 = board2.getModule(SensorFusionBosch.class);
//            final CancellationTokenSource cts2 = new CancellationTokenSource();
//
//            // use ndof mode with +/-16g acc range and 2000dps gyro range
//            sensorFusion2.configure()
//                    .mode(SensorFusionBosch.Mode.NDOF)
//                    .accRange(SensorFusionBosch.AccRange.AR_16G)
//                    .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
//                    .commit();
//
//            sensorFusion2.calibrate(cts2.getToken(), state -> {
//                String calibrationState2 = state.toString();
//
//                requireActivity().runOnUiThread(() -> {
//                    int nextStateLoc = 34;          //This means the 34th char of calibrationState is the first letter of the accelerometer state
//                    //HOW TO CALIBRATE: GYRO - LAY STILL ON TABLE   ACCEL - TURN 45 DEGREES ON 1 AXIS REPEATEDLY   MAGNET - CREATE ONE MOTION TO DO REPEATEDLY
//                    //Lengths: H = 13, U = 10, M = 15, L = 12
//                    //More lengths: Accelerometer first letter = 35, gyroscope is an additional 13 chars after accelState, magnetometer is an additional 16 or 17 chars after gyroState
//                    char accelLetter = calibrationState2.charAt(nextStateLoc);
////                System.out.println();
////                System.out.println(accelLetter + " Accelletter!!!!!!!!!!!!!");
//                    switch (accelLetter) {
//                        case 'H':
//                            s2AccelStatus.setText("Accelerometer: High Accuracy");
//                            s2AccelImage.setImageResource(R.drawable.connection_status_high_accuracy);
//                            nextStateLoc += 13 + 13;
//                            break;
//                        case 'M':
//                            s2AccelStatus.setText("Accelerometer: Medium Accuracy");
//                            s2AccelImage.setImageResource(R.drawable.connection_status_medium_accuracy);
//                            nextStateLoc += 15 + 13;
//                            break;
//                        case 'L':
//                            s2AccelStatus.setText("Accelerometer: Low Accuracy");
//                            s2AccelImage.setImageResource(R.drawable.connection_status_low_accuracy);
//                            nextStateLoc += 12 + 13;
//                            break;
//                        case 'U':
//                            s2AccelStatus.setText("Accelerometer: Unreliable");
//                            s2AccelImage.setImageResource(R.drawable.connection_status_unreliable);
//                            nextStateLoc += 10 + 13;
//                            break;
//                    }
//                    char gyroLetter = calibrationState2.charAt(nextStateLoc);
////                System.out.println(gyroLetter + " Gyroletter!!!!!!!!!!!!!");
//                    switch (gyroLetter) {
//                        case 'H':
//                            s2GyroStatus.setText("Gyroscope: High Accuracy");
//                            s2GyroImage.setImageResource(R.drawable.connection_status_high_accuracy);
//                            nextStateLoc += 13 + 16;
//                            break;
//                        case 'M':
//                            s2GyroStatus.setText("Gyroscope: Medium Accuracy");
//                            s2GyroImage.setImageResource(R.drawable.connection_status_medium_accuracy);
//                            nextStateLoc += 15 + 16;
//                            break;
//                        case 'L':
//                            s2GyroStatus.setText("Gyroscope: Low Accuracy");
//                            s2GyroImage.setImageResource(R.drawable.connection_status_low_accuracy);
//                            nextStateLoc += 12 + 16;
//                            break;
//                        case 'U':
//                            s2GyroStatus.setText("Gyroscope: Unreliable");
//                            s2GyroImage.setImageResource(R.drawable.connection_status_unreliable);
//                            nextStateLoc += 10 + 16;
//                            break;
//                    }
//                    char magnetLetter = calibrationState2.charAt(nextStateLoc);
////                System.out.println(magnetLetter + " Magnetletter!!!!!!!!!!!!!");
//                    switch (magnetLetter) {
//                        case 'H':
//                            s2MagnetStatus.setText("Magnetometer: High Accuracy");
//                            s2MagnetImage.setImageResource(R.drawable.connection_status_high_accuracy);
//                            break;
//                        case 'M':
//                            s2MagnetStatus.setText("Magnetometer: Medium Accuracy");
//                            s2MagnetImage.setImageResource(R.drawable.connection_status_medium_accuracy);
//                            break;
//                        case 'L':
//                            s2MagnetStatus.setText("Magnetometer: Low Accuracy");
//                            s2MagnetImage.setImageResource(R.drawable.connection_status_low_accuracy);
//                            break;
//                        case 'U':
//                            s2MagnetStatus.setText("Magnetometer: Unreliable");
//                            s2MagnetImage.setImageResource(R.drawable.connection_status_unreliable);
//                            break;
//                    }
//
//                    //Condensing calibration UI
//                    if (accelLetter == 'H' && gyroLetter == 'H' && magnetLetter == 'H') {
//                        s2AccelImage.setImageResource(R.drawable.connection_status_high_accuracy);
//                        s2AccelStatus.setText("Sensor Calibrated");
//                        s2Calibrated = true;
////                    sendBoards();
//
//                        Led led;
//                        if ((led = board2.getModule(Led.class)) != null) {
//                            led.stop(true);
//                        }
//
//                        s2GyroImage.setVisibility(View.GONE);
//                        s2GyroStatus.setVisibility(View.GONE);
//                        s2MagnetImage.setVisibility(View.GONE);
//                        s2MagnetStatus.setVisibility(View.GONE);
//                        s2Calibrate.setVisibility(View.GONE);
//                        sensorConnection2.setText("Connected Calibrated");
//                        close.setVisibility(View.VISIBLE);
//
//                        if (s1Calibrated && s2Calibrated) next.setVisibility(View.VISIBLE);
//                    }
//                });
//
//                Log.i("MainActivity", "Sensor 2 - " + calibrationState2);
//            }).onSuccess(task -> {
//                // calibration data is reloaded every time mode changes
//                sensorFusion2.writeCalibrationData(task.getResult());
//                return null;
//            });
//
//            // stream quaternion values from the board
//            sensorFusion2.quaternion().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
//                    }))
//                    .continueWith((Continuation<Route, Void>) task -> {
//                        sensorFusion2.quaternion().start();
//                        sensorFusion2.start();
//                        return null;
//                    });
//        });
//
//        escape.setOnClickListener(view -> {
//            s2CalibrationScreen.dismiss();
//            Led led2;
//            if ((led2 = board2.getModule(Led.class)) != null) {
//                led2.stop(true);
//            }
//        });
//
//        close.setOnClickListener(view -> {
//            s2CalibrationScreen.dismiss();
//            Led led2;
//            if ((led2 = board2.getModule(Led.class)) != null) {
//                led2.stop(true);
//            }
//        });
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