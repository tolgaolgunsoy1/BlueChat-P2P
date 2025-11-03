package com.example.bluechat;

import android.Manifest;
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
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {
    private static final String TAG = "DeviceListActivity";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 101;

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private WebView webView;
    private BluetoothAdapter mBtAdapter;
    private android.content.SharedPreferences prefs;
    private Set<String> discoveredDevices = new HashSet<>();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(Activity.RESULT_CANCELED);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Load initial devices based on mode
                String mode = getIntent().getStringExtra("selection_mode");
                if ("paired".equals(mode)) {
                    loadPairedDevices();
                } else {
                    // For scan mode, start with empty list
                    updateDevicesInWebView(new JSONArray());
                }
            }
        });

        webView.loadUrl("file:///android_asset/device_search_web.html");

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        prefs = getSharedPreferences("bluechat_prefs", MODE_PRIVATE);

        // Check permissions
        if (!ensureBluetoothPermissions()) {
            return;
        }
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

    private void loadPairedDevices() {
        Set<BluetoothDevice> pairedDevices = null;
        try {
            pairedDevices = mBtAdapter.getBondedDevices();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get bonded devices", e);
            pairedDevices = new HashSet<>();
        }

        JSONArray devicesArray = new JSONArray();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                if (device == null) continue;
                try {
                    JSONObject deviceObj = createDeviceJSONObject(device, true, Short.MIN_VALUE);
                    devicesArray.put(deviceObj);
                } catch (Exception e) {
                    Log.w(TAG, "Error creating device JSON", e);
                }
            }
        }
        updateDevicesInWebView(devicesArray);
    }

    private JSONObject createDeviceJSONObject(BluetoothDevice device, boolean isPaired, short rssi) throws Exception {
        String deviceName = "Unknown Device";
        try {
            String name = device.getName();
            if (name != null && !name.trim().isEmpty()) {
                deviceName = name;
            }
        } catch (Exception e) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for device name", e);
        }

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

        JSONObject deviceObj = new JSONObject();
        deviceObj.put("name", deviceName);
        deviceObj.put("address", device.getAddress());
        deviceObj.put("type", getDeviceType(icon));
        deviceObj.put("isOnline", isPaired);
        deviceObj.put("signalStrength", rssi != Short.MIN_VALUE ? rssi + " dBm" : "--");
        deviceObj.put("distance", calculateDistance(rssi));

        return deviceObj;
    }

    private String getDeviceType(String icon) {
        switch (icon) {
            case "💻": return "computer";
            case "🔊": return "audio";
            case "📱": return "phone";
            case "⌚": return "watch";
            case "❤️": return "health";
            default: return "unknown";
        }
    }

    private String calculateDistance(short rssi) {
        if (rssi == Short.MIN_VALUE) return "--";
        // Simple distance estimation (not accurate)
        double distance = Math.pow(10, (rssi + 50) / -20.0);
        return String.format(Locale.getDefault(), "%.1fm", distance);
    }

    private void updateDevicesInWebView(JSONArray devicesArray) {
        runOnUiThread(() -> {
            webView.evaluateJavascript("javascript:updateDevices(" + devicesArray.toString() + ")", null);
        });
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
                String mode = getIntent().getStringExtra("selection_mode");
                if ("paired".equals(mode)) {
                    loadPairedDevices();
                }
            } else {
                Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtAdapter != null) {
            try {
                mBtAdapter.cancelDiscovery();
            } catch (SecurityException e) {
                Log.w(TAG, "cancelDiscovery failed", e);
            }
        }
        unregisterReceiver(mReceiver);
    }

    private void startDiscovery() {
        if (mBtAdapter == null || isScanning) return;

        try {
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }
        } catch (SecurityException e) {
            Log.w(TAG, "cancelDiscovery failed", e);
        }

        try {
            discoveredDevices.clear();
            mBtAdapter.startDiscovery();
            isScanning = true;
            runOnUiThread(() -> {
                webView.evaluateJavascript("javascript:onDiscoveryStarted()", null);
            });
        } catch (SecurityException e) {
            Log.w(TAG, "startDiscovery failed", e);
            Toast.makeText(this, "Discovery failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopDiscovery() {
        if (mBtAdapter == null || !isScanning) return;

        try {
            mBtAdapter.cancelDiscovery();
            isScanning = false;
            runOnUiThread(() -> {
                webView.evaluateJavascript("javascript:onDiscoveryFinished()", null);
            });
        } catch (SecurityException e) {
            Log.w(TAG, "cancelDiscovery failed", e);
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void goBack() {
            runOnUiThread(() -> finish());
        }

        @JavascriptInterface
        public void startDiscovery() {
            runOnUiThread(() -> startDiscovery());
        }

        @JavascriptInterface
        public void stopDiscovery() {
            runOnUiThread(() -> stopDiscovery());
        }

        @JavascriptInterface
        public void selectDevice(String address) {
            runOnUiThread(() -> {
                stopDiscovery();
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                setResult(Activity.RESULT_OK, intent);
                finish();
            });
        }

        @JavascriptInterface
        public void loadDevices() {
            runOnUiThread(() -> {
                String mode = getIntent().getStringExtra("selection_mode");
                if ("paired".equals(mode)) {
                    loadPairedDevices();
                } else {
                    updateDevicesInWebView(new JSONArray());
                }
            });
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;

                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                int bondState = BluetoothDevice.BOND_NONE;
                try {
                    bondState = device.getBondState();
                } catch (Exception e) {
                    Log.w(TAG, "getBondState failed", e);
                }

                if (bondState != BluetoothDevice.BOND_BONDED && !discoveredDevices.contains(device.getAddress())) {
                    discoveredDevices.add(device.getAddress());
                    try {
                        JSONObject deviceObj = createDeviceJSONObject(device, false, rssi);
                        runOnUiThread(() -> {
                            webView.evaluateJavascript("javascript:addDevice(" + deviceObj.toString() + ")", null);
                        });
                    } catch (Exception e) {
                        Log.w(TAG, "Error adding discovered device", e);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                isScanning = false;
                runOnUiThread(() -> {
                    webView.evaluateJavascript("javascript:onDiscoveryFinished()", null);
                });
            }
        }
    };
}
