package com.example.bluechat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class BluetoothNotificationService extends Service {
    private static final String TAG = "BluetoothNotificationService";
    private static final String CHANNEL_ID = "bluetooth_chat_channel";
    private static final int NOTIFICATION_ID = 1001;

    private BluetoothService bluetoothService;
    private String connectedDeviceName = null;
    private NotificationManager notificationManager;
    private Handler serviceHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        serviceHandler = new Handler(Looper.getMainLooper(), new ServiceCallback());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        // Start Bluetooth service for background listening
        bluetoothService = new BluetoothService(serviceHandler);
        bluetoothService.start();

        // Start foreground service with notification
        Notification notification = createForegroundNotification();
        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.stop();
        }
        stopForeground(true);
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Bluetooth Chat",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background Bluetooth chat service");
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String statusText = connectedDeviceName != null ?
            "Bağlı: " + connectedDeviceName : "Bluetooth dinleniyor...";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BlueChat Arka Plan")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_bluetooth_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void showMessageNotification(String message, String sender) {
        SharedPreferences prefs = getSharedPreferences("BlueChatSettings", MODE_PRIVATE);
        if (!prefs.getBoolean("notifications_enabled", true)) {
            return;
        }

        Intent chatIntent = new Intent(this, ChatActivity.class);
        chatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 1, chatIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(sender != null ? sender : "Yeni Mesaj")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void updateForegroundNotification() {
        Notification notification = createForegroundNotification();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private class ServiceCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            connectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                            updateForegroundNotification();
                            break;
                        case BluetoothService.STATE_NONE:
                        case BluetoothService.STATE_LISTEN:
                            connectedDeviceName = null;
                            updateForegroundNotification();
                            break;
                    }
                    break;

                case BluetoothService.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    showMessageNotification(readMessage, connectedDeviceName);
                    break;

                case BluetoothService.MESSAGE_DEVICE_NAME:
                    connectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                    updateForegroundNotification();
                    break;
            }
            return true;
        }
    }
}
