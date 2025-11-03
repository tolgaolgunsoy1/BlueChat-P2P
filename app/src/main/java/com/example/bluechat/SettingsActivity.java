package com.example.bluechat;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {

    // UI Components - Header
    private Button backButton;

    // Profile Section
    private LinearLayout profileCard;

    // Account Settings Items
    private LinearLayout profileEditItem;
    private LinearLayout privacyItem;
    private LinearLayout passwordItem;

    // Preferences Items
    private LinearLayout notificationsItem;
    private LinearLayout darkModeItem;
    private LinearLayout chatSettingsItem;
    private LinearLayout languageItem;

    // Switches
    private Switch notificationSwitch;
    private Switch darkModeSwitch;

    // About Items
    private LinearLayout appInfoItem;
    private LinearLayout helpItem;
    private LinearLayout rateItem;

    // Danger Zone Items
    private LinearLayout logoutItem;
    private LinearLayout deleteAccountItem;

    // SharedPreferences
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "BlueChatSettings";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    private static final String KEY_LANGUAGE = "language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize all views
        initializeViews();

        // Setup all click listeners
        setupClickListeners();

        // Setup switch listeners
        setupSwitchListeners();

        // Load saved preferences
        loadSavedPreferences();

        // Apply entrance animations
        animateEntranceItems();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        // Header
        backButton = findViewById(R.id.back_button);

        // Profile
        profileCard = findViewById(R.id.profile_card);

        // Account Group
        profileEditItem = findViewById(R.id.profile_edit_item);
        privacyItem = findViewById(R.id.privacy_item);
        passwordItem = findViewById(R.id.password_item);

        // Preferences Group
        notificationsItem = findViewById(R.id.notifications_item);
        darkModeItem = findViewById(R.id.dark_mode_item);
        chatSettingsItem = findViewById(R.id.chat_settings_item);
        languageItem = findViewById(R.id.language_item);

        // Switches
        notificationSwitch = findViewById(R.id.switch_notifications);
        darkModeSwitch = findViewById(R.id.switch_theme);

        // About Group
        appInfoItem = findViewById(R.id.app_info_item);
        helpItem = findViewById(R.id.help_item);
        rateItem = findViewById(R.id.rate_item);

        // Danger Zone
        logoutItem = findViewById(R.id.logout_item);
        deleteAccountItem = findViewById(R.id.delete_account_item);
    }

    /**
     * Setup click listeners for all items
     */
    private void setupClickListeners() {
        // Back button
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                animateButtonClick(v);
                onBackPressed();
            });
        }

        // Profile card
        if (profileCard != null) {
            profileCard.setOnClickListener(v -> {
                animateItemClick(v);
                openProfileEdit();
            });
        }

        // Account Settings
        if (profileEditItem != null) {
            profileEditItem.setOnClickListener(v -> {
                animateItemClick(v);
                openProfileEdit();
            });
        }

        if (privacyItem != null) {
            privacyItem.setOnClickListener(v -> {
                animateItemClick(v);
                openPrivacySettings();
            });
        }

        if (passwordItem != null) {
            passwordItem.setOnClickListener(v -> {
                animateItemClick(v);
                openPasswordChange();
            });
        }

        // Preferences (non-switch items)
        if (chatSettingsItem != null) {
            chatSettingsItem.setOnClickListener(v -> {
                animateItemClick(v);
                openChatSettings();
            });
        }

        if (languageItem != null) {
            languageItem.setOnClickListener(v -> {
                animateItemClick(v);
                openLanguageSettings();
            });
        }

        // About
        if (appInfoItem != null) {
            appInfoItem.setOnClickListener(v -> {
                animateItemClick(v);
                openAppInfo();
            });
        }

        if (helpItem != null) {
            helpItem.setOnClickListener(v -> {
                animateItemClick(v);
                openHelp();
            });
        }

        if (rateItem != null) {
            rateItem.setOnClickListener(v -> {
                animateItemClick(v);
                openRateApp();
            });
        }

        // Danger Zone
        if (logoutItem != null) {
            logoutItem.setOnClickListener(v -> {
                animateItemClick(v);
                showLogoutDialog();
            });
        }

        if (deleteAccountItem != null) {
            deleteAccountItem.setOnClickListener(v -> {
                animateItemClick(v);
                showDeleteAccountDialog();
            });
        }

        // Parent items for switches (to toggle when clicking the row)
        if (notificationsItem != null) {
            notificationsItem.setOnClickListener(v -> {
                if (notificationSwitch != null) {
                    notificationSwitch.setChecked(!notificationSwitch.isChecked());
                }
            });
        }

        if (darkModeItem != null) {
            darkModeItem.setOnClickListener(v -> {
                if (darkModeSwitch != null) {
                    darkModeSwitch.setChecked(!darkModeSwitch.isChecked());
                }
            });
        }
    }

    /**
     * Setup switch listeners
     */
    private void setupSwitchListeners() {
        if (notificationSwitch != null) {
            notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    savePreference(KEY_NOTIFICATIONS, isChecked);
                    handleNotificationToggle(isChecked);
                    showToast(isChecked ? "Bildirimler açıldı" : "Bildirimler kapatıldı");
                }
            });
        }

        if (darkModeSwitch != null) {
            darkModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    savePreference(KEY_DARK_MODE, isChecked);
                    applyDarkMode(isChecked);
                    showToast(isChecked ? "Karanlık mod açıldı" : "Karanlık mod kapatıldı");
                }
            });
        }
    }

    /**
     * Load saved preferences from SharedPreferences
     */
    private void loadSavedPreferences() {
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true);
        boolean darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false);
        String language = prefs.getString(KEY_LANGUAGE, "tr");

        if (notificationSwitch != null) {
            notificationSwitch.setChecked(notificationsEnabled);
        }

        if (darkModeSwitch != null) {
            darkModeSwitch.setChecked(darkModeEnabled);
        }
    }

    /**
     * Save preference to SharedPreferences
     */
    private void savePreference(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    private void saveLanguagePreference(String language) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    /**
     * Handle notification toggle
     */
    private void handleNotificationToggle(boolean enabled) {
        Intent serviceIntent = new Intent(this, BluetoothNotificationService.class);
        if (enabled) {
            startService(serviceIntent);
        } else {
            stopService(serviceIntent);
        }
    }

    /**
     * Apply dark mode
     */
    private void applyDarkMode(boolean enabled) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        // Recreate activity to apply theme immediately
        recreate();
    }

    // ==================== ANIMATION METHODS ====================

    /**
     * Animate button click
     */
    private void animateButtonClick(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(150);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    /**
     * Animate item click
     */
    private void animateItemClick(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.98f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.98f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(100);
        animatorSet.start();
    }

    /**
     * Animate items on entrance
     */
    private void animateEntranceItems() {
        View[] items = {
            profileCard,
            profileEditItem, privacyItem, passwordItem,
            notificationsItem, darkModeItem, chatSettingsItem, languageItem,
            appInfoItem, helpItem, rateItem,
            logoutItem, deleteAccountItem
        };

        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                final View item = items[i];
                item.setAlpha(0f);
                item.setTranslationY(20f);

                item.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(i * 30L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            }
        }
    }

    // ==================== NAVIGATION METHODS ====================

    private void openProfileEdit() {
        showToast("Profil düzenleme özelliği yakında eklenecek.");
        // TODO: Implement navigation
        // Intent intent = new Intent(this, ProfileEditActivity.class);
        // startActivity(intent);
    }

    private void openPrivacySettings() {
        showToast("Gizlilik ayarları özelliği yakında eklenecek.");
        // TODO: Implement navigation
        // Intent intent = new Intent(this, PrivacySettingsActivity.class);
        // startActivity(intent);
    }

    private void openPasswordChange() {
        showToast("Şifre değiştirme özelliği yakında eklenecek.");
        // TODO: Implement navigation
        // Intent intent = new Intent(this, PasswordChangeActivity.class);
        // startActivity(intent);
    }

    private void openChatSettings() {
        showToast("Sohbet ayarları özelliği yakında eklenecek.");
        // TODO: Implement navigation
        // Intent intent = new Intent(this, ChatSettingsActivity.class);
        // startActivity(intent);
    }

    private void openLanguageSettings() {
        String[] languages = {"Türkçe", "English"};
        String currentLanguage = prefs.getString(KEY_LANGUAGE, "tr");
        int selected = currentLanguage.equals("en") ? 1 : 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dil Seçin");
        builder.setSingleChoiceItems(languages, selected, (dialog, which) -> {
            String newLanguage = which == 0 ? "tr" : "en";
            saveLanguagePreference(newLanguage);
            showToast("Dil değiştirildi: " + languages[which]);
            dialog.dismiss();
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }

    private void openAppInfo() {
        showToast("Uygulama bilgisi özelliği yakında eklenecek.");
        // TODO: Implement navigation
        // Intent intent = new Intent(this, AppInfoActivity.class);
        // startActivity(intent);
    }

    private void openHelp() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/help"));
        startActivity(intent);
    }

    private void openRateApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
            startActivity(intent);
        }
    }

    // ==================== DIALOG METHODS ====================

    /**
     * Show logout confirmation dialog
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Çıkış Yap")
            .setMessage("Hesabınızdan çıkış yapmak istediğinizden emin misiniz?")
            .setPositiveButton("Çıkış Yap", (dialog, which) -> performLogout())
            .setNegativeButton("İptal", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    /**
     * Show delete account confirmation dialog
     */
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Hesabı Sil")
            .setMessage("Hesabınızı kalıcı olarak silmek istediğinizden emin misiniz?\n\nBu işlem geri alınamaz ve tüm verileriniz silinecektir!")
            .setPositiveButton("Sil", (dialog, which) -> confirmDeleteAccount())
            .setNegativeButton("İptal", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    /**
     * Second confirmation for account deletion
     */
    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
            .setTitle("Son Onay")
            .setMessage("Bu işlem geri alınamaz. Devam etmek istediğinizden EMİN MİSİNİZ?")
            .setPositiveButton("Evet, Sil", (dialog, which) -> performDeleteAccount())
            .setNegativeButton("Hayır", null)
            .show();
    }

    /**
     * Perform logout
     */
    private void performLogout() {
        showToast("Çıkış yapılıyor...");

        // Clear user data
        prefs.edit().clear().apply();

        // TODO: Clear user session, tokens, etc.

        // Navigate to login screen
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        finish();
    }

    /**
     * Perform account deletion
     */
    private void performDeleteAccount() {
        showToast("Hesap siliniyor...");

        // TODO: Make API call to delete account
        // TODO: Clear all local data

        prefs.edit().clear().apply();

        // Navigate to login or welcome screen
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Show toast message
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload preferences in case they changed
        loadSavedPreferences();
    }
}
