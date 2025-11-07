package com.example.bluechat;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DeviceSelectionActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceClickListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    private BluetoothAdapter bluetoothAdapter;
    private Button btnScan, btnBack, btnRefresh;
    private SwitchCompat bluetoothToggle;
    private TextView statusText, bluetoothDesc, sectionTitle;
    private EditText searchInput;
    private RecyclerView devicesRecycler;
    private View emptyState;
    private DeviceAdapter deviceAdapter;

    private List<DeviceItem> allDevices = new ArrayList<>();
    private boolean isScanning = false;
    private Handler scanHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_selection);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check Bluetooth availability
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        updateBluetoothStatus();

        // Check permissions
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }
    }

    private void initViews() {
        bluetoothToggle = findViewById(R.id.bluetooth_toggle);
        btnScan = findViewById(R.id.scan_button);
        btnBack = findViewById(R.id.back_button);
        btnRefresh = findViewById(R.id.refresh_button);
        statusText = findViewById(R.id.status_text);
        bluetoothDesc = findViewById(R.id.bluetooth_desc);
        searchInput = findViewById(R.id.search_input);
        devicesRecycler = findViewById(R.id.devices_recycler);
        emptyState = findViewById(R.id.empty_state);
        sectionTitle = findViewById(R.id.section_title);

        // Setup RecyclerView
        deviceAdapter = new DeviceAdapter(this);
        devicesRecycler.setLayoutManager(new LinearLayoutManager(this));
        devicesRecycler.setAdapter(deviceAdapter);

        // Load mock devices
        loadMockDevices();
        updateDeviceCount();
    }

    private void setupClickListeners() {
        if (bluetoothToggle != null) {
            bluetoothToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Enable Bluetooth
                    if (!bluetoothAdapter.isEnabled()) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            requestBluetoothPermissions();
                            return;
                        }
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        updateBluetoothStatus();
                        enableScanButton();
                    }
                } else {
                    // Disable Bluetooth
                    if (bluetoothAdapter.isEnabled()) {
                        bluetoothAdapter.disable();
                    }
                    updateBluetoothStatus();
                    disableScanButton();
                }
            });
        }

        if (btnScan != null) {
            btnScan.setOnClickListener(v -> {
                startScanning();
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                startScanning();
            });
        }

        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterDevices(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
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

    private void updateBluetoothStatus() {
        if (bluetoothAdapter != null && statusText != null && bluetoothDesc != null) {
            boolean isEnabled = bluetoothAdapter.isEnabled();
            if (isEnabled) {
                statusText.setText("Bluetooth Açık");
                bluetoothDesc.setText("Yakındaki cihazları tarayabilirsiniz");
                enableScanButton();
                if (bluetoothToggle != null) bluetoothToggle.setChecked(true);
            } else {
                statusText.setText("Bluetooth Kapalı");
                bluetoothDesc.setText("Bluetooth'u açarak cihaz aramaya başlayın");
                disableScanButton();
                if (bluetoothToggle != null) bluetoothToggle.setChecked(false);
            }
        }
    }

    private void enableScanButton() {
        if (btnScan != null) btnScan.setEnabled(true);
    }

    private void disableScanButton() {
        if (btnScan != null) btnScan.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                updateBluetoothStatus();
            } else {
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                if (bluetoothToggle != null) bluetoothToggle.setChecked(false);
                updateBluetoothStatus();
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
                updateBluetoothStatus();
            } else {
                Toast.makeText(this, R.string.bluetooth_permissions_required, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public void onDeviceClick(DeviceItem device) {
        // Handle device selection
        Toast.makeText(this, "Cihaz seçildi: " + device.getName(), Toast.LENGTH_SHORT).show();
        // For now, just finish with result. In real implementation, connect to device
        Intent resultIntent = new Intent();
        resultIntent.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, device.getAddress());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void loadMockDevices() {
        allDevices.clear();
        allDevices.add(new DeviceItem("📱", "iPhone 14 Pro", "00:11:22:33:44:55", true, 2.5f));
        allDevices.add(new DeviceItem("💻", "MacBook Air M2", "AA:BB:CC:DD:EE:FF", true, 5.1f));
        allDevices.add(new DeviceItem("⌚", "Galaxy Watch 5", "11:22:33:44:55:66", false, 0f));
        deviceAdapter.setDevices(allDevices);
    }

    private void updateDeviceCount() {
        if (sectionTitle != null) {
            sectionTitle.setText(String.format(getString(R.string.nearby_devices_section), allDevices.size()));
        }
        if (emptyState != null) {
            emptyState.setVisibility(allDevices.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (devicesRecycler != null) {
            devicesRecycler.setVisibility(allDevices.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void startScanning() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }

        if (isScanning) return;

        isScanning = true;
        btnScan.setText(R.string.scanning_text);
        btnRefresh.setEnabled(false);

        // Add pulse animation to scan button
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(btnScan, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(btnScan, "scaleY", 1f, 1.05f, 1f);
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.start();
        scaleY.start();

        // Simulate scanning for 3 seconds
        scanHandler.postDelayed(() -> {
            isScanning = false;
            btnScan.setText(R.string.scan_devices_text);
            btnRefresh.setEnabled(true);
            scaleX.cancel();
            scaleY.cancel();
            btnScan.setScaleX(1f);
            btnScan.setScaleY(1f);
            Toast.makeText(this, "Tarama tamamlandı", Toast.LENGTH_SHORT).show();
        }, 3000);
    }

    private void filterDevices(String query) {
        List<DeviceItem> filtered = new ArrayList<>();
        for (DeviceItem device : allDevices) {
            if (device.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(device);
            }
        }
        deviceAdapter.setDevices(filtered);
        updateDeviceCount();
    }
}
