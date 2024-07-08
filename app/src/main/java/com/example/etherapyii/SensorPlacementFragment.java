package com.example.etherapyii;

import static android.service.controls.ControlsProviderService.TAG;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.module.Led;

import java.util.Objects;


public class SensorPlacementFragment extends Fragment implements ServiceConnection {
    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard board, board2;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor_placement, container, false);

        // Variable Declarations
        String therapy;
        ImageView hand_image;
        ImageView human_image;
        Button start_activity;
        BluetoothManager bluetoothManager = requireActivity().getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Binding Service
        requireActivity().getApplicationContext().bindService(new Intent(getActivity(), BtleService.class), this, Context.BIND_AUTO_CREATE);

        // Getting Metric From Connection Fragment
        assert getArguments() != null;
        therapy = getArguments().getString("therapy");

        // Setting Images on Therapy
        switch (Objects.requireNonNull(therapy)) {
            case "Hott":
                Log.i("Hott", "Switch enterred - case 'Hott'");
                hand_image = view.findViewById(R.id.hott_hand);
                human_image = view.findViewById(R.id.hott_human);
                hand_image.setImageResource(R.drawable.hott_hand);
                human_image.setImageResource(R.drawable.hott_human);
                break;
        }

        // Next Activity Navigation
        start_activity = view.findViewById(R.id.start_activity);
        start_activity.setOnClickListener(v -> {
            // Reset Sensor LEDs
            turnOffLEDs();

            // Getting Value from SeekBar
            SeekBar seekBar = view.findViewById(R.id.reps_input);
            int reps = seekBar.getProgress();
            SeekBar seekBar2 = view.findViewById(R.id.hold_input);
            int holdTime = seekBar2.getProgress();
            SeekBar seekBar3 = view.findViewById(R.id.threshhold_input);
            int threshold = seekBar3.getProgress();

            // Create a bundle with the necessary information
            Bundle bundle = new Bundle();
            bundle.putString("therapy", therapy);
            bundle.putInt("reps", reps);
            bundle.putInt("holdTime", holdTime);
            bundle.putInt("threshold", threshold);

            // Create the new fragment and set the bundle as its arguments
            TherapyMainFragment therapyMainFragment = new TherapyMainFragment();
            therapyMainFragment.setArguments(bundle);

            // Replace the current fragment with the new one
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.therapyContainer, therapyMainFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        SeekBar seekBar = view.findViewById(R.id.reps_input);
        TextView valueLabel = view.findViewById(R.id.reps_value_label);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueLabel.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed for this example
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed for this example
            }
        });

        SeekBar seekBar2 = view.findViewById(R.id.hold_input);
        TextView valueLabel2 = view.findViewById(R.id.hold_value_label);
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar2, int progress, boolean fromUser) {
                valueLabel2.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar2) {
                // Not needed for this example
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar2) {
                // Not needed for this example
            }
        });

        SeekBar seekBar3 = view.findViewById(R.id.threshhold_input);
        TextView valueLabel3 = view.findViewById(R.id.threshhold_value_label);
        seekBar3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar3, int progress, boolean fromUser) {
                valueLabel3.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar3) {
                // Not needed for this example
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar3) {
                // Not needed for this example
            }
        });


        // Inflate the layout for this fragment
        return view;
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


    /**
     * Uses the MAC addresses of the sensors to make sure that they are connected
     */
    public void retrieveBoard() {
        final BluetoothManager btManager = (BluetoothManager) requireActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        //Has the haptic coin
        String macAddress1 = "ED:5B:0A:50:14:59";
        BluetoothDevice sensor = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress1);
        String macAddress2 = "FE:C2:4B:10:FB:D5";
        BluetoothDevice sensor2 = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress2);


        // Create a MetaWear board object for the Bluetooth Device
        board = serviceBinder.getMetaWearBoard(sensor);
        board2 = serviceBinder.getMetaWearBoard(sensor2);
        turnOnLEDs(); //TODO: Find new way to turn on LEDs so that it happens every time the activity is started, not just the first time
    }

    /**
     * Turns on the sensor LEDs
     */
    public void turnOnLEDs() {
        //Turn on LEDs
        Led led, led2;
        if ((led = board.getModule(Led.class)) != null) {
            led.stop(true);
            led.editPattern(Led.Color.RED, Led.PatternPreset.SOLID)
                    .commit();
            led.play();
        }
        if ((led2 = board2.getModule(Led.class)) != null) {
            led2.stop(true);
            led2.editPattern(Led.Color.BLUE, Led.PatternPreset.SOLID)
                    .commit();
            led2.play();
        }
    }

    /**
     * Turns off the sensor LEDs
     */
    public void turnOffLEDs() {
        //Turn on LEDs
        Led led, led2;
        if ((led = board.getModule(Led.class)) != null) {
            led.stop(true);
        }
        if ((led2 = board2.getModule(Led.class)) != null) {
            led2.stop(true);
        }
    }
}