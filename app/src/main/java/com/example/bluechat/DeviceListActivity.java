package com.example.bluechat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import android.Manifest;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {
    private static final String TAG = "DeviceListActivity";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 101;

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter mBtAdapter;
    private RichDeviceAdapter mNewDevicesAdapter;
    private RichDeviceAdapter mRecentDevicesAdapter;
    private android.content.SharedPreferences prefs;

    private RecyclerView devicesRecycler;
    private RecyclerView recentDevicesRecycler;
    private MaterialButton btnScanToggle;
    private TextView statusText;
    private View statusProgress;
    private AutoCompleteTextView filterDropdown;
    private View permissionWarning;
    private View emptyState;
    private MaterialButton btnRetryScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);

        setResult(Activity.RESULT_CANCELED);

        // Get selection mode from intent
        String mode = getIntent().getStringExtra("selection_mode");

        // Initialize new UI components
        devicesRecycler = findViewById(R.id.devices_recycler);
        recentDevicesRecycler = findViewById(R.id.recent_devices_recycler);
        btnScanToggle = findViewById(R.id.btn_scan_toggle);
        statusText = findViewById(R.id.status_text);
        statusProgress = findViewById(R.id.status_progress);
        filterDropdown = findViewById(R.id.filter_dropdown);
        permissionWarning = findViewById(R.id.permission_warning);
        emptyState = findViewById(R.id.empty_state);
        btnRetryScan = findViewById(R.id.btn_retry_scan);

        // Setup toolbar
        android.view.View tb = findViewById(R.id.toolbar);
        if (tb instanceof androidx.appcompat.widget.Toolbar) {
            androidx.appcompat.widget.Toolbar toolbar = (androidx.appcompat.widget.Toolbar) tb;
            setSupportActionBar(toolbar);
            toolbar.setTitle("🔵 Cihaz Ara");
            toolbar.setTitleTextColor(getResources().getColor(R.color.colorTextPrimary));
        }

        // Setup RecyclerViews
        devicesRecycler.setLayoutManager(new LinearLayoutManager(this));
        recentDevicesRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        mNewDevicesAdapter = new RichDeviceAdapter(this);
        mRecentDevicesAdapter = new RichDeviceAdapter(this);

        devicesRecycler.setAdapter(mNewDevicesAdapter);
        recentDevicesRecycler.setAdapter(mRecentDevicesAdapter);

        // Setup scan toggle button
        btnScanToggle.setOnClickListener(v -> {
            if (mBtAdapter != null && mBtAdapter.isDiscovering()) {
                stopDiscovery();
            } else {
                startDiscovery();
            }
        });

        // Setup filter dropdown
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.device_filters));
        filterDropdown.setAdapter(filterAdapter);
        filterDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String filter = (String) parent.getItemAtPosition(position);
            applyFilter(filter);
        });

        // Setup retry button
        btnRetryScan.setOnClickListener(v -> startDiscovery());

        // Setup device click listeners
        mNewDevicesAdapter.setOnDeviceClickListener(this::onDeviceClicked);
        mRecentDevicesAdapter.setOnDeviceClickListener(this::onDeviceClicked);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        prefs = getSharedPreferences("bluechat_prefs", MODE_PRIVATE);

        // Check and request Bluetooth permissions if needed, then populate paired devices
        if (!ensureBluetoothPermissions()) {
            // Permissions requested; wait for callback to populate
            return;
        }
        populatePairedDevices();
    }

    private boolean ensureBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            String[] permissions = new String[] {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            };
            boolean needs = false;
            for (String p : permissions) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    needs = true;
                    break;
                }
            }
            if (needs) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS);
                return false;
            }
            return true;
        } else {
            String[] permissions = new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION
            };
            boolean needs = false;
            for (String p : permissions) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    needs = true;
                    break;
                }
            }
            if (needs) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS);
                return false;
            }
            return true;
        }
    }

    private void populatePairedDevices() {
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = null;
        try {
            pairedDevices = mBtAdapter.getBondedDevices();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get bonded devices due to missing permission", e);
            pairedDevices = new java.util.HashSet<>();
        }

        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device == null) continue;
                String deviceName = "Unknown Device";
                try {
                    String name = device.getName();
                    if (name != null && !name.trim().isEmpty()) {
                        deviceName = name;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for device name", e);
                }
                // Paired device, determine icon; RSSI unknown
                String icon = "📱";
                try {
                    BluetoothClass bc = device.getBluetoothClass();
                    if (bc != null) {
                        int mc = bc.getMajorDeviceClass();
                        if (mc == BluetoothClass.Device.Major.COMPUTER) icon = "💻";
                        else if (mc == BluetoothClass.Device.Major.AUDIO_VIDEO) icon = "🔊";
                        else if (mc == BluetoothClass.Device.Major.PHONE) icon = "📱";
                        else if (mc == BluetoothClass.Device.Major.WEARABLE) icon = "⌚";
                        else if (mc == BluetoothClass.Device.Major.HEALTH) icon = "❤️";
                    }
                } catch (Exception ignored) {}
                String nickname = prefs.getString("nickname_" + device.getAddress(), null);
                mRecentDevicesAdapter.addOrUpdate(new DeviceItem(deviceName, device.getAddress(), Short.MIN_VALUE, true, icon, nickname));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean granted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                populatePairedDevices();
            } else {
                Log.w(TAG, "Bluetooth permissions not granted; limited functionality");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBtAdapter != null) {
            // Note: cancelDiscovery may require BLUETOOTH_SCAN permission on Android 12+
            // but we're handling this in the Activity level
            try {
                mBtAdapter.cancelDiscovery();
            } catch (SecurityException e) {
                Log.w(TAG, "cancelDiscovery failed due to missing permission");
            }
        }

        this.unregisterReceiver(mReceiver);
    }

    private void startDiscovery() {
        if (mBtAdapter == null) return;

        try {
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }
        } catch (SecurityException e) {
            Log.w(TAG, "cancelDiscovery failed due to missing permission");
        }

        try {
            mBtAdapter.startDiscovery();
            updateScanUI(true);
            Toast.makeText(this, R.string.scanning_status, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Log.w(TAG, "startDiscovery failed due to missing permission");
            showPermissionWarning();
        }
    }

    private void stopDiscovery() {
        if (mBtAdapter == null) return;

        try {
            mBtAdapter.cancelDiscovery();
            updateScanUI(false);
            Toast.makeText(this, R.string.scan_stopped_status, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Log.w(TAG, "cancelDiscovery failed due to missing permission");
        }
    }

    private void updateScanUI(boolean isScanning) {
        if (isScanning) {
            btnScanToggle.setText(R.string.stop_scan_button);
            android.graphics.drawable.Drawable pauseIcon = getDrawable(android.R.drawable.ic_media_pause);
            if (pauseIcon != null) {
                btnScanToggle.setIcon(pauseIcon);
            }
            statusText.setText(R.string.scanning_status);
            statusProgress.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        } else {
            btnScanToggle.setText(R.string.scan_button);
            android.graphics.drawable.Drawable rotateIcon = getDrawable(android.R.drawable.ic_menu_rotate);
            if (rotateIcon != null) {
                btnScanToggle.setIcon(rotateIcon);
            }
            statusText.setText(R.string.scan_stopped_status);
            statusProgress.setVisibility(View.GONE);
            updateEmptyState();
        }
    }

    private void updateEmptyState() {
        if (mNewDevicesAdapter.getItemCount() == 0) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showPermissionWarning() {
        permissionWarning.setVisibility(View.VISIBLE);
        Snackbar.make(findViewById(android.R.id.content), R.string.permission_warning, Snackbar.LENGTH_LONG)
                .setAction("Grant", v -> ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_BLUETOOTH_PERMISSIONS))
                .show();
    }

    private void applyFilter(String filter) {
        // Implement filtering logic here
        // For now, just show a toast
        Toast.makeText(this, "Filter: " + filter, Toast.LENGTH_SHORT).show();
    }

    private void onDeviceClicked(DeviceItem device) {
        Toast.makeText(this, getString(R.string.connecting_toast, device.name), Toast.LENGTH_SHORT).show();

        // Stop discovery
        stopDiscovery();

        // Return device address
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, device.address);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }



    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                // Note: getBondState may require BLUETOOTH_CONNECT permission on Android 12+
                // but we're handling this in the Activity level
                int bondState = BluetoothDevice.BOND_NONE;
                try {
                    bondState = device.getBondState();
                } catch (Exception e) {
                    Log.w(TAG, "getBondState failed due to missing permission", e);
                }
                if (bondState != BluetoothDevice.BOND_BONDED) {
                    // Note: device.getName() may require BLUETOOTH_CONNECT permission on Android 12+
                    // but we're handling this in the Activity level
                    String deviceName = "Unknown Device";
                    try {
                        String name = device.getName();
                        if (name != null && !name.trim().isEmpty()) {
                            deviceName = name;
                        }
                    } catch (Exception e) {
                        // Permission not granted, use default name
                        Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for device name", e);
                    }
                    // Device type icon
                    String icon = "📱";
                    try {
                        BluetoothClass bc = device.getBluetoothClass();
                        if (bc != null) {
                            int mc = bc.getMajorDeviceClass();
                            if (mc == BluetoothClass.Device.Major.COMPUTER) icon = "💻";
                            else if (mc == BluetoothClass.Device.Major.AUDIO_VIDEO) icon = "🔊";
                            else if (mc == BluetoothClass.Device.Major.PHONE) icon = "📱";
                            else if (mc == BluetoothClass.Device.Major.WEARABLE) icon = "⌚";
                            else if (mc == BluetoothClass.Device.Major.HEALTH) icon = "❤️";
                        }
                    } catch (Exception ignored) {}
                    // Add/update rich item (avoid duplicates by address)
                    if (!mNewDevicesAdapter.containsAddress(device.getAddress())) {
                        mNewDevicesAdapter.addOrUpdate(new DeviceItem(deviceName, device.getAddress(), rssi, false, icon));
                    } else {
                        mNewDevicesAdapter.addOrUpdate(new DeviceItem(deviceName, device.getAddress(), rssi, false, icon));
                    }
                    updateEmptyState();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                updateScanUI(false);
                if (mNewDevicesAdapter.getItemCount() == 0) {
                    Toast.makeText(DeviceListActivity.this, R.string.no_devices_found_title, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
}
