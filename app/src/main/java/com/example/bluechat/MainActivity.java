
 
package com.example.bluechat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver bluetoothStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Apply theme from preferences before super.onCreate
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

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient());

        webView.loadUrl("file:///android_asset/chat_list_web.html");

        // Load chat list after page loads
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                new LoadChatListTask().execute();
            }
        });

        bluetoothStateReceiver = new BluetoothStateReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void goBack() {
            runOnUiThread(() -> {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        @JavascriptInterface
        public void openDeviceSearch() {
            runOnUiThread(() -> {
                if (!isBluetoothEnabled()) {
                    showToast("Önce Bluetooth'u etkinleştirin");
                    return;
                }
                Intent intent = new Intent(MainActivity.this, DeviceSelectionActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        @JavascriptInterface
        public void openChat(String deviceAddress) {
            runOnUiThread(() -> {
                if (!isBluetoothEnabled()) {
                    showToast("Önce Bluetooth'u etkinleştirin");
                    return;
                }
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, deviceAddress);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        @JavascriptInterface
        public boolean isBluetoothEnabled() {
            return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        }

        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> {
                android.widget.Toast.makeText(MainActivity.this, message,
                    android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        @JavascriptInterface
        public void getChatList() {
            new LoadChatListTask().execute();
        }
    }

    private class BluetoothStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // Could update UI if needed
            }
        }
    }

    private class LoadChatListTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                AppDatabase database = DatabaseHelper.getInstance(MainActivity.this);
                List<ChatSummary> summaries = database.messageDao().getChatSummaries();

                JSONArray chatArray = new JSONArray();
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

                for (ChatSummary summary : summaries) {
                    MessageEntity lastMessage = database.messageDao().getLastMessageForDevice(summary.getDeviceAddress());

                    JSONObject chatObj = new JSONObject();
                    chatObj.put("deviceAddress", summary.getDeviceAddress());
                    chatObj.put("deviceName", getDeviceName(summary.getDeviceAddress()));
                    chatObj.put("lastMessage", lastMessage != null ? lastMessage.getContent() : "");
                    chatObj.put("timestamp", summary.getLastTimestamp());
                    chatObj.put("messageCount", summary.getMessageCount());
                    chatObj.put("isOnline", isDeviceOnline(summary.getDeviceAddress()));

                    // Format time
                    Date date = new Date(summary.getLastTimestamp());
                    long now = System.currentTimeMillis();
                    long diff = now - summary.getLastTimestamp();

                    String timeStr;
                    if (diff < 24 * 60 * 60 * 1000) { // Less than 24 hours
                        timeStr = timeFormat.format(date);
                    } else if (diff < 7 * 24 * 60 * 60 * 1000) { // Less than 7 days
                        timeStr = dateFormat.format(date);
                    } else {
                        timeStr = "Eski";
                    }
                    chatObj.put("timeDisplay", timeStr);

                    chatArray.put(chatObj);
                }

                return chatArray.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "[]";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            runOnUiThread(() -> {
                webView.evaluateJavascript("javascript:updateChatList(" + result + ")", null);
            });
        }
    }

    private String getDeviceName(String deviceAddress) {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                android.bluetooth.BluetoothDevice device = adapter.getRemoteDevice(deviceAddress);
                String name = device.getName();
                return name != null ? name : deviceAddress.substring(0, 8);
            }
        } catch (Exception e) {
            // Ignore
        }
        return deviceAddress.substring(0, 8);
    }

    private boolean isDeviceOnline(String deviceAddress) {
        // For now, assume devices are offline. In future, could check connected devices
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothStateReceiver != null) {
            unregisterReceiver(bluetoothStateReceiver);
        }
    }

    private String getChatHtmlContent() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"tr\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Blue Chat - Sohbetler</title>\n" +
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
                "            height: 100vh;\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "\n" +
                "        /* Background gradient */\n" +
                "        .background {\n" +
                "            position: fixed;\n" +
                "            width: 100%;\n" +
                "            height: 100%;\n" +
                "            background: \n" +
                "                radial-gradient(circle at 20% 20%, rgba(219, 234, 254, 0.4) 0%, transparent 50%),\n" +
                "                radial-gradient(circle at 80% 80%, rgba(191, 219, 254, 0.3) 0%, transparent 50%);\n" +
                "            z-index: 0;\n" +
                "        }\n" +
                "\n" +
                "        /* Header */\n" +
                "        .header {\n" +
                "            position: relative;\n" +
                "            background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);\n" +
                "            color: white;\n" +
                "            padding: 50px 24px 30px;\n" +
                "            border-radius: 0 0 35px 35px;\n" +
                "            box-shadow: 0 10px 40px rgba(59, 130, 246, 0.3);\n" +
                "            z-index: 10;\n" +
                "        }\n" +
                "\n" +
                "        .header-top {\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            align-items: center;\n" +
                "            margin-bottom: 8px;\n" +
                "        }\n" +
                "\n" +
                "        .back-button {\n" +
                "            width: 40px;\n" +
                "            height: 40px;\n" +
                "            border-radius: 12px;\n" +
                "            background: rgba(255, 255, 255, 0.2);\n" +
                "            border: none;\n" +
                "            color: white;\n" +
                "            font-size: 20px;\n" +
                "            cursor: pointer;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "            transition: all 0.3s ease;\n" +
                "        }\n" +
                "\n" +
                "        .back-button:hover {\n" +
                "            background: rgba(255, 255, 255, 0.3);\n" +
                "            transform: scale(1.05);\n" +
                "        }\n" +
                "\n" +
                "        .header-actions {\n" +
                "            display: flex;\n" +
                "            gap: 10px;\n" +
                "        }\n" +
                "\n" +
                "        .header-button {\n" +
                "            width: 40px;\n" +
                "            height: 40px;\n" +
                "            border-radius: 12px;\n" +
                "            background: rgba(255, 255, 255, 0.2);\n" +
                "            border: none;\n" +
                "            color: white;\n" +
                "            font-size: 18px;\n" +
                "            cursor: pointer;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "            transition: all 0.3s ease;\n" +
                "        }\n" +
                "\n" +
                "        .header-button:hover {\n" +
                "            background: rgba(255, 255, 255, 0.3);\n" +
                "            transform: scale(1.05);\n" +
                "        }\n" +
                "\n" +
                "        .header-title {\n" +
                "            font-size: 36px;\n" +
                "            font-weight: 800;\n" +
                "            margin-bottom: 6px;\n" +
                "            letter-spacing: -1px;\n" +
                "        }\n" +
                "\n" +
                "        .header-subtitle {\n" +
                "            font-size: 15px;\n" +
                "            opacity: 0.9;\n" +
                "            font-weight: 500;\n" +
                "        }\n" +
                "\n" +
                "        /* Search Box */\n" +
                "        .search-container {\n" +
                "            padding: 20px 24px 16px;\n" +
                "            position: relative;\n" +
                "            z-index: 5;\n" +
                "        }\n" +
                "\n" +
                "        .search-box {\n" +
                "            background: white;\n" +
                "            border-radius: 18px;\n" +
                "            padding: 14px 18px;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            gap: 12px;\n" +
                "            box-shadow: 0 4px 16px rgba(59, 130, 246, 0.1);\n" +
                "            border: 2px solid transparent;\n" +
                "            transition: all 0.3s ease;\n" +
                "        }\n" +
                "\n" +
                "        .search-box:focus-within {\n" +
                "            border-color: #3b82f6;\n" +
                "            box-shadow: 0 6px 24px rgba(59, 130, 246, 0.2);\n" +
                "        }\n" +
                "\n" +
                "        .search-icon {\n" +
                "            font-size: 20px;\n" +
                "            color: #94a3b8;\n" +
                "        }\n" +
                "\n" +
                "        .search-input {\n" +
                "            border: none;\n" +
                "            outline: none;\n" +
                "            flex: 1;\n" +
                "            font-size: 15px;\n" +
                "            color: #1e293b;\n" +
                "            background: transparent;\n" +
                "        }\n" +
                "\n" +
                "        .search-input::placeholder {\n" +
                "            color: #cbd5e1;\n" +
                "        }\n" +
                "\n" +
                "        /* Chat List */\n" +
                "        .chat-list {\n" +
                "            padding: 0 20px 20px;\n" +
                "            height: calc(100vh - 250px);\n" +
                "            overflow-y: auto;\n" +
                "            position: relative;\n" +
                "            z-index: 5;\n" +
                "        }\n" +
                "\n" +
                "        .chat-list::-webkit-scrollbar {\n" +
                "            width: 0;\n" +
                "        }\n" +
                "\n" +
                "        .chat-item {\n" +
                "            background: white;\n" +
                "            border-radius: 22px;\n" +
                "            padding: 18px;\n" +
                "            margin-bottom: 14px;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            gap: 16px;\n" +
                "            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);\n" +
                "            border: 1px solid rgba(59, 130, 246, 0.08);\n" +
                "            cursor: pointer;\n" +
                "            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);\n" +
                "            animation: slideUp 0.5s ease-out backwards;\n" +
                "        }\n" +
                "\n" +
                "        .chat-item:nth-child(1) { animation-delay: 0.1s; }\n" +
                "        .chat-item:nth-child(2) { animation-delay: 0.2s; }\n" +
                "        .chat-item:nth-child(3) { animation-delay: 0.3s; }\n" +
                "        .chat-item:nth-child(4) { animation-delay: 0.4s; }\n" +
                "        .chat-item:nth-child(5) { animation-delay: 0.5s; }\n" +
                "\n" +
                "        @keyframes slideUp {\n" +
                "            from {\n" +
                "                opacity: 0;\n" +
                "                transform: translateY(20px);\n" +
                "            }\n" +
                "            to {\n" +
                "                opacity: 1;\n" +
                "                transform: translateY(0);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        .chat-item:active {\n" +
                "            transform: scale(0.98);\n" +
                "        }\n" +
                "\n" +
                "        .chat-item:hover {\n" +
                "            transform: translateX(4px);\n" +
                "            box-shadow: 0 8px 24px rgba(59, 130, 246, 0.15);\n" +
                "            border-color: rgba(59, 130, 246, 0.2);\n" +
                "        }\n" +
                "\n" +
                "        .chat-avatar {\n" +
                "            width: 58px;\n" +
                "            height: 58px;\n" +
                "            border-radius: 18px;\n" +
                "            background: linear-gradient(135deg, #3b82f6, #60a5fa);\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "            color: white;\n" +
                "            font-weight: 700;\n" +
                "            font-size: 22px;\n" +
                "            flex-shrink: 0;\n" +
                "            box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);\n" +
                "            position: relative;\n" +
                "        }\n" +
                "\n" +
                "        .online-indicator {\n" +
                "            position: absolute;\n" +
                "            bottom: 2px;\n" +
                "            right: 2px;\n" +
                "            width: 14px;\n" +
                "            height: 14px;\n" +
                "            background: #10b981;\n" +
                "            border: 3px solid white;\n" +
                "            border-radius: 50%;\n" +
                "            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);\n" +
                "        }\n" +
                "\n" +
                "        .chat-info {\n" +
                "            flex: 1;\n" +
                "            min-width: 0;\n" +
                "        }\n" +
                "\n" +
                "        .chat-name {\n" +
                "            font-size: 17px;\n" +
                "            font-weight: 700;\n" +
                "            color: #1e293b;\n" +
                "        }\n" +
                "\n" +
                "        .chat-last-message {\n" +
                "            font-size: 14px;\n" +
                "            color: #64748b;\n" +
                "            margin-top: 2px;\n" +
                "            overflow: hidden;\n" +
                "            text-overflow: ellipsis;\n" +
                "            white-space: nowrap;\n" +
                "        }\n" +
                "\n" +
                "        .chat-time {\n" +
                "            font-size: 12px;\n" +
                "            color: #94a3b8;\n" +
                "            margin-top: 4px;\n" +
                "        }\n" +
                "\n" +
                "        .unread-count {\n" +
                "            background: #3b82f6;\n" +
                "            color: white;\n" +
                "            border-radius: 12px;\n" +
                "            padding: 4px 8px;\n" +
                "            font-size: 12px;\n" +
                "            font-weight: 600;\n" +
                "            margin-left: auto;\n" +
                "            align-self: flex-start;\n" +
                "        }\n" +
                "    </style>\n" +
                "    <body>\n" +
                "        <div class=\"background\"></div>\n" +
                "        <div class=\"header\">\n" +
                "            <div class=\"header-top\">\n" +
                "                <button class=\"back-button\" onclick=\"Android.goBack()\">&#x2039;</button>\n" +
                "                <div class=\"header-actions\">\n" +
                "                    <button class=\"header-button\" onclick=\"Android.openDeviceSearch()\">+</button>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            <h1 class=\"header-title\">Sohbetler</h1>\n" +
                "            <p class=\"header-subtitle\">Mesajlarınıza hızlı erişim</p>\n" +
                "        </div>\n" +
                "        <div class=\"search-container\">\n" +
                "            <div class=\"search-box\">\n" +
                "                <span class=\"search-icon\">🔍</span>\n" +
                "                <input type=\"text\" class=\"search-input\" placeholder=\"Sohbet ara...\" oninput=\"filterChats(this.value)\">\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div class=\"chat-list\" id=\"chatList\">\n" +
                "            <!-- Dynamic chat items -->\n" +
                "        </div>\n" +
                "    </body>\n" +
                "    <script>\n" +
                "        let chats = [];\n" +
                "\n" +
                "        function updateChatList(jsonData) {\n" +
                "            try {\n" +
                "                chats = JSON.parse(jsonData);\n" +
                "                renderChats();\n" +
                "            } catch (e) {\n" +
                "                console.error('JSON parse error:', e);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function renderChats() {\n" +
                "            const container = document.getElementById('chatList');\n" +
                "            container.innerHTML = '';\n" +
                "            chats.forEach((chat, index) => {\n" +
                "                const item = document.createElement('div');\n" +
                "                item.className = 'chat-item';\n" +
                "                item.style.animationDelay = (index * 0.1) + 's';\n" +
                "                item.onclick = () => Android.openChat(chat.deviceAddress);\n" +
                "\n" +
                "                // Avatar\n" +
                "                const avatar = document.createElement('div');\n" +
                "                avatar.className = 'chat-avatar';\n" +
                "                avatar.textContent = chat.deviceName.charAt(0).toUpperCase();\n" +
                "                if (chat.isOnline) {\n" +
                "                    const dot = document.createElement('div');\n" +
                "                    dot.className = 'online-indicator';\n" +
                "                    avatar.appendChild(dot);\n" +
                "                }\n" +
                "\n" +
                "                // Info\n" +
                "                const info = document.createElement('div');\n" +
                "                info.className = 'chat-info';\n" +
                "\n" +
                "                const nameEl = document.createElement('div');\n" +
                "                nameEl.className = 'chat-name';\n" +
                "                nameEl.textContent = chat.deviceName;\n" +
                "\n" +
                "                const msgEl = document.createElement('div');\n" +
                "                msgEl.className = 'chat-last-message';\n" +
                "                msgEl.textContent = chat.lastMessage || 'Henüz mesaj yok';\n" +
                "\n" +
                "                const timeEl = document.createElement('div');\n" +
                "                timeEl.className = 'chat-time';\n" +
                "                timeEl.textContent = chat.timeDisplay;\n" +
                "\n" +
                "                info.appendChild(nameEl);\n" +
                "                info.appendChild(msgEl);\n" +
                "                info.appendChild(timeEl);\n" +
                "\n" +
                "                item.appendChild(avatar);\n" +
                "                item.appendChild(info);\n" +
                "                container.appendChild(item);\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function filterChats(query) {\n" +
                "            const filtered = chats.filter(c => \n" +
                "                c.deviceName.toLowerCase().includes(query.toLowerCase()) ||\n" +
                "                (c.lastMessage && c.lastMessage.toLowerCase().includes(query.toLowerCase()))\n" +
                "            );\n" +
                "            const container = document.getElementById('chatList');\n" +
                "            container.innerHTML = '';\n" +
                "            filtered.forEach(chat => {\n" +
                "                const item = createChatItem(chat);\n" +
                "                container.appendChild(item);\n" +
                "            });\n" +
                "        }\n" +
                "\n" +
                "        function createChatItem(chat) {\n" +
                "            const item = document.createElement('div');\n" +
                "            item.className = 'chat-item';\n" +
                "            item.onclick = () => Android.openChat(chat.deviceAddress);\n" +
                "\n" +
                "            const avatar = document.createElement('div');\n" +
                "            avatar.className = 'chat-avatar';\n" +
                "            avatar.textContent = chat.deviceName.charAt(0).toUpperCase();\n" +
                "            if (chat.isOnline) {\n" +
                "                const dot = document.createElement('div');\n" +
                "                dot.className = 'online-indicator';\n" +
                "                avatar.appendChild(dot);\n" +
                "            }\n" +
                "\n" +
                "            const info = document.createElement('div');\n" +
                "            info.className = 'chat-info';\n" +
                "\n" +
                "            const nameEl = document.createElement('div');\n" +
                "            nameEl.className = 'chat-name';\n" +
                "            nameEl.textContent = chat.deviceName;\n" +
                "\n" +
                "            const msgEl = document.createElement('div');\n" +
                "            msgEl.className = 'chat-last-message';\n" +
                "            msgEl.textContent = chat.lastMessage || 'Henüz mesaj yok';\n" +
                "\n" +
                "            const timeEl = document.createElement('div');\n" +
                "            timeEl.className = 'chat-time';\n" +
                "            timeEl.textContent = chat.timeDisplay;\n" +
                "\n" +
                "            info.appendChild(nameEl);\n" +
                "            info.appendChild(msgEl);\n" +
                "            info.appendChild(timeEl);\n" +
                "\n" +
                "            item.appendChild(avatar);\n" +
                "            item.appendChild(info);\n" +
                "            return item;\n" +
                "        }\n" +
                "\n" +
                "        // Initial load\n" +
                "        Android.getChatList();\n" +
                "    </script>\n" +
                "</html>";
    }
}

