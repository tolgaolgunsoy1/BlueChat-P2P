package com.example.bluechat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private WebView webView;
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver bluetoothStateReceiver;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                updateBluetoothStatus();
            }
        });

        webView.loadDataWithBaseURL(null, getHtmlContent(), "text/html", "UTF-8", null);

        bluetoothStateReceiver = new BluetoothStateReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);
    }

    private void updateBluetoothStatus() {
        if (bluetoothAdapter != null) {
            boolean isEnabled = bluetoothAdapter.isEnabled();
            webView.evaluateJavascript("updateBluetoothStatus(" + isEnabled + ")", null);
        }
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            };
            ActivityCompat.requestPermissions(this, permissions, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void toggleBluetooth() {
            runOnUiThread(() -> {
                if (bluetoothAdapter == null) {
                    showToast("Bluetooth desteklenmiyor");
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(LoginActivity.this,
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        requestBluetoothPermissions();
                        return;
                    }
                }

                if (bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            });
        }

        @JavascriptInterface
        public boolean isBluetoothEnabled() {
            return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        }

        @JavascriptInterface
        public void openDeviceSearch() {
            runOnUiThread(() -> {
                if (!isBluetoothEnabled()) {
                    showToast("Önce Bluetooth'u etkinleştirin");
                    return;
                }
                // Cihaz arama aktivitesine geç
                Intent intent = new Intent(LoginActivity.this, DeviceSelectionActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        @JavascriptInterface
        public void openSettings() {
            runOnUiThread(() -> {
                // Ayarlar aktivitesine geç
                Intent intent = new Intent(LoginActivity.this, SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        @JavascriptInterface
        public void openChat() {
            runOnUiThread(() -> {
                if (!isBluetoothEnabled()) {
                    showToast("Önce Bluetooth'u etkinleştirin");
                    return;
                }
                // Always go to the chat list screen
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> {
                android.widget.Toast.makeText(LoginActivity.this, message,
                    android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            updateBluetoothStatus();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            updateBluetoothStatus();
        }
    }

    private class BluetoothStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                boolean isEnabled = state == BluetoothAdapter.STATE_ON;
                runOnUiThread(() -> {
                    if (webView != null) {
                        webView.evaluateJavascript("updateBluetoothStatus(" + isEnabled + ")", null);
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothStateReceiver != null) {
            unregisterReceiver(bluetoothStateReceiver);
        }
    }

    private String getHtmlContent() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"tr\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Blue Chat</title>\n" +
                "    <style>\n" +
                "        * {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            box-sizing: border-box;\n" +
                "        }\n" +
                "\n" +
                "        body {\n" +
                "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;\n" +
                "            background: #FFFFFF;\n" +
                "            min-height: 100vh;\n" +
                "            padding: 20px;\n" +
                "            position: relative;\n" +
                "            overflow-x: hidden;\n" +
                "        }\n" +
                "\n" +
                "        .background {\n" +
                "            position: fixed;\n" +
                "            width: 100%;\n" +
                "            height: 100%;\n" +
                "            top: 0;\n" +
                "            left: 0;\n" +
                "            background: \n" +
                "                radial-gradient(circle at 20% 20%, rgba(219, 234, 254, 0.4) 0%, transparent 50%),\n" +
                "                radial-gradient(circle at 80% 80%, rgba(191, 219, 254, 0.3) 0%, transparent 50%);\n" +
                "            z-index: 0;\n" +
                "        }\n" +
                "\n" +
                "        .container {\n" +
                "            position: relative;\n" +
                "            z-index: 1;\n" +
                "            max-width: 500px;\n" +
                "            margin: 0 auto;\n" +
                "        }\n" +
                "\n" +
                "        .header {\n" +
                "            text-align: center;\n" +
                "            margin-bottom: 40px;\n" +
                "            padding-top: 20px;\n" +
                "        }\n" +
                "\n" +
                "        .logo {\n" +
                "            width: 100px;\n" +
                "            height: 100px;\n" +
                "            margin: 0 auto 20px;\n" +
                "            background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);\n" +
                "            border-radius: 30px;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "            box-shadow: 0 10px 40px rgba(59, 130, 246, 0.3);\n" +
                "            animation: logoFloat 3s ease-in-out infinite;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes logoFloat {\n" +
                "            0%, 100% { transform: translateY(0); }\n" +
                "            50% { transform: translateY(-10px); }\n" +
                "        }\n" +
                "\n" +
                "        .logo-icon {\n" +
                "            width: 50px;\n" +
                "            height: 45px;\n" +
                "            background: #FFFFFF;\n" +
                "            border-radius: 15px 15px 15px 5px;\n" +
                "            position: relative;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "            gap: 5px;\n" +
                "        }\n" +
                "\n" +
                "        .logo-dot {\n" +
                "            width: 6px;\n" +
                "            height: 6px;\n" +
                "            background: #3b82f6;\n" +
                "            border-radius: 50%;\n" +
                "        }\n" +
                "\n" +
                "        .app-title {\n" +
                "            font-size: 48px;\n" +
                "            font-weight: 800;\n" +
                "            background: linear-gradient(135deg, #1e40af 0%, #3b82f6 50%, #60a5fa 100%);\n" +
                "            -webkit-background-clip: text;\n" +
                "            -webkit-text-fill-color: transparent;\n" +
                "            background-clip: text;\n" +
                "            letter-spacing: -2px;\n" +
                "            margin-bottom: 8px;\n" +
                "        }\n" +
                "\n" +
                "        .app-subtitle {\n" +
                "            font-size: 14px;\n" +
                "            color: #64748b;\n" +
                "            font-weight: 500;\n" +
                "            letter-spacing: 2px;\n" +
                "            text-transform: uppercase;\n" +
                "        }\n" +
                "\n" +
                "        .bluetooth-card {\n" +
                "            background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);\n" +
                "            border: 2px solid #bfdbfe;\n" +
                "            border-radius: 25px;\n" +
                "            padding: 25px;\n" +
                "            margin-bottom: 30px;\n" +
                "            box-shadow: 0 4px 15px rgba(59, 130, 246, 0.1);\n" +
                "        }\n" +
                "\n" +
                "        .bluetooth-header {\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: space-between;\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "\n" +
                "        .bluetooth-title {\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            gap: 12px;\n" +
                "            font-size: 18px;\n" +
                "            font-weight: 700;\n" +
                "            color: #1e40af;\n" +
                "        }\n" +
                "\n" +
                "        .bluetooth-icon {\n" +
                "            width: 28px;\n" +
                "            height: 28px;\n" +
                "        }\n" +
                "\n" +
        "        .toggle-switch {\n" +
                "            position: relative;\n" +
                "            width: 60px;\n" +
                "            height: 32px;\n" +
                "            background: #cbd5e1;\n" +
                "            border-radius: 30px;\n" +
                "            cursor: pointer;\n" +
                "            transition: all 0.3s ease;\n" +
                "            box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);\n" +
                "        }\n" +
                "\n" +
                "        .toggle-switch.active {\n" +
                "            background: linear-gradient(135deg, #3b82f6, #2563eb);\n" +
                "            box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);\n" +
                "        }\n" +
                "\n" +
                "        .toggle-slider {\n" +
                "            position: absolute;\n" +
                "            width: 26px;\n" +
                "            height: 26px;\n" +
                "            background: #FFFFFF;\n" +
                "            border-radius: 50%;\n" +
                "            top: 3px;\n" +
                "            left: 3px;\n" +
                "            transition: all 0.3s ease;\n" +
                "            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);\n" +
                "        }\n" +
                "\n" +
                "        .toggle-switch.active .toggle-slider {\n" +
                "            left: 31px;\n" +
                "        }\n" +
                "\n" +
                "        .bluetooth-status {\n" +
                "            font-size: 14px;\n" +
                "            color: #475569;\n" +
                "            font-weight: 500;\n" +
                "        }\n" +
                "\n" +
                "        .status-badge {\n" +
                "            display: inline-block;\n" +
                "            padding: 6px 14px;\n" +
                "            border-radius: 15px;\n" +
                "            font-size: 12px;\n" +
                "            font-weight: 600;\n" +
                "            margin-left: 10px;\n" +
                "        }\n" +
                "\n" +
                "        .status-badge.active {\n" +
                "            background: #dcfce7;\n" +
                "            color: #166534;\n" +
                "        }\n" +
                "\n" +
                "        .status-badge.inactive {\n" +
                "            background: #fee2e2;\n" +
                "            color: #991b1b;\n" +
                "        }\n" +
                "\n" +
                "        .menu-grid {\n" +
                "            display: grid;\n" +
                "            grid-template-columns: 1fr;\n" +
                "            gap: 15px;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "\n" +
                "        .menu-item {\n" +
                "            background: #FFFFFF;\n" +
                "            border: 2px solid #e2e8f0;\n" +
                "            border-radius: 20px;\n" +
                "            padding: 25px;\n" +
                "            cursor: pointer;\n" +
                "            transition: all 0.3s ease;\n" +
                "            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);\n" +
                "        }\n" +
                "\n" +
                "        .menu-item:active {\n" +
                "            transform: scale(0.98);\n" +
                "            box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);\n" +
                "        }\n" +
                "\n" +
                "        .menu-item.primary {\n" +
                "            background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);\n" +
                "            border-color: #2563eb;\n" +
                "            box-shadow: 0 10px 30px rgba(59, 130, 246, 0.3);\n" +
                "        }\n" +
                "\n" +
                "        .menu-item.primary:active {\n" +
                "            box-shadow: 0 5px 15px rgba(59, 130, 246, 0.3);\n" +
                "        }\n" +
                "\n" +
                "        .menu-icon {\n" +
                "            width: 50px;\n" +
                "            height: 50px;\n" +
                "            background: #f1f5f9;\n" +
                "            border-radius: 15px;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "\n" +
                "        .menu-item.primary .menu-icon {\n" +
                "            background: rgba(255, 255, 255, 0.2);\n" +
                "        }\n" +
                "\n" +
                "        .menu-icon svg {\n" +
                "            width: 28px;\n" +
                "            height: 28px;\n" +
                "            stroke: #3b82f6;\n" +
                "        }\n" +
                "\n" +
                "        .menu-item.primary .menu-icon svg {\n" +
                "            stroke: #FFFFFF;\n" +
                "        }\n" +
                "\n" +
                "        .menu-content {\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: space-between;\n" +
                "        }\n" +
                "\n" +
                "        .menu-text h3 {\n" +
                "            font-size: 18px;\n" +
                "            font-weight: 700;\n" +
                "            color: #1e293b;\n" +
                "            margin-bottom: 5px;\n" +
                "        }\n" +
                "\n" +
                "        .menu-item.primary .menu-text h3 {\n" +
                "            color: #FFFFFF;\n" +
                "        }\n" +
                "\n" +
                "        .menu-text p {\n" +
                "            font-size: 13px;\n" +
                "            color: #64748b;\n" +
                "            font-weight: 500;\n" +
                "        }\n" +
                "\n" +
                "        .menu-item.primary .menu-text p {\n" +
                "            color: rgba(255, 255, 255, 0.8);\n" +
                "        }\n" +
                "\n" +
                "        .menu-arrow {\n" +
                "            width: 24px;\n" +
                "            height: 24px;\n" +
                "            stroke: #94a3b8;\n" +
                "        }\n" +
                "\n" +
                "        .menu-item.primary .menu-arrow {\n" +
                "            stroke: #FFFFFF;\n" +
                "        }\n" +
                "\n" +
                "        .footer {\n" +
                "            text-align: center;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "\n" +
                "        .version {\n" +
                "            display: inline-block;\n" +
                "            background: #f1f5f9;\n" +
                "            padding: 8px 16px;\n" +
                "            border-radius: 20px;\n" +
                "            font-size: 12px;\n" +
                "            font-weight: 600;\n" +
                "            color: #64748b;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"background\"></div>\n" +
                "    \n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\">\n" +
                "            <div class=\"logo\">\n" +
                "                <div class=\"logo-icon\">\n" +
                "                    <div class=\"logo-dot\"></div>\n" +
                "                    <div class=\"logo-dot\"></div>\n" +
                "                    <div class=\"logo-dot\"></div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            <div class=\"app-title\">Blue Chat</div>\n" +
                "            <div class=\"app-subtitle\">Bluetooth Mesajlaşma</div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"bluetooth-card\">\n" +
                "            <div class=\"bluetooth-header\">\n" +
                "                <div class=\"bluetooth-title\">\n" +
                "                    <svg class=\"bluetooth-icon\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.5\">\n" +
                "                        <path d=\"M6.5 6.5l11 11L12 23V1l5.5 5.5-11 11\"/>\n" +
                "                    </svg>\n" +
                "                    Bluetooth\n" +
                "                </div>\n" +
                "                <div class=\"toggle-switch\" id=\"bluetoothToggle\" onclick=\"toggleBluetooth()\">\n" +
                "                    <div class=\"toggle-slider\"></div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            <div class=\"bluetooth-status\">\n" +
                "                <span id=\"statusText\">Bluetooth'u etkinleştirin</span>\n" +
                "                <span class=\"status-badge inactive\" id=\"statusBadge\">Kapalı</span>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "\n" +
        "        <div class=\"menu-grid\">\n" +
            "            <div class=\"menu-item primary\" onclick=\"openChat()\">\n" +
                "                <div class=\"menu-icon\">\n" +
                "                    <svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.5\">\n" +
                "                        <path d=\"M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z\"/>\n" +
                "                    </svg>\n" +
                "                </div>\n" +
                "                <div class=\"menu-content\">\n" +
                "                    <div class=\"menu-text\">\n" +
                "                        <h3>Mesajlaşmaya Başla</h3>\n" +
                "                        <p>Sohbet ekranına git</p>\n" +
                "                    </div>\n" +
                "                    <svg class=\"menu-arrow\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.5\">\n" +
                "                        <path d=\"M5 12h14M12 5l7 7-7 7\"/>\n" +
                "                    </svg>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "\n" +
                "            <div class=\"menu-item\" onclick=\"openDeviceSearch()\">\n" +
                "                <div class=\"menu-icon\">\n" +
                "                    <svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.5\">\n" +
                "                        <circle cx=\"11\" cy=\"11\" r=\"8\"/>\n" +
                "                        <path d=\"M21 21l-4.35-4.35\"/>\n" +
                "                    </svg>\n" +
                "                </div>\n" +
                "                <div class=\"menu-content\">\n" +
                "                    <div class=\"menu-text\">\n" +
                "                        <h3>Cihaz Ara</h3>\n" +
                "                        <p>Bluetooth cihazları keşfet</p>\n" +
                "                    </div>\n" +
                "                    <svg class=\"menu-arrow\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.5\">\n" +
                "                        <path d=\"M5 12h14M12 5l7 7-7 7\"/>\n" +
                "                    </svg>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "\n" +
                "            <div class=\"menu-item\" onclick=\"openSettings()\">\n" +
                "                <div class=\"menu-icon\">\n" +
                "                    <svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.5\">\n" +
                "                        <circle cx=\"12\" cy=\"12\" r=\"3\"/>\n" +
                "                        <path d=\"M12 1v6m0 6v6M5.6 5.6l4.2 4.2m4.2 4.2l4.2 4.2M1 12h6m6 0h6M5.6 18.4l4.2-4.2m4.2-4.2l4.2-4.2\"/>\n" +
                "                    </svg>\n" +
                "                </div>\n" +
                "                <div class=\"menu-content\">\n" +
                "                    <div class=\"menu-text\">\n" +
                "                        <h3>Ayarlar</h3>\n" +
                "                        <p>Uygulama ayarlarını düzenle</p>\n" +
                "                    </div>\n" +
                "                    <svg class=\"menu-arrow\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.5\">\n" +
                "                        <path d=\"M5 12h14M12 5l7 7-7 7\"/>\n" +
                "                    </svg>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class=\"footer\">\n" +
                "            <div class=\"version\">v1.0.0</div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        let bluetoothEnabled = false;\n" +
                "\n" +
                "        function updateBluetoothStatus(enabled) {\n" +
                "            bluetoothEnabled = enabled;\n" +
                "            const toggle = document.getElementById('bluetoothToggle');\n" +
                "            const statusText = document.getElementById('statusText');\n" +
                "            const statusBadge = document.getElementById('statusBadge');\n" +
                "            \n" +
                "            if (enabled) {\n" +
                "                toggle.classList.add('active');\n" +
                "                statusText.textContent = 'Bluetooth aktif ve hazır';\n" +
                "                statusBadge.textContent = 'Açık';\n" +
                "                statusBadge.classList.remove('inactive');\n" +
                "                statusBadge.classList.add('active');\n" +
                "            } else {\n" +
                "                toggle.classList.remove('active');\n" +
                "                statusText.textContent = 'Bluetooth\\'u etkinleştirin';\n" +
                "                statusBadge.textContent = 'Kapalı';\n" +
                "                statusBadge.classList.remove('active');\n" +
                "                statusBadge.classList.add('inactive');\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
        "        function toggleBluetooth() {\n" +
                "            const toggle = document.getElementById('bluetoothToggle');\n" +
                "            if (window.Android && window.Android.toggleBluetooth) {\n" +
                "                toggle.classList.add('disabled');\n" +
                "                window.Android.toggleBluetooth();\n" +
                "                setTimeout(() => {\n" +
                "                    if (window.Android && window.Android.isBluetoothEnabled) {\n" +
                "                        updateBluetoothStatus(window.Android.isBluetoothEnabled());\n" +
                "                        toggle.classList.remove('disabled');\n" +
                "                    }\n" +
                "                }, 500);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function openChat() {\n" +
                "            if (!bluetoothEnabled) {\n" +
                "                if (window.Android && window.Android.showToast) {\n" +
                "                    window.Android.showToast('Önce Bluetooth\\'u etkinleştirin');\n" +
                "                }\n" +
                "                return;\n" +
                "            }\n" +
                "            if (window.Android && window.Android.openChat) {\n" +
                "                window.Android.openChat();\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function openDeviceSearch() {\n" +
                "            if (window.Android && window.Android.openDeviceSearch) {\n" +
                "                window.Android.openDeviceSearch();\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function openSettings() {\n" +
                "            if (window.Android && window.Android.openSettings) {\n" +
                "                window.Android.openSettings();\n" +
                "            }\n" +
                "        }\n" +
                "    </script>\n" +
                "</html>";
    }
}
