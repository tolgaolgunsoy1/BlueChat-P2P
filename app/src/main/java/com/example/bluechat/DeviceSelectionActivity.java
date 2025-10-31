package com.example.bluechat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;

public class DeviceSelectionActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    private BluetoothAdapter bluetoothAdapter;
    private MaterialButton btnPairedDevices, btnNewDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_selection);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Always initialize views and listeners first
        initViews();
        setupClickListeners();

        // Check Bluetooth availability
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermissions();
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        // Check permissions
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }
    }

    private void initViews() {
        btnPairedDevices = findViewById(R.id.btnPairedDevices);
        btnNewDevices = findViewById(R.id.btnNewDevices);
    }

    private void setupClickListeners() {
        btnPairedDevices.setOnClickListener(v -> {
            Intent intent = new Intent(this, DeviceListActivity.class);
            intent.putExtra("selection_mode", "paired");
            startActivityForResult(intent, 1);
        });

        btnNewDevices.setOnClickListener(v -> {
            Intent intent = new Intent(this, DeviceListActivity.class);
            intent.putExtra("selection_mode", "scan");
            startActivityForResult(intent, 1);
        });
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            };
            ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            };
            ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth enabled, proceed
                if (hasBluetoothPermissions()) {
                    initViews();
                    setupClickListeners();
                } else {
                    requestBluetoothPermissions();
                }
            } else {
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == 1) {
            if (resultCode == RESULT_OK && data != null) {
                String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                if (address != null && !address.isEmpty()) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, address);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, R.string.permissions_granted, Toast.LENGTH_SHORT).show();
                initViews();
                setupClickListeners();
            } else {
                Toast.makeText(this, R.string.bluetooth_permissions_required, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
