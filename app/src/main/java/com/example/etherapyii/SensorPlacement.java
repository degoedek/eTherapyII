package com.example.etherapyii;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.module.Led;

import java.util.Objects;

public class SensorPlacement extends AppCompatActivity implements ServiceConnection {
    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard board, board2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sensor_placement);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Variable Declarations
        String therapy;
        ImageView hand_image;
        ImageView human_image;
        Button start_activity;
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Binding Service
        getApplicationContext().bindService(new Intent(this, BtleService.class), this, Context.BIND_AUTO_CREATE);

        // Getting Metric From Therapy Selection
        Intent intent = getIntent();
        therapy = intent.getExtras().getString("Therapy");

        // Setting Images on Therapy
        switch (Objects.requireNonNull(therapy)) {
            case "Hott":
                Log.i("Hott", "Switch enterred - case 'Hott'");
                hand_image = findViewById(R.id.hott_hand);
                human_image = findViewById(R.id.hott_human);
                hand_image.setImageResource(R.drawable.hott_hand);
                human_image.setImageResource(R.drawable.hott_human);
                break;
        }

        // Next Activity Navigation
        start_activity = findViewById(R.id.start_activity);
        start_activity.setOnClickListener(v -> {
            // Reset Sensor LEDs
            turnOffLEDs();

            // Getting Value from SeekBar
            SeekBar seekBar = findViewById(R.id.reps_input);
            int reps = seekBar.getProgress();
            SeekBar seekBar2 = findViewById(R.id.hold_input);
            int holdTime = seekBar2.getProgress();

            Intent intent2 = new Intent(SensorPlacement.this, TherapyActivity.class);
            intent2.putExtra("Therapy" , therapy);
            intent2.putExtra("Reps" , reps);
            intent2.putExtra("HoldTime", holdTime);
            startActivity(intent2);
        });

        SeekBar seekBar = findViewById(R.id.reps_input);
        TextView valueLabel = findViewById(R.id.reps_value_label);

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

         SeekBar seekBar2 = findViewById(R.id.hold_input);
         TextView valueLabel2 = findViewById(R.id.hold_value_label);
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

    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Log.d("SensorPlacement", "Service Connected");
        //Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;
        retrieveBoard();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.i("SensorPlacement", "Service Disconnected");
    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//
//        ///< Unbind the service when the activity is destroyed
//        getApplicationContext().unbindService(this);
//    }

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