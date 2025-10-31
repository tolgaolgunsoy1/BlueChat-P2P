package com.example.bluechat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class FileTransferService extends Service {
    private static final String TAG = "FileTransferService";
    private static final String CHANNEL_ID = "file_transfer_channel";
    private static final int NOTIFICATION_ID = 2001;

    private NotificationManager notificationManager;
    private Handler serviceHandler;
    private BluetoothService bluetoothService;
    private boolean isTransferring = false;
    private long totalBytes = 0;
    private long transferredBytes = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        serviceHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if ("SEND_FILE".equals(action)) {
                Uri fileUri = intent.getParcelableExtra("file_uri");
                String deviceAddress = intent.getStringExtra("device_address");
                if (fileUri != null && deviceAddress != null) {
                    sendFile(fileUri, deviceAddress);
                }
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.stop();
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Dosya Aktarımı",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Dosya aktarım ilerleme durumu");
            channel.enableLights(false);
            channel.enableVibration(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendFile(Uri fileUri, String deviceAddress) {
        if (isTransferring) {
            Log.w(TAG, "Transfer already in progress");
            return;
        }

        isTransferring = true;

        // Start foreground service
        Notification notification = createProgressNotification("Dosya gönderiliyor...", 0);
        startForeground(NOTIFICATION_ID, notification);

        serviceHandler.post(() -> {
            try {
                // Get file info
                File file = new File(fileUri.getPath());
                if (!file.exists()) {
                    showErrorNotification("Dosya bulunamadı");
                    return;
                }

                totalBytes = file.length();
                transferredBytes = 0;

                // Connect to device
                bluetoothService = new BluetoothService(null); // We'll handle messages differently
                // TODO: Implement device connection and file transfer logic

                // For now, simulate transfer
                simulateFileTransfer(file.getName());

            } catch (Exception e) {
                Log.e(TAG, "File transfer failed", e);
                showErrorNotification("Aktarım başarısız: " + e.getMessage());
            } finally {
                isTransferring = false;
                stopForeground(true);
            }
        });
    }

    private void simulateFileTransfer(String fileName) {
        // Simulate file transfer progress
        for (int progress = 0; progress <= 100; progress += 10) {
            try {
                Thread.sleep(500); // Simulate transfer time
                updateProgressNotification(fileName, progress);
                transferredBytes = (totalBytes * progress) / 100;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        showSuccessNotification(fileName + " başarıyla gönderildi");
    }

    private Notification createProgressNotification(String title, int progress) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(progress + "% tamamlandı")
            .setSmallIcon(R.drawable.ic_chat)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void updateProgressNotification(String fileName, int progress) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(fileName + " gönderiliyor")
            .setContentText(progress + "% tamamlandı")
            .setSmallIcon(R.drawable.ic_chat)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void showSuccessNotification(String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aktarım Tamamlandı")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_chat)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build();

        notificationManager.notify(NOTIFICATION_ID + 1, notification);
    }

    private void showErrorNotification(String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aktarım Hatası")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_chat)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build();

        notificationManager.notify(NOTIFICATION_ID + 2, notification);
    }

    // Utility method to get file size as human readable string
    private String getFileSizeString(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
