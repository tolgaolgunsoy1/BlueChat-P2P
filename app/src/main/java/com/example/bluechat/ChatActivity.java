package com.example.bluechat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends Activity {
    private static final String TAG = "ChatActivity";

    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothService bluetoothService = null;
    private StringBuffer outStringBuffer;

    private String connectedDeviceName = null;
    private String connectedDeviceAddress = null;

    private RecyclerView messageRecyclerView;
    private MessageAdapter messageAdapter;
    private List<com.example.bluechat.Message> messages;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton attachButton;

    private AppDatabase database;
    private android.app.ProgressDialog connectDialog;

    private android.widget.TextView chatTitleView;
    private android.widget.TextView connectionBadgeView;

    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_BT_PERMISSIONS = 102;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        database = DatabaseHelper.getInstance(this);

        // Header views (may be null on older layouts)
        chatTitleView = findViewById(R.id.chat_title);
        connectionBadgeView = findViewById(R.id.connection_badge);
        if (connectionBadgeView != null) {
            connectionBadgeView.setText("🔴 Kopuk");
        }

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    private void playIncomingTone() {
        try {
            android.media.ToneGenerator tg = new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 80);
            tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150);
            // Let the system stop the tone automatically; no long-lived resources kept
        } catch (Exception ignored) {}
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setTitle(getString(R.string.title_connected_to, connectedDeviceName != null ? connectedDeviceName : connectedDeviceAddress));
                            sendButton.setEnabled(true);
                            loadMessageHistory();
                            Toast.makeText(ChatActivity.this, "Bağlantı başarılı ✓", Toast.LENGTH_SHORT).show();
                            if (connectDialog != null && connectDialog.isShowing()) {
                                try { connectDialog.dismiss(); } catch (Exception ignored) {}
                            }
                            if (connectionBadgeView != null) {
                                connectionBadgeView.setText("🟢 Bağlı");
                            }
                            if (chatTitleView != null) {
                                String titleName = connectedDeviceName != null ? connectedDeviceName : connectedDeviceAddress;
                                if (titleName != null) chatTitleView.setText(titleName);
                            }
                            // Save to recent connections
                            saveRecentConnection(connectedDeviceName, connectedDeviceAddress);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setTitle(R.string.title_connecting);
                            sendButton.setEnabled(false);
                            Toast.makeText(ChatActivity.this, "Bağlanıyor...", Toast.LENGTH_SHORT).show();
                            if (connectionBadgeView != null) {
                                connectionBadgeView.setText("🔵 Bağlanıyor");
                            }
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setTitle(R.string.title_not_connected);
                            sendButton.setEnabled(false);
                            if (connectDialog != null && connectDialog.isShowing()) {
                                try { connectDialog.dismiss(); } catch (Exception ignored) {}
                            }
                            if (connectionBadgeView != null) {
                                connectionBadgeView.setText("🔴 Kopuk");
                            }
                            break;
                    }
                    break;
                case BluetoothService.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    com.example.bluechat.Message sentMsg = new com.example.bluechat.Message(writeMessage, true);
                    messageAdapter.addMessage(sentMsg);
                    messageRecyclerView.scrollToPosition(messages.size() - 1);
                    saveMessageToDatabase(sentMsg);
                    break;
                case BluetoothService.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    com.example.bluechat.Message receivedMsg = new com.example.bluechat.Message(readMessage, false);
                    messageAdapter.addMessage(receivedMsg);
                    messageRecyclerView.scrollToPosition(messages.size() - 1);
                    saveMessageToDatabase(receivedMsg);
                    playIncomingTone();
                    break;
                case BluetoothService.MESSAGE_DEVICE_NAME:
                    connectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                    String addr = msg.getData().getString("device_address");
                    if (connectedDeviceAddress == null && addr != null) {
                        connectedDeviceAddress = addr;
                    }
                    setTitle(getString(R.string.title_connected_to, connectedDeviceName));
                    Toast.makeText(getApplicationContext(), "Connected to " + connectedDeviceName, Toast.LENGTH_SHORT).show();
                    if (chatTitleView != null && connectedDeviceName != null) {
                        chatTitleView.setText(connectedDeviceName);
                    }
                    // Save to recent connections when we know both name and address
                    saveRecentConnection(connectedDeviceName, connectedDeviceAddress);
                    break;
                case BluetoothService.MESSAGE_TOAST:
                    String t = msg.getData().getString(BluetoothService.TOAST);
                    Toast.makeText(getApplicationContext(), t, Toast.LENGTH_SHORT).show();
                    if ("Device connection was lost".equals(t)) {
                        showReconnectDialog();
                    }
                    if (connectDialog != null && connectDialog.isShowing()) {
                        try { connectDialog.dismiss(); } catch (Exception ignored) {}
                    }
                    break;
            }
        }
    };

    private void showReconnectDialog() {
        // Check auto-reconnect preference
        android.content.SharedPreferences prefs = getSharedPreferences("bluechat_prefs", MODE_PRIVATE);
        boolean autoReconnect = prefs.getBoolean("auto_reconnect", true);

        if (autoReconnect) {
            // Auto-reconnect without dialog
            if (connectedDeviceAddress != null) {
                try {
                    BluetoothDevice dev = bluetoothAdapter.getRemoteDevice(connectedDeviceAddress);
                    bluetoothService.connect(dev);
                    Toast.makeText(this, "Otomatik yeniden bağlanıyor...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Auto-reconnect failed", e);
                    showManualReconnectDialog();
                }
            }
        } else {
            showManualReconnectDialog();
        }
    }

    private void showManualReconnectDialog() {
        try {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Bağlantı koptu")
                .setMessage("Yeniden bağlanmak ister misiniz?")
                .setNegativeButton("Hayır", (d, w) -> { d.dismiss(); })
                .setPositiveButton("Evet", (d, w) -> {
                    if (connectedDeviceAddress != null) {
                        try {
                            BluetoothDevice dev = bluetoothAdapter.getRemoteDevice(connectedDeviceAddress);
                            bluetoothService.connect(dev);
                        } catch (Exception ignored) {}
                    }
                })
                .show();
        } catch (Exception ignored) {}
    }
    private boolean ensureBtPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            String[] permissions = new String[] {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            };
            boolean needs = false;
            for (String p : permissions) {
                if (ContextCompat.checkSelfPermission(this, p) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    needs = true;
                    break;
                }
            }
            if (needs) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_BT_PERMISSIONS);
                return false;
            }
        }
        return true;
    }

    private BluetoothDevice pendingBondDevice;
    private BroadcastReceiver bondReceiver;

    private void registerBondReceiver() {
        if (bondReceiver != null) return;
        bondReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                final String action = intent.getAction();
                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                    Log.d(TAG, "Bond state changed: " + prevState + " -> " + state);
                    if (state == BluetoothDevice.BOND_BONDED && device != null && device.getAddress().equals(connectedDeviceAddress)) {
                        // Now connect
                        try {
                            bluetoothAdapter.cancelDiscovery();
                        } catch (SecurityException ignored) {}
                        bluetoothService.connect(device);
                        unregisterBondReceiverIfNeeded();
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bondReceiver, filter);
    }

    private void unregisterBondReceiverIfNeeded() {
        if (bondReceiver != null) {
            try { unregisterReceiver(bondReceiver); } catch (Exception ignored) {}
            bondReceiver = null;
        }
    }
    

    @Override
    public void onStart() {
        super.onStart();

        if (!bluetoothAdapter.isEnabled()) {
            // Note: ACTION_REQUEST_ENABLE may require BLUETOOTH_CONNECT permission on Android 12+
            // but we're handling this in the Activity level
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            try {
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to request Bluetooth enable due to missing permission");
                Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (bluetoothService == null) {
            setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        if (bluetoothService != null) {
            if (bluetoothService.getState() == BluetoothService.STATE_NONE) {
                bluetoothService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        messageRecyclerView = findViewById(R.id.message_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);
        attachButton = findViewById(R.id.attach_button);

        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages);
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageRecyclerView.setAdapter(messageAdapter);

        loadMessageHistory();

        messageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                    String message = view.getText().toString();
                    sendMessage(message);
                }
                return true;
            }
        });

        // Disable send until connected
        sendButton.setEnabled(false);

        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String message = messageEditText.getText().toString();
                sendMessage(message);
            }
        });

        if (attachButton != null) {
            attachButton.setOnClickListener(v -> showFilePicker());
        }

        bluetoothService = new BluetoothService(mHandler);
        outStringBuffer = new StringBuffer("");

        // Start listening for incoming connections to increase success rate
        bluetoothService.start();

        Intent intent = getIntent();
        String address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        if (address == null || address.length() < 17) {
            // Listen-only mode: wait for incoming connection
            setTitle(R.string.title_not_connected);
            return;
        }
        connectedDeviceAddress = address;
        BluetoothDevice device;
        try {
            device = bluetoothAdapter.getRemoteDevice(address);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Geçersiz cihaz adresi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure Bluetooth CONNECT permission on Android 12+
        if (!ensureBtPermissions()) {
            // Permissions requested; will continue in onRequestPermissionsResult
            return;
        }

        // If not bonded, initiate pairing and connect after bonded
        try {
            int bondState = device.getBondState();
            if (bondState != BluetoothDevice.BOND_BONDED) {
                registerBondReceiver();
                boolean started = device.createBond();
                if (!started) {
                    Toast.makeText(this, "Eşleştirme başlatılamadı", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(this, "Eşleştirme başlatıldı...", Toast.LENGTH_SHORT).show();
                return; // wait for bond receiver
            }
        } catch (SecurityException se) {
            Toast.makeText(this, "Bluetooth izinleri gerekli", Toast.LENGTH_SHORT).show();
            return;
        }
        // Cancel discovery before attempting connection for faster connect
        try {
            bluetoothAdapter.cancelDiscovery();
        } catch (SecurityException e) {
            Log.w(TAG, "cancelDiscovery failed due to missing permission");
        }
        try {
            // Show connecting dialog with cancel option
            connectDialog = new android.app.ProgressDialog(this);
            connectDialog.setMessage("Bağlanıyor...");
            connectDialog.setCancelable(true);
            connectDialog.setButton(android.content.DialogInterface.BUTTON_NEGATIVE, "İptal", (d, w) -> {
                if (bluetoothService != null) bluetoothService.cancelConnect();
                try { d.dismiss(); } catch (Exception ignored) {}
            });
            try { connectDialog.show(); } catch (Exception ignored) {}
            bluetoothService.connect(device);
        } catch (Exception e) {
            Log.e(TAG, "connect failed", e);
            Toast.makeText(this, "Bağlantı başlatılamadı", Toast.LENGTH_SHORT).show();
            if (connectDialog != null && connectDialog.isShowing()) {
                try { connectDialog.dismiss(); } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) bluetoothService.stop();
        unregisterBondReceiverIfNeeded();
    }

    private void ensureDiscoverable() {
        // Note: getScanMode may require BLUETOOTH_SCAN permission on Android 12+
        // but we're handling this in the Activity level
        try {
            if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Failed to check scan mode due to missing permission");
        }
    }

    private void sendMessage(String message) {
        if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            bluetoothService.write(send);

            outStringBuffer.setLength(0);
            messageEditText.setText(outStringBuffer);

            com.example.bluechat.Message msg = new com.example.bluechat.Message(message, true);
            messageAdapter.addMessage(msg);
            messageRecyclerView.scrollToPosition(messages.size() - 1);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_FILE_PICK:
                if (resultCode == RESULT_OK && data != null) {
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        sendFile(fileUri);
                    }
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_PERMISSIONS) {
            // Retry setup after permissions granted/denied; setupChat handles toasts
            setupChat();
        }
    }

    private void loadMessageHistory() {
        if (connectedDeviceAddress != null) {
            new LoadMessagesTask(database, messages, messageAdapter, messageRecyclerView).execute(connectedDeviceAddress);
        }
    }

    private void saveMessageToDatabase(com.example.bluechat.Message message) {
        if (connectedDeviceAddress != null) {
            MessageEntity entity = new MessageEntity(message.getText(), message.isSent(), connectedDeviceAddress);
            new SaveMessageTask(database).execute(entity);
        }
    }

    private void saveRecentConnection(String name, String address) {
        if (address == null || address.trim().isEmpty()) return;
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("bluechat_prefs", Context.MODE_PRIVATE);
            String raw = prefs.getString("recent_connections", "");
            java.util.LinkedList<String> list = new java.util.LinkedList<>();
            if (raw != null && !raw.isEmpty()) {
                for (String part : raw.split(";")) {
                    if (part != null && !part.trim().isEmpty()) list.add(part);
                }
            }
            String safeName = name == null ? "Bilinmeyen Cihaz" : name.replace(";", " ").replace(",", " ");
            long ts = System.currentTimeMillis();
            String entry = safeName + "," + address + "," + ts;
            // Remove existing with same address
            java.util.Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                String e = it.next();
                if (e.contains(",")) {
                    String[] parts = e.split(",");
                    if (parts.length >= 2 && address.equals(parts[1])) {
                        it.remove();
                    }
                }
            }
            list.addFirst(entry);
            // Cap at 5
            while (list.size() > 5) list.removeLast();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(';');
                sb.append(list.get(i));
            }
            prefs.edit().putString("recent_connections", sb.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static class LoadMessagesTask extends AsyncTask<String, Void, List<MessageEntity>> {
        private final AppDatabase database;
        private final List<com.example.bluechat.Message> messages;
        private final MessageAdapter messageAdapter;
        private final RecyclerView messageRecyclerView;

        LoadMessagesTask(AppDatabase database, List<com.example.bluechat.Message> messages, MessageAdapter messageAdapter, RecyclerView messageRecyclerView) {
            this.database = database;
            this.messages = messages;
            this.messageAdapter = messageAdapter;
            this.messageRecyclerView = messageRecyclerView;
        }

        @Override
        protected List<MessageEntity> doInBackground(String... params) {
            return database.messageDao().getMessagesForDevice(params[0]);
        }

        @Override
        protected void onPostExecute(List<MessageEntity> messageEntities) {
            messages.clear();
            for (MessageEntity entity : messageEntities) {
                messages.add(new com.example.bluechat.Message(entity.getText(), entity.isSent()));
            }
            messageAdapter.notifyDataSetChanged();
            if (!messages.isEmpty()) {
                messageRecyclerView.scrollToPosition(messages.size() - 1);
            }
        }
    }

    private static class SaveMessageTask extends AsyncTask<MessageEntity, Void, Void> {
        private final AppDatabase database;

        SaveMessageTask(AppDatabase database) {
            this.database = database;
        }

        @Override
        protected Void doInBackground(MessageEntity... params) {
            database.messageDao().insert(params[0]);
            return null;
        }
    }

    private void showFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Dosya Seç"), REQUEST_FILE_PICK);
        } catch (Exception e) {
            Toast.makeText(this, "Dosya seçici açılamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private static final int REQUEST_FILE_PICK = 1001;

    private void sendFile(Uri fileUri) {
        if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "Dosya göndermek için bağlantı gerekli", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent serviceIntent = new Intent(this, FileTransferService.class);
        serviceIntent.setAction("SEND_FILE");
        serviceIntent.putExtra("file_uri", fileUri);
        serviceIntent.putExtra("device_address", connectedDeviceAddress);
        startForegroundService(serviceIntent);

        Toast.makeText(this, "Dosya aktarımı başlatıldı", Toast.LENGTH_SHORT).show();
    }
}
