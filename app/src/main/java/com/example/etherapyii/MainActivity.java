package com.example.etherapyii;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wit.witsdk.modular.sensor.modular.connector.modular.bluetooth.WitBluetoothManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "PermissionCheck";  // Log tag for debugging
    private boolean toastShown = false;

    // ActivityResultLauncher for requesting multiple permissions
    private ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.welcomeContainer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the permissions launcher
        initializePermissionLauncher();

        // Check and request permissions
        if (!checkPermissions()) {
            requestPermissions();
        }

        // Initialize fragment transaction
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.welcomeContainer, new WelcomeScreen()).commit();
    }

    // Initializes the ActivityResultLauncher for permission requests
    private void initializePermissionLauncher() {
        requestMultiplePermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::handlePermissionsResult
        );
    }

    // Handle the result of permission requests
    private void handlePermissionsResult(Map<String, Boolean> permissions) {
        boolean allGranted = true;
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            String permission = entry.getKey();
            boolean isGranted = entry.getValue();

            if (isGranted) {
                Log.i(TAG, "Permission granted: " + permission);
            } else {
                allGranted = false;
                Log.w(TAG, "Permission denied: " + permission);
            }
        }

        if (allGranted) {
            try {
                WitBluetoothManager.initInstance(this);
                Toast.makeText(this, "Bluetooth manager initialized.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to initialize Bluetooth manager.", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (!toastShown) {
                toastShown = true;
                Toast.makeText(this, "Bluetooth permissions are required to use this feature.", Toast.LENGTH_SHORT).show();
            }
            requestPermissions();  // Request permissions again if denied
        }
    }

    // Checks if all necessary permissions are granted
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {  // Android 12+
            return checkPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    checkPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
                    checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);  // Optional
        } else {
            return checkPermission(Manifest.permission.BLUETOOTH) &&
                    checkPermission(Manifest.permission.BLUETOOTH_ADMIN);
        }
    }

    // Helper method to check individual permissions
    private boolean checkPermission(String permission) {
        boolean granted = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        Log.i(TAG, permission + " granted: " + granted);
        return granted;
    }

    // Request the necessary permissions
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {  // Android 12+
            List<String> permissionsToRequest = new ArrayList<>();

            if (!checkPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (!checkPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (!permissionsToRequest.isEmpty()) {
                requestMultiplePermissionsLauncher.launch(
                        permissionsToRequest.toArray(new String[0])
                );
            }
        } else {
            requestMultiplePermissionsLauncher.launch(new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
            });
        }
    }
}
