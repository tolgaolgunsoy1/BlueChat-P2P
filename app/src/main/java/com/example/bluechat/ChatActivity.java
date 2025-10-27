package com.example.bluechat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private Button sendButton;

    private AppDatabase database;

    private static final int REQUEST_ENABLE_BT = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        database = DatabaseHelper.getInstance(this);

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
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

        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String message = messageEditText.getText().toString();
                sendMessage(message);
            }
        });

        bluetoothService = new BluetoothService(mHandler);
        outStringBuffer = new StringBuffer("");

        Intent intent = getIntent();
        String address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        connectedDeviceAddress = address;
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        bluetoothService.connect(device);
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

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setTitle(getString(R.string.title_connected_to, connectedDeviceName));
                            loadMessageHistory();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setTitle(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setTitle(R.string.title_not_connected);
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
                    break;
                case BluetoothService.MESSAGE_DEVICE_NAME:
                    connectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + connectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothService.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothService.TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void loadMessageHistory() {
        if (connectedDeviceAddress != null) {
            new LoadMessagesTask(messages, messageAdapter, messageRecyclerView).execute(connectedDeviceAddress);
        }
    }

    private void saveMessageToDatabase(com.example.bluechat.Message message) {
        if (connectedDeviceAddress != null) {
            MessageEntity entity = new MessageEntity(message.getText(), message.isSent(), connectedDeviceAddress);
            new SaveMessageTask(database).execute(entity);
        }
    }

    private static class LoadMessagesTask extends AsyncTask<String, Void, List<MessageEntity>> {
        private final List<com.example.bluechat.Message> messages;
        private final MessageAdapter messageAdapter;
        private final RecyclerView messageRecyclerView;

        LoadMessagesTask(List<com.example.bluechat.Message> messages, MessageAdapter messageAdapter, RecyclerView messageRecyclerView) {
            this.messages = messages;
            this.messageAdapter = messageAdapter;
            this.messageRecyclerView = messageRecyclerView;
        }

        @Override
        protected List<MessageEntity> doInBackground(String... params) {
            return DatabaseHelper.getInstance(null).messageDao().getMessagesForDevice(params[0]);
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
}
