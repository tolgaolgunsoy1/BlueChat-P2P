
package com.example.bluechat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS = "bluechat_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_AUTO_RECONNECT = "auto_reconnect";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar_settings);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setTitle(R.string.settings_title);
            toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // Dark mode switch
        Switch themeSwitch = findViewById(R.id.switch_theme);
        boolean dark = prefs.getBoolean(KEY_DARK_MODE, false);
        themeSwitch.setChecked(dark);
        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
                applyTheme(isChecked);
            }
        });

        // Notifications switch
        Switch notificationsSwitch = findViewById(R.id.switch_notifications);
        if (notificationsSwitch != null) {
            boolean notifications = prefs.getBoolean(KEY_NOTIFICATIONS, true);
            notificationsSwitch.setChecked(notifications);
            notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
                if (isChecked) {
                    startNotificationService();
                } else {
                    stopNotificationService();
                }
            });
        }

        // Auto reconnect switch
        Switch autoReconnectSwitch = findViewById(R.id.switch_auto_reconnect);
        if (autoReconnectSwitch != null) {
            boolean autoReconnect = prefs.getBoolean(KEY_AUTO_RECONNECT, true);
            autoReconnectSwitch.setChecked(autoReconnect);
            autoReconnectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_AUTO_RECONNECT, isChecked).apply();
            });
        }
    }

    private void applyTheme(boolean darkMode) {
        int mode = darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

        // Check if system theme is preferred
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean followSystem = prefs.getBoolean("follow_system_theme", false);
        if (followSystem) {
            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }

        AppCompatDelegate.setDefaultNightMode(mode);

        // Smooth transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void startNotificationService() {
        Intent serviceIntent = new Intent(this, BluetoothNotificationService.class);
        startForegroundService(serviceIntent);
    }

    private void stopNotificationService() {
        Intent serviceIntent = new Intent(this, BluetoothNotificationService.class);
        stopService(serviceIntent);
    }
}
