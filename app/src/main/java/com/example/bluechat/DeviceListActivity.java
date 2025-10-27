package com.example.bluechat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class DeviceListActivity extends Activity {
    private static final String TAG = "DeviceListActivity";

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);

        setResult(Activity.RESULT_CANCELED);

        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        // Note: getBondedDevices may require BLUETOOTH_CONNECT permission on Android 12+
        // but we're handling this in the Activity level
        Set<BluetoothDevice> pairedDevices = null;
        try {
            pairedDevices = mBtAdapter.getBondedDevices();
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to get bonded devices due to missing permission");
            pairedDevices = new java.util.HashSet<>();
        }

        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                // Note: device.getName() may require BLUETOOTH_CONNECT permission on Android 12+
                // but we're handling this in the Activity level
                String deviceName = "Unknown Device";
                try {
                    String name = device.getName();
                    if (name != null && !name.trim().isEmpty()) {
                        deviceName = name;
                    }
                } catch (SecurityException e) {
                    // Permission not granted, use default name
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for device name");
                }
                mPairedDevicesArrayAdapter.add(deviceName + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
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

    private void doDiscovery() {
        findViewById(R.id.scanning_progress).setVisibility(View.VISIBLE);
        setTitle(R.string.scanning);

        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // Note: isDiscovering and cancelDiscovery may require BLUETOOTH_SCAN permission on Android 12+
        // but we're handling this in the Activity level
        try {
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }
        } catch (SecurityException e) {
            Log.w(TAG, "isDiscovering/cancelDiscovery failed due to missing permission");
        }

        // Note: startDiscovery may require BLUETOOTH_SCAN permission on Android 12+
        // but we're handling this in the Activity level
        try {
            mBtAdapter.startDiscovery();
        } catch (SecurityException e) {
            Log.w(TAG, "startDiscovery failed due to missing permission");
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Note: cancelDiscovery may require BLUETOOTH_SCAN permission on Android 12+
            // but we're handling this in the Activity level
            try {
                mBtAdapter.cancelDiscovery();
            } catch (SecurityException e) {
                Log.w(TAG, "cancelDiscovery failed due to missing permission");
            }

            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Note: getBondState may require BLUETOOTH_CONNECT permission on Android 12+
                // but we're handling this in the Activity level
                int bondState = BluetoothDevice.BOND_NONE;
                try {
                    bondState = device.getBondState();
                } catch (SecurityException e) {
                    Log.w(TAG, "getBondState failed due to missing permission");
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
                    } catch (SecurityException e) {
                        // Permission not granted, use default name
                        Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for device name");
                    }
                    mNewDevicesArrayAdapter.add(deviceName + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                findViewById(R.id.scanning_progress).setVisibility(View.GONE);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };
}
