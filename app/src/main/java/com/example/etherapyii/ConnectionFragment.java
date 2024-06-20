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

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import java.util.Objects;

import bolts.CancellationTokenSource;
import bolts.Continuation;
import bolts.Task;

public class ConnectionFragment extends Fragment implements ServiceConnection {
    String therapy;
    BtleService.LocalBinder serviceBinder;
    private MetaWearBoard board, board2;
    boolean s1Connected = false;
    boolean s2Connected = false;
    boolean s1Calibrated = false;
    boolean s2Calibrated = false;
    int connectionColor = Color.parseColor("#FF26FF00");
    Button next;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_connection, container, false);

        //Variable Declarations
        BluetoothManager bluetoothManager = requireActivity().getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();


        //Buttons
        Button reset = view.findViewById(R.id.resetButton);
        Button connect = view.findViewById(R.id.connect);
        next = view.findViewById(R.id.next_btn);

        // Getting Metric From Therapy Description
        assert getArguments() != null;
        therapy = getArguments().getString("therapy");

        //Bind the service when the activity is created
        requireActivity().getApplicationContext().bindService(new Intent(getActivity(), BtleService.class), this, Context.BIND_AUTO_CREATE);

        //onClickListeners
        connect.setOnClickListener(view2 -> {
            //Bind the service when the activity is created
            requireActivity().getApplicationContext().bindService(new Intent(getActivity(), BtleService.class), this, Context.BIND_AUTO_CREATE);

            Thread connectThread = new Thread(() -> {
                Log.i("ConnectionPage", "Connect thread started");
                requireActivity().getApplicationContext().bindService(new Intent(getActivity(), BtleService.class), ConnectionFragment.this, Context.BIND_AUTO_CREATE);

                if (!s1Connected) {
                    connectBoard();
                }
                if (!s2Connected) {
                    connectBoard2();
                }
            });
            connectThread.start();
        });


        reset.setOnClickListener(view2 -> {
            if (!s1Connected && !s2Connected) {
                Toast.makeText(requireActivity().getApplicationContext(), "No Sensors Connected", Toast.LENGTH_SHORT).show();
            } else {
                // Reset Button Invisible
                connect.setVisibility(View.VISIBLE);

                // Reset Sensors
                resetSensor(board);
                resetSensor(board2);

                // Disconnect the boards
                if (board != null) board.disconnectAsync();
                if (board2 != null) board2.disconnectAsync();


                s1Connected = false;
                s2Connected = false;
                s1Calibrated = false;
                s2Calibrated = false;

                resetSensorUI();

                // TODO: Restart the activity
                restartCurrentFragment();
            }
        });

        //Calibration Listeners:
        s1CalibrationInflater();
        s2CalibrationInflater();
        checkSensorFusion();


        // Inflate the layout for this fragment
        return view;
    }

    public void restartCurrentFragment() {
        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
        Fragment currentFragment = getParentFragmentManager().findFragmentById(R.id.therapyContainer);

        if (currentFragment != null) {
            fragmentTransaction.detach(currentFragment);
            fragmentTransaction.attach(currentFragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Log.d(TAG, "Service Connected!");
        //Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;
        retrieveBoard();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    public Task<Void> resetSensor(MetaWearBoard b) {
        if (b == null) return null;

        // Resetting Sensor
        b.getModule(Macro.class).eraseAll();
        b.getModule(Debug.class).resetAfterGc();
        return b.getModule(Debug.class).disconnectAsync();
    }



    public void retrieveBoard() {
        // TODO: Convert this to a bluetooth scan rather than hard-coding MAC Addresses
        final BluetoothManager btManager = (BluetoothManager) requireActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        String macAddress1 = "ED:5B:0A:50:14:59";
        String macAddress2 = "FE:C2:4B:10:FB:D5";
        BluetoothDevice sensor = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress1);
        BluetoothDevice sensor2 = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress2);

        // Create a MetaWear board object for the Bluetooth Device
        board = serviceBinder.getMetaWearBoard(sensor);
        board2 = serviceBinder.getMetaWearBoard(sensor2);
    }

    public void connectBoard() {

        board.connectAsync().continueWith(new Continuation<Void, Void>() {
            final TextView sensorConnection1 = view.findViewById(R.id.SensorConnection1);
            final Button connect = view.findViewById(R.id.connect);
            final Button s1Calibrate = view.findViewById(R.id.s1Calibrate);


            @Override
            public Void then(Task<Void> task) {
                if (task.isFaulted()) {
                    Log.i("MainActivity", "Board 1: Failed to connect");
                    //Toast Message
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireActivity().getApplicationContext(), "Sensor 1 - Failed to Connect", Toast.LENGTH_SHORT).show());
                } else {
                    sensorConnection1.setText("Connected");
                    sensorConnection1.setBackgroundColor(connectionColor);
                    s1Connected = true;

                    Log.i("MainActivity", "Board 1: Connected");

                    checkBattery(board, 1);

                    Led led;
                    if ((led = board.getModule(Led.class)) != null) {
                        led.editPattern(Led.Color.GREEN, Led.PatternPreset.BLINK)
                                .repeatCount((byte) 3)
                                .commit();
                        led.play();
                    }
                    if (board.getModule(Haptic.class) != null) {
                        board.getModule(Haptic.class).startMotor(25.F, (short) 750);
                    }
                    requireActivity().runOnUiThread(() -> {
                        s1Calibrate.setVisibility(View.VISIBLE);

                        if (s1Connected && s2Connected) {
                            connect.setVisibility(View.GONE);
                            Log.i("MainActivity", "Connect Button: Gone");
                        }
                    });
                    board.readDeviceInformationAsync().continueWith((Continuation<DeviceInformation, Void>) task1 -> {
                        Log.i("MainActivity", "Device Information: " + task1.getResult());
                        return null;
                    });
                }
                return null;
            }
        });

    }

    public void connectBoard2() {
        board2.connectAsync().continueWith(new Continuation<Void, Void>() {
            TextView sensorConnection2 = view.findViewById(R.id.SensorConnection2);
            Button connect = view.findViewById(R.id.connect);
            Button s2Calibrate = view.findViewById(R.id.s2Calibrate);


            @Override
            public Void then(Task<Void> task) {
                if (task.isFaulted()) {
                    Log.i("MainActivity", "Board 2: Failed to connect");
                    //Toast Message
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireActivity().getApplicationContext(), "Sensor 2 - Failed to Connect", Toast.LENGTH_SHORT).show());
                } else {
                    sensorConnection2.setText("Connected");     //Connection color: FF26FF00
                    sensorConnection2.setBackgroundColor(connectionColor);
                    s2Connected = true;

                    Log.i("MainActivity", "Board 2: Connected");

                    checkBattery(board2, 2);

                    Led led;
                    if ((led = board2.getModule(Led.class)) != null) {
                        led.editPattern(Led.Color.GREEN, Led.PatternPreset.BLINK)
                                .repeatCount((byte) 3)
                                .commit();
                        led.play();
                    }
                    if (board2.getModule(Haptic.class) != null) {
                        board2.getModule(Haptic.class).startMotor(30.F, (short) 750);
                    }
                    requireActivity().runOnUiThread(() -> {
                        s2Calibrate.setVisibility(View.VISIBLE);
                        if (s1Connected && s2Connected) {
                            connect.setVisibility(View.GONE);
                            Log.i("MainActivity", "Connect Button: Gone");
                        }

                    });
                    board2.readDeviceInformationAsync().continueWith((Continuation<DeviceInformation, Void>) task1 -> {
                        Log.i("MainActivity", "Device Information: " + task1.getResult());
                        return null;
                    });
                }
                return null;
            }
        });
    }

    /** board - the MetaWearBoard that will have it's battery percent checked
     * sensorNum - *currently* either a 1 or a 2, helps clarify which sensor display to change
     */
    public void checkBattery(@NonNull MetaWearBoard boardId, int sensorNum) {
//        Log.i("MainActivity", "checkBattery Method Called");
        TextView s1BP = view.findViewById(R.id.s1batteryPercent);        //Battery Percentages
        TextView s2BP = view.findViewById(R.id.s2batteryPercent);

        ImageView s1B = view.findViewById(R.id.s1battery);               //Battery Icons
        ImageView s2B = view.findViewById(R.id.s2battery);

        boardId.readBatteryLevelAsync().continueWith(task -> {
            int batteryPercentage = task.getResult();

            Log.i("MainActivity", "Sensor " + sensorNum + " Battery Level: " + batteryPercentage);

            //Updating UI
            requireActivity().runOnUiThread(() -> {
                if (sensorNum == 1) {
                    String stringS1BP = "      " + batteryPercentage + "%";
                    Log.i("MainActivity", "Sensor 1 Case Entered");
                    s1BP.setText(stringS1BP);

                    //Updating Imaging
                    if (75 < batteryPercentage && batteryPercentage <= 100) {
                        s1B.setImageResource(R.drawable.battery_full);
                    } else if (50 < batteryPercentage && batteryPercentage <= 75) {
                        s1B.setImageResource(R.drawable.battery_medium);
                    } else if (25 < batteryPercentage && batteryPercentage <= 50) {
                        s1B.setImageResource(R.drawable.battery_low);
                    } else if (0 <= batteryPercentage && batteryPercentage <= 25) {
                        s1B.setImageResource(R.drawable.battery_dead);
                    }

                } else if (sensorNum == 2) {
                    String stringS2BP = "      " + batteryPercentage + "%";
                    s2BP.setText(stringS2BP);

                    //Updating Imaging
                    if (75 < batteryPercentage && batteryPercentage <= 100) {
                        s2B.setImageResource(R.drawable.battery_full);
                    } else if (50 < batteryPercentage && batteryPercentage <= 75) {
                        s2B.setImageResource(R.drawable.battery_medium);
                    } else if (25 < batteryPercentage && batteryPercentage <= 50) {
                        s2B.setImageResource(R.drawable.battery_low);
                    } else if (0 <= batteryPercentage && batteryPercentage <= 25) {
                        s2B.setImageResource(R.drawable.battery_dead);
                    }
                } else Log.wtf("MainActivity", "Wrong sensor number!!!!!!!!!!!!!!!!!!!!!!!!!");
            });

            return null;
        });
    }

    public void s1CalibrationInflater() {
        //Variable Declarations:
        AlertDialog s1CalibrationScreen;
        Button s1Calibrate = view.findViewById(R.id.s1Calibrate);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setCancelable(false);
        View view2 = getLayoutInflater().inflate(R.layout.popup_s1_calibration_screen, null);

        TextView s1AccelStatus = view2.findViewById(R.id.s1AccelStatus);
        TextView s1GyroStatus = view2.findViewById(R.id.s1GyroStatus);
        TextView s1MagnetStatus = view2.findViewById(R.id.s1MagnetStatus);
        TextView sensorConnection1 = view.findViewById(R.id.SensorConnection1);

        ImageView s1AccelImage = view2.findViewById(R.id.s1AccelImage);
        ImageView s1GyroImage = view2.findViewById(R.id.s1GyroImage);
        ImageView s1MagnetImage = view2.findViewById(R.id.s1MagnetImage);

        ImageButton escape = view2.findViewById(R.id.emergencyExit);
        Button close = view2.findViewById(R.id.close);

        builder.setView(view2);
        s1CalibrationScreen = builder.create();
        s1Calibrate.setOnClickListener(view -> {
            s1CalibrationScreen.show();
            Led led1;
            if ((led1 = board.getModule(Led.class)) != null) {
                led1.stop(true);
                led1.editPattern(Led.Color.RED, Led.PatternPreset.SOLID)
                        .commit();
                led1.play();
            }

            if (!s1Calibrated) {
                s1GyroImage.setVisibility(View.VISIBLE);
                s1GyroStatus.setVisibility(View.VISIBLE);
                s1MagnetImage.setVisibility(View.VISIBLE);
                s1MagnetStatus.setVisibility(View.VISIBLE);
                s1Calibrate.setVisibility(View.VISIBLE);
                close.setVisibility(View.INVISIBLE);
            }

            final SensorFusionBosch sensorFusion = board.getModule(SensorFusionBosch.class);
            final CancellationTokenSource cts = new CancellationTokenSource();


            // use ndof mode with +/-16g acc range and 2000dps gyro range
            sensorFusion.configure()
                    .mode(SensorFusionBosch.Mode.NDOF)
                    .accRange(SensorFusionBosch.AccRange.AR_16G)
                    .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                    .commit();

            sensorFusion.calibrate(cts.getToken(), state -> {
                String calibrationState = state.toString();

                requireActivity().runOnUiThread(() -> {
                    int nextStateLoc = 34;          //This means the 34th char of calibrationState is the first letter of the accelerometer state
                    //HOW TO CALIBRATE: GYRO - LAY STILL ON TABLE   ACCEL - TURN 45 DEGREES ON 1 AXIS REPEATEDLY   MAGNET - CREATE ONE MOTION TO DO REPEATEDLY
                    //Lengths: H = 13, U = 10, M = 15, L = 12
                    //More lengths: Accelerometer first letter = 35, gyroscope is an additional 13 chars after accelState, magnetometer is an additional 16 or 17 chars after gyroState
                    char accelLetter = calibrationState.charAt(nextStateLoc);

                    switch (accelLetter) {
                        case 'H':
                            s1AccelStatus.setText("Accelerometer: High Accuracy");
                            s1AccelImage.setImageResource(R.drawable.connection_status_high_accuracy);
                            nextStateLoc += 13 + 13;
                            break;
                        case 'M':
                            s1AccelStatus.setText("Accelerometer: Medium Accuracy");
                            s1AccelImage.setImageResource(R.drawable.connection_status_medium_accuracy);
                            nextStateLoc += 15 + 13;
                            break;
                        case 'L':
                            s1AccelStatus.setText("Accelerometer: Low Accuracy");
                            s1AccelImage.setImageResource(R.drawable.connection_status_low_accuracy);
                            nextStateLoc += 12 + 13;
                            break;
                        case 'U':
                            s1AccelStatus.setText("Accelerometer: Unreliable");
                            s1AccelImage.setImageResource(R.drawable.connection_status_unreliable);
                            nextStateLoc += 10 + 13;
                            break;
                    }
                    char gyroLetter = calibrationState.charAt(nextStateLoc);

                    switch (gyroLetter) {
                        case 'H':
                            s1GyroStatus.setText("Gyroscope: High Accuracy");
                            s1GyroImage.setImageResource(R.drawable.connection_status_high_accuracy);
                            nextStateLoc += 13 + 16;
                            break;
                        case 'M':
                            s1GyroStatus.setText("Gyroscope: Medium Accuracy");
                            s1GyroImage.setImageResource(R.drawable.connection_status_medium_accuracy);
                            nextStateLoc += 15 + 16;
                            break;
                        case 'L':
                            s1GyroStatus.setText("Gyroscope: Low Accuracy");
                            s1GyroImage.setImageResource(R.drawable.connection_status_low_accuracy);
                            nextStateLoc += 12 + 16;
                            break;
                        case 'U':
                            s1GyroStatus.setText("Gyroscope: Unreliable");
                            s1GyroImage.setImageResource(R.drawable.connection_status_unreliable);
                            nextStateLoc += 10 + 16;
                            break;
                    }
                    char magnetLetter = calibrationState.charAt(nextStateLoc);

                    switch (magnetLetter) {
                        case 'H':
                            s1MagnetStatus.setText("Magnetometer: High Accuracy");
                            s1MagnetImage.setImageResource(R.drawable.connection_status_high_accuracy);
                            break;
                        case 'M':
                            s1MagnetStatus.setText("Magnetometer: Medium Accuracy");
                            s1MagnetImage.setImageResource(R.drawable.connection_status_medium_accuracy);
                            break;
                        case 'L':
                            s1MagnetStatus.setText("Magnetometer: Low Accuracy");
                            s1MagnetImage.setImageResource(R.drawable.connection_status_low_accuracy);
                            break;
                        case 'U':
                            s1MagnetStatus.setText("Magnetometer: Unreliable");
                            s1MagnetImage.setImageResource(R.drawable.connection_status_unreliable);
                            break;
                    }

                    //Condensing calibration UI
                    if (accelLetter == 'H' && gyroLetter == 'H' && magnetLetter == 'H') {
                        s1AccelImage.setImageResource(R.drawable.connection_status_high_accuracy);
                        s1AccelStatus.setText("Sensor Calibrated");
                        s1Calibrated = true;
//                    sendBoards();

                        Led led;
                        if ((led = board.getModule(Led.class)) != null) {
                            led.stop(true);
                        }

                        s1GyroImage.setVisibility(View.GONE);
                        s1GyroStatus.setVisibility(View.GONE);
                        s1MagnetImage.setVisibility(View.GONE);
                        s1MagnetStatus.setVisibility(View.GONE);
                        s1Calibrate.setVisibility(View.GONE);
                        sensorConnection1.setText("Connected Calibrated");
                        close.setVisibility(View.VISIBLE);

                        if (s1Calibrated && s2Calibrated) next.setVisibility(View.VISIBLE);
                    }

                });

                Log.i("MainActivity", "Sensor 1 - " + calibrationState);
            }).onSuccess(task -> {
                // calibration data is reloaded every time mode changes
                sensorFusion.writeCalibrationData(task.getResult());
                return null;
            });

            // stream quaternion values from the board
            sensorFusion.quaternion().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
                    }))
                    .continueWith((Continuation<Route, Void>) task -> {
                        sensorFusion.quaternion().start();
                        sensorFusion.start();
                        return null;
                    });
            Log.i("ConnectionPage", "s1Calibrate Thread Started");
        });

        escape.setOnClickListener(view -> {
            s1CalibrationScreen.dismiss();
            Led led;
            if ((led = board.getModule(Led.class)) != null) {
                led.stop(true);
            }
        });

        close.setOnClickListener(view -> {
            s1CalibrationScreen.dismiss();
            Led led;
            if ((led = board.getModule(Led.class)) != null) {
                led.stop(true);
            }
        });

    }

    public void s2CalibrationInflater() {
        //Variable Declarations:
        AlertDialog s2CalibrationScreen;
        Button s2Calibrate = view.findViewById(R.id.s2Calibrate);

        AlertDialog.Builder builder1 = new AlertDialog.Builder(requireActivity());
        builder1.setCancelable(false);
        View view3 = getLayoutInflater().inflate(R.layout.popup_s2_calibration_screen, null);

        TextView s2AccelStatus = view3.findViewById(R.id.s2AccelStatus);
        TextView s2GyroStatus = view3.findViewById(R.id.s2GyroStatus);
        TextView s2MagnetStatus = view3.findViewById(R.id.s2MagnetStatus);
        TextView sensorConnection2 = view.findViewById(R.id.SensorConnection2);

        ImageView s2AccelImage = view3.findViewById(R.id.s2AccelImage);
        ImageView s2GyroImage = view3.findViewById(R.id.s2GyroImage);
        ImageView s2MagnetImage = view3.findViewById(R.id.s2MagnetImage);

        Button close = view3.findViewById(R.id.close);
        ImageButton escape = view3.findViewById(R.id.emergencyExit);


        builder1.setView(view3);
        s2CalibrationScreen = builder1.create();
        s2Calibrate.setOnClickListener(view -> {
            s2CalibrationScreen.show();
            Led led2;
            if ((led2 = board2.getModule(Led.class)) != null) {
                led2.stop(true);
                led2.editPattern(Led.Color.BLUE, Led.PatternPreset.SOLID)
                        .commit();
                led2.play();
            }

            if (!s2Calibrated) {
                s2GyroImage.setVisibility(View.VISIBLE);
                s2GyroStatus.setVisibility(View.VISIBLE);
                s2MagnetImage.setVisibility(View.VISIBLE);
                s2MagnetStatus.setVisibility(View.VISIBLE);
                s2Calibrate.setVisibility(View.VISIBLE);
                close.setVisibility(View.INVISIBLE);
            }

            final SensorFusionBosch sensorFusion2 = board2.getModule(SensorFusionBosch.class);
            final CancellationTokenSource cts2 = new CancellationTokenSource();

            // use ndof mode with +/-16g acc range and 2000dps gyro range
            sensorFusion2.configure()
                    .mode(SensorFusionBosch.Mode.NDOF)
                    .accRange(SensorFusionBosch.AccRange.AR_16G)
                    .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                    .commit();

            sensorFusion2.calibrate(cts2.getToken(), state -> {
                String calibrationState2 = state.toString();

                requireActivity().runOnUiThread(() -> {
                    int nextStateLoc = 34;          //This means the 34th char of calibrationState is the first letter of the accelerometer state
                    //HOW TO CALIBRATE: GYRO - LAY STILL ON TABLE   ACCEL - TURN 45 DEGREES ON 1 AXIS REPEATEDLY   MAGNET - CREATE ONE MOTION TO DO REPEATEDLY
                    //Lengths: H = 13, U = 10, M = 15, L = 12
                    //More lengths: Accelerometer first letter = 35, gyroscope is an additional 13 chars after accelState, magnetometer is an additional 16 or 17 chars after gyroState
                    char accelLetter = calibrationState2.charAt(nextStateLoc);
//                System.out.println();
//                System.out.println(accelLetter + " Accelletter!!!!!!!!!!!!!");
                    switch (accelLetter) {
                        case 'H':
                            s2AccelStatus.setText("Accelerometer: High Accuracy");
                            s2AccelImage.setImageResource(R.drawable.connection_status_high_accuracy);
                            nextStateLoc += 13 + 13;
                            break;
                        case 'M':
                            s2AccelStatus.setText("Accelerometer: Medium Accuracy");
                            s2AccelImage.setImageResource(R.drawable.connection_status_medium_accuracy);
                            nextStateLoc += 15 + 13;
                            break;
                        case 'L':
                            s2AccelStatus.setText("Accelerometer: Low Accuracy");
                            s2AccelImage.setImageResource(R.drawable.connection_status_low_accuracy);
                            nextStateLoc += 12 + 13;
                            break;
                        case 'U':
                            s2AccelStatus.setText("Accelerometer: Unreliable");
                            s2AccelImage.setImageResource(R.drawable.connection_status_unreliable);
                            nextStateLoc += 10 + 13;
                            break;
                    }
                    char gyroLetter = calibrationState2.charAt(nextStateLoc);
//                System.out.println(gyroLetter + " Gyroletter!!!!!!!!!!!!!");
                    switch (gyroLetter) {
                        case 'H':
                            s2GyroStatus.setText("Gyroscope: High Accuracy");
                            s2GyroImage.setImageResource(R.drawable.connection_status_high_accuracy);
                            nextStateLoc += 13 + 16;
                            break;
                        case 'M':
                            s2GyroStatus.setText("Gyroscope: Medium Accuracy");
                            s2GyroImage.setImageResource(R.drawable.connection_status_medium_accuracy);
                            nextStateLoc += 15 + 16;
                            break;
                        case 'L':
                            s2GyroStatus.setText("Gyroscope: Low Accuracy");
                            s2GyroImage.setImageResource(R.drawable.connection_status_low_accuracy);
                            nextStateLoc += 12 + 16;
                            break;
                        case 'U':
                            s2GyroStatus.setText("Gyroscope: Unreliable");
                            s2GyroImage.setImageResource(R.drawable.connection_status_unreliable);
                            nextStateLoc += 10 + 16;
                            break;
                    }
                    char magnetLetter = calibrationState2.charAt(nextStateLoc);
//                System.out.println(magnetLetter + " Magnetletter!!!!!!!!!!!!!");
                    switch (magnetLetter) {
                        case 'H':
                            s2MagnetStatus.setText("Magnetometer: High Accuracy");
                            s2MagnetImage.setImageResource(R.drawable.connection_status_high_accuracy);
                            break;
                        case 'M':
                            s2MagnetStatus.setText("Magnetometer: Medium Accuracy");
                            s2MagnetImage.setImageResource(R.drawable.connection_status_medium_accuracy);
                            break;
                        case 'L':
                            s2MagnetStatus.setText("Magnetometer: Low Accuracy");
                            s2MagnetImage.setImageResource(R.drawable.connection_status_low_accuracy);
                            break;
                        case 'U':
                            s2MagnetStatus.setText("Magnetometer: Unreliable");
                            s2MagnetImage.setImageResource(R.drawable.connection_status_unreliable);
                            break;
                    }

                    //Condensing calibration UI
                    if (accelLetter == 'H' && gyroLetter == 'H' && magnetLetter == 'H') {
                        s2AccelImage.setImageResource(R.drawable.connection_status_high_accuracy);
                        s2AccelStatus.setText("Sensor Calibrated");
                        s2Calibrated = true;
//                    sendBoards();

                        Led led;
                        if ((led = board2.getModule(Led.class)) != null) {
                            led.stop(true);
                        }

                        s2GyroImage.setVisibility(View.GONE);
                        s2GyroStatus.setVisibility(View.GONE);
                        s2MagnetImage.setVisibility(View.GONE);
                        s2MagnetStatus.setVisibility(View.GONE);
                        s2Calibrate.setVisibility(View.GONE);
                        sensorConnection2.setText("Connected Calibrated");
                        close.setVisibility(View.VISIBLE);

                        if (s1Calibrated && s2Calibrated) next.setVisibility(View.VISIBLE);
                    }
                });

                Log.i("MainActivity", "Sensor 2 - " + calibrationState2);
            }).onSuccess(task -> {
                // calibration data is reloaded every time mode changes
                sensorFusion2.writeCalibrationData(task.getResult());
                return null;
            });

            // stream quaternion values from the board
            sensorFusion2.quaternion().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
                    }))
                    .continueWith((Continuation<Route, Void>) task -> {
                        sensorFusion2.quaternion().start();
                        sensorFusion2.start();
                        return null;
                    });
        });

        escape.setOnClickListener(view -> {
            s2CalibrationScreen.dismiss();
            Led led2;
            if ((led2 = board2.getModule(Led.class)) != null) {
                led2.stop(true);
            }
        });

        close.setOnClickListener(view -> {
            s2CalibrationScreen.dismiss();
            Led led2;
            if ((led2 = board2.getModule(Led.class)) != null) {
                led2.stop(true);
            }
        });
    }

    public void checkSensorFusion() {
        // Variable Declarations
        AlertDialog loadingDialog;
        Button next = view.findViewById(R.id.next_btn);


        AlertDialog.Builder builder1 = new AlertDialog.Builder(requireActivity());
        builder1.setCancelable(false);
        View view3 = getLayoutInflater().inflate(R.layout.popup_sensor_fusion_loading, null);

        ImageView loadingImage = view3.findViewById(R.id.loadingImage);
        TextView loadingText = view3.findViewById(R.id.loadingText);
        TextView infoDisplay = view3.findViewById(R.id.infoDisplay);;

        builder1.setView(view3);
        loadingDialog = builder1.create();
        next.setOnClickListener(view -> {
            loadingDialog.show();
            // More variable declarations
            SensorFusionBosch sf1;
            SensorFusionBosch sf2;
            final int QuatListSize = 10;
            Quaternion[] s1QuatList = new Quaternion[QuatListSize];
            Quaternion[] s2QuatList = new Quaternion[QuatListSize];

            // Define the rotation animation
            imageRotator = ObjectAnimator.ofFloat(loadingImage, "rotation", 0f, 360f);
            imageRotator.setDuration(2000);
            imageRotator.setRepeatCount(ValueAnimator.INFINITE);

            // Create AnimatorSet and start animation
            animatorSet = new AnimatorSet();
            animatorSet.playTogether(imageRotator);
            animatorSet.start();

            // Begin Sensor Fusion
            if (board == null || board2 == null) {
                // Commented out for testing purposes
                // TODO: Add some form of exit outside of testing purposes
            } else {
                sf1 = board.getModule(SensorFusionBosch.class);
                sf2 = board2.getModule(SensorFusionBosch.class);

                sf1.configure()
                        .mode(SensorFusionBosch.Mode.NDOF)
                        .accRange(SensorFusionBosch.AccRange.AR_16G)
                        .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                        .commit();
                sf2.configure()
                        .mode(SensorFusionBosch.Mode.NDOF)
                        .accRange(SensorFusionBosch.AccRange.AR_16G)
                        .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                        .commit();

                sf1.quaternion().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
                    Log.i("SF Check", "Sensor 2: " + data.value(Quaternion.class).toString());
                    if (s1QuatListIndex < QuatListSize) {
                        s1QuatList[s1QuatListIndex] = data.value(Quaternion.class);
                        Log.i("SF Check", "Sensor 1: " + s1QuatList[s1QuatListIndex].toString());
                        s1QuatListIndex += 1;
                    } else {
                        stopSensorFusion(sf1, 1);
                    }
                }))
                .continueWith((Continuation<Route, Void>) task -> {
                    s1Route = task.getResult();
                    sf1.quaternion().start();
                    sf1.start();

                    return null;
                });

                sf2.quaternion().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
                    Log.i("SF Check", "Sensor 2: " + data.value(Quaternion.class).toString());
                    if (s2QuatListIndex < QuatListSize) {
                        s2QuatList[s2QuatListIndex] = data.value(Quaternion.class);
                        Log.i("SF Check", "Sensor 2: " + s2QuatList[s2QuatListIndex].toString());
                        s2QuatListIndex += 1;
                    } else {
                        stopSensorFusion(sf2, 2);
                    }
                }))
                .continueWith((Continuation<Route, Void>) task -> {
                    s2Route = task.getResult();
                    sf2.quaternion().start();
                    sf2.start();

                    return null;
                });

            }

            loadingImage.postDelayed(() -> {
                boolean fullConnection = true;

                // fullConnection Check
                if (board == null || board2 == null) {
                    fullConnection = false;
                } else {
                    if (s1QuatList[QuatListSize - 1] == null || s2QuatList[QuatListSize - 1] == null) {
                        fullConnection = false;
                    }
                }

                if (fullConnection) {
                    Log.i("SF Check", "Sensors Fully Connected");
                    loadingDialog.dismiss();
                    Bundle bundle = new Bundle();
                    bundle.putString("therapy", therapy);

                    SensorPlacementFragment sensorPlacementFragment = new SensorPlacementFragment();
                    sensorPlacementFragment.setArguments(bundle);

                    FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                    transaction.replace(R.id.therapyContainer, sensorPlacementFragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                } else {
                    String errorInstructions = "One or more of the sensors is not fully connected:\n";
                    Log.i("SF Check", "Sensors Not Fully Connected");
                    loadingText.setText("...OOPS!");
                    if (s1QuatList[QuatListSize - 1] == null && s2QuatList[QuatListSize - 1] == null) {
                        Log.i("SF Check", "Both sensors need calibrated");
                        errorInstructions += "Sensor 1\nSensor 2\n";
                    } else if (s1QuatList[QuatListSize - 1] == null) {
                        Log.i("SF Check", "Sensor 1 needs calibrated");
                        errorInstructions += "Sensor 1\n";
                    } else if (s2QuatList[QuatListSize - 1] == null) {
                        Log.i("SF Check", "Sensor 2 needs calibrated");
                        errorInstructions += "Sensor 2\n";
                    }
                    errorInstructions += "Please close the app and try again - we're sorry! This is a bug with the hardware.";
                    infoDisplay.setText(errorInstructions);
                    loadingImage.setVisibility(View.INVISIBLE);
                }

            }, 5000); // Should be 2000 normally - at 5000 for testing


        });

    }

    public void resetSensorUI() {
        ImageView s1BatteryDisplay = view.findViewById(R.id.s1battery);
        ImageView s2BatteryDisplay = view.findViewById(R.id.s2battery);
        TextView s1BatteryPercent = view.findViewById(R.id.s1batteryPercent);
        TextView s2BatteryPercent = view.findViewById(R.id.s2batteryPercent);
        TextView s1ConnectedTV = view.findViewById(R.id.SensorConnection1);
        TextView s2ConnectedTV = view.findViewById(R.id.SensorConnection2);
        Button s1Calibrate = view.findViewById(R.id.s1Calibrate);
        Button s2Calibrate = view.findViewById(R.id.s2Calibrate);

        // Sensor 1 Calibration Variables
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setCancelable(false);
        View view2 = getLayoutInflater().inflate(R.layout.popup_s1_calibration_screen, null);
        TextView s1AccelStatus = view2.findViewById(R.id.s1AccelStatus);
        TextView s1GyroStatus = view2.findViewById(R.id.s1GyroStatus);
        TextView s1MagnetStatus = view2.findViewById(R.id.s1MagnetStatus);
        ImageView s1AccelImage = view2.findViewById(R.id.s1AccelImage);
        ImageView s1GyroImage = view2.findViewById(R.id.s1GyroImage);
        ImageView s1MagnetImage = view2.findViewById(R.id.s1MagnetImage);
        Button s1close = view2.findViewById(R.id.close);

        // Sensor 2 Calibration Variables
        AlertDialog.Builder builder1 = new AlertDialog.Builder(requireActivity());
        builder1.setCancelable(false);
        View view3 = getLayoutInflater().inflate(R.layout.popup_s2_calibration_screen, null);
        TextView s2AccelStatus = view3.findViewById(R.id.s2AccelStatus);
        TextView s2GyroStatus = view3.findViewById(R.id.s2GyroStatus);
        TextView s2MagnetStatus = view3.findViewById(R.id.s2MagnetStatus);
        ImageView s2AccelImage = view3.findViewById(R.id.s2AccelImage);
        ImageView s2GyroImage = view3.findViewById(R.id.s2GyroImage);
        ImageView s2MagnetImage = view3.findViewById(R.id.s2MagnetImage);
        Button s2close = view3.findViewById(R.id.close);

        // Resetting Base UI
        s1BatteryDisplay.setImageResource(R.drawable.battery_unknown);
        s2BatteryDisplay.setImageResource(R.drawable.battery_unknown);

        s1BatteryPercent.setText("      %");
        s2BatteryPercent.setText("      %");

        s1ConnectedTV.setText("Disconnected");
        s1ConnectedTV.setBackgroundColor(Color.parseColor("#FF0000"));
        s2ConnectedTV.setText("Disconnected");
        s2ConnectedTV.setBackgroundColor(Color.parseColor("#FF0000"));

        s1Calibrate.setVisibility(View.INVISIBLE);
        s2Calibrate.setVisibility(View.INVISIBLE);

        // Resetting Sensor 1 Calibration UI
        s1AccelImage.setImageResource(R.drawable.connection_status_unreliable);
        s1AccelStatus.setText("Accelerometer: Unreliable");
        s1GyroImage.setImageResource(R.drawable.connection_status_unreliable);
        s1GyroStatus.setText("Gyroscope: Unreliable");
        s1MagnetImage.setImageResource(R.drawable.connection_status_unreliable);
        s1MagnetStatus.setText("Magnetometer: Unreliable");
        s1close.setVisibility(View.INVISIBLE);

        // Resetting Sensor 2 Calibration UI
        s2AccelImage.setImageResource(R.drawable.connection_status_unreliable);
        s2AccelStatus.setText("Accelerometer: Unreliable");
        s2GyroImage.setImageResource(R.drawable.connection_status_unreliable);
        s2GyroStatus.setText("Gyroscope: Unreliable");
        s2MagnetImage.setImageResource(R.drawable.connection_status_unreliable);
        s2MagnetStatus.setText("Magnetometer: Unreliable");


    }

    public void stopSensorFusion(SensorFusionBosch sf, int sNum) {
        Thread stopThread = new Thread(() -> {
            if (sNum == 1) {
                s1Route.remove();
            }
            if (sNum == 2) {
                s2Route.remove();
            }
            sf.stop();
            sf.quaternion().stop();
        });
        stopThread.start();


    }
}