package com.example.bluechat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISCOVERABLE_BT = 2;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 3;
    
    private BluetoothAdapter bluetoothAdapter;
    private Button btnSelectDevice;
    private Button btnMakeDiscoverable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Bluetooth adapter'ı al
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // Cihaz Bluetooth desteklemiyor mu kontrol et
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bu cihaz Bluetooth desteklemiyor", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // Butonları bul
        btnSelectDevice = findViewById(R.id.select_device_button);
        btnMakeDiscoverable = findViewById(R.id.make_discoverable_button);
        
        // Bluetooth izinlerini kontrol et ve iste
        checkBluetoothPermissions();
        
        // Select Device butonu
        btnSelectDevice.setOnClickListener(v -> {
            if (!bluetoothAdapter.isEnabled()) {
                // Bluetooth kapalıysa aç
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                    checkBluetoothPermissions();
                    return;
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                // DeviceListActivity'yi başlat
                Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivity(intent);
            }
        });
        
        // Make Discoverable butonu
        btnMakeDiscoverable.setOnClickListener(v -> {
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Önce Bluetooth'u açın", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Cihazı görünür yap
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) 
                != PackageManager.PERMISSION_GRANTED) {
                checkBluetoothPermissions();
                return;
            }
            startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE_BT);
        });
    }
    
    private void checkBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12 ve üzeri için
            String[] permissions = {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
            };
            
            boolean needsPermission = false;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                    needsPermission = true;
                    break;
                }
            }
            
            if (needsPermission) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS);
            }
        } else {
            // Android 11 ve altı için
            String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            };
            
            boolean needsPermission = false;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                    needsPermission = true;
                    break;
                }
            }
            
            if (needsPermission) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS);
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth açıldı", Toast.LENGTH_SHORT).show();
                // DeviceListActivity'yi başlat
                Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Bluetooth açılamadı", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_DISCOVERABLE_BT) {
            if (resultCode > 0) {
                Toast.makeText(this, "Cihaz " + resultCode + " saniye boyunca görünür", 
                    Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Görünür yapılamadı", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Bluetooth izinleri verildi", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth izinleri gerekli!", Toast.LENGTH_LONG).show();
            }
        }
    }
}
