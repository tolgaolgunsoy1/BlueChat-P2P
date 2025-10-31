package com.example.bluechat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;
    private static final int REQUEST_DISCOVERABLE_BT = 3;
    private static final int REQUEST_SELECT_DEVICE = 4;
    private static final int REQUEST_CONNECT_DEVICE = 5;
    
    private BluetoothAdapter bluetoothAdapter;
    private MaterialButton btnScan;
    private BroadcastReceiver bluetoothStateReceiver;

    // New views for updated layout
    private View statusIcon;
    private TextView statusText, statusSubtext;
    private MaterialCardView cardBluetooth, cardVisibility, cardSettings, cardPaired;
    private View bluetoothIcon, visibilityIcon, settingsIcon, pairedIcon, deviceIcon;
    private View bluetoothToggle;
    private TextView visibilityStatus, pairedCount;
    private TextView seeAllText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        // ❗ BURASI GÜNCELLENDİ: Splash teması bittikten sonra normal uygulama temasına geçiş yapılır.
        // Bu satır, Manifest'teki splash teması biter bitmez devreye girer.
        setTheme(R.style.Theme_BlueChat); 

        // Apply theme from preferences before super.onCreate (BU KISIM ARTIK setTeme'den SONRA ÇALIŞIR)
        SharedPreferences prefs = getSharedPreferences("bluechat_prefs", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        boolean followSystem = prefs.getBoolean("follow_system_theme", false);

        int mode = AppCompatDelegate.MODE_NIGHT_NO; // default to light
        if (followSystem) {
            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        } else if (darkMode) {
            mode = AppCompatDelegate.MODE_NIGHT_YES;
        }
        AppCompatDelegate.setDefaultNightMode(mode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        checkBluetoothPermissions();
        setupAnimations();
        setupClickListeners();
        registerBluetoothReceiver();
    }

    private void initViews() {
        btnScan = findViewById(R.id.scan_button);

        // Initialize new views
        statusIcon = findViewById(R.id.status_indicator);
        statusText = findViewById(R.id.status_text);
        statusSubtext = findViewById(R.id.bluetooth_desc);

        bluetoothToggle = findViewById(R.id.bluetooth_toggle);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    }

    private void setupAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);

        btnScan.startAnimation(slideUp);

        updateBluetoothStatus();
    }

    private void pulseAnimation(View view) {
        view.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .alpha(0.5f)
            .setDuration(1000)
            .withEndAction(() -> {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(1000)
                    .withEndAction(() -> pulseAnimation(view))
                    .start();
            })
            .start();
    }

    private void setupClickListeners() {
        btnScan.setOnClickListener(v -> {
            animateButton(v);
            if (checkBluetooth()) {
                startActivityForResult(new Intent(this, DeviceSelectionActivity.class), REQUEST_CONNECT_DEVICE);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

        // Bluetooth toggle click
        if (cardBluetooth != null) {
            cardBluetooth.setOnClickListener(v -> {
                if (bluetoothAdapter != null) {
                    if (bluetoothAdapter.isEnabled()) {
                        // Turn off Bluetooth
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            bluetoothAdapter.disable();
                        }
                    } else {
                        // Turn on Bluetooth
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                }
            });
        }

        // Visibility toggle click
        if (cardVisibility != null) {
            cardVisibility.setOnClickListener(v -> {
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                    makeDeviceDiscoverable();
                } else {
                    Toast.makeText(this, "Önce Bluetooth'u açın", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Settings click
        if (cardSettings != null) {
            cardSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }

        // Paired devices click
        if (cardPaired != null) {
            cardPaired.setOnClickListener(v -> {
                startActivity(new Intent(this, DeviceListActivity.class));
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }

        // See all text click
        if (seeAllText != null) {
            seeAllText.setOnClickListener(v -> {
                startActivity(new Intent(this, DeviceListActivity.class));
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }
    }

    private void animateButton(View button) {
        button.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction(() -> {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start();
            })
            .start();
    }

    private boolean checkBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth desteklenmiyor", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermissions();
                return false;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
        return true;
    }

    private void makeDeviceDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) 
            != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermissions();
            return;
        }
        startActivity(discoverableIntent);
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            };
            boolean allGranted = true;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS);
            }
        } else {
            String[] permissions = new String[] {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
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
            }
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
        }
    }

    private void updateBluetoothStatus() {
        // Update status icon and text based on Bluetooth state
        if (statusIcon != null && statusText != null && statusSubtext != null) {
            if (bluetoothAdapter == null) {
                statusIcon.setBackgroundResource(R.drawable.status_inactive);
                statusText.setText("Bluetooth Desteklenmiyor");
                statusSubtext.setText("Bu cihaz Bluetooth desteklemiyor");
                if (visibilityStatus != null) visibilityStatus.setText("Desteklenmiyor");
            } else if (bluetoothAdapter.isEnabled()) {
                statusIcon.setBackgroundResource(R.drawable.status_active);
                statusText.setText("Bluetooth Açık");
                statusSubtext.setText("Cihazlarınızı taramaya başlayın");
                if (visibilityStatus != null) visibilityStatus.setText("Açık");
            } else {
                statusIcon.setBackgroundResource(R.drawable.status_inactive);
                statusText.setText("Bluetooth Kapalı");
                statusSubtext.setText("Bluetooth'u açmak için dokunun");
                if (visibilityStatus != null) visibilityStatus.setText("Kapalı");
            }
        }

        // Update paired devices count
        if (pairedCount != null && bluetoothAdapter != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    int pairedCountInt = bluetoothAdapter.getBondedDevices().size();
                    pairedCount.setText(pairedCountInt + " Cihaz");
                }
            } catch (SecurityException e) {
                pairedCount.setText("0 Cihaz");
            }
        }
    }

    private void registerBluetoothReceiver() {
        bluetoothStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    runOnUiThread(() -> updateBluetoothStatus());
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBluetoothStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothStateReceiver != null) {
            unregisterReceiver(bluetoothStateReceiver);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth açıldı", Toast.LENGTH_SHORT).show();
                // Bluetooth açıldıktan sonra cihaz seçimine geç
                Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(intent, REQUEST_SELECT_DEVICE);
            } else {
                Toast.makeText(this, "Bluetooth açılamadı", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_DISCOVERABLE_BT) {
            if (resultCode > 0) {
                String name;
                try { name = bluetoothAdapter.getName(); } catch (SecurityException e) { name = "Bu cihaz"; }
                if (name == null || name.trim().isEmpty()) name = "Bu cihaz";
                Toast.makeText(this, "Diğer cihazlar tarafından görünüyorsunuz (" + resultCode + " sn)\nAd: " + name + "\nBağlantı bekleniyor...", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Görünür yapılamadı", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_SELECT_DEVICE) {
            if (resultCode == RESULT_OK && data != null) {
                String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                if (address != null && !address.isEmpty()) {
                    Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
                    chatIntent.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, address);
                    Toast.makeText(this, "Bağlantı kuruluyor...", Toast.LENGTH_SHORT).show();
                    startActivity(chatIntent);
                } else {
                    Toast.makeText(this, "Cihaz adresi alınamadı", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_CONNECT_DEVICE) {
            if (resultCode == RESULT_OK && data != null) {
                String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                if (address != null && !address.isEmpty()) {
                    Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
                    chatIntent.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, address);
                    Toast.makeText(this, "Bağlantı kuruluyor...", Toast.LENGTH_SHORT).show();
                    startActivity(chatIntent);
                } else {
                    Toast.makeText(this, "Cihaz adresi alınamadı", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Bluetooth izinleri verildi", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth izinleri gerekli!", Toast.LENGTH_LONG).show();
            }
        }
    }
}