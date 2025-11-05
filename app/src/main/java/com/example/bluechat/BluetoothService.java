package com.example.bluechat;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.Arrays;
import java.util.Queue;
import java.util.LinkedList;

public class BluetoothService {
    private static final String TAG = "BluetoothService";
    private static final String APP_NAME = "BluetoothChat";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;
    private Queue<byte[]> messageQueue;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    public BluetoothService(Handler handler) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.handler = handler;
        state = STATE_NONE;
        messageQueue = new LinkedList<>();
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        setState(STATE_LISTEN);
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // Flush queued messages on reconnect
        flushMessageQueue();

        // Note: device.getName() may require BLUETOOTH_CONNECT permission on Android 12+
        // but we're handling this in the Activity level
        String deviceName = "Unknown Device"; // Default fallback
        try {
            String name = device.getName();
            if (name != null && !name.trim().isEmpty()) {
                deviceName = name;
            }
        } catch (SecurityException e) {
            // Permission not granted, use default name
            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for device name");
        }
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString(DEVICE_NAME, deviceName);
        bundle.putString("device_address", device.getAddress());
        android.os.Message msg = handler.obtainMessage(MESSAGE_DEVICE_NAME);
        msg.setData(bundle);
        msg.sendToTarget();
        setState(STATE_CONNECTED);
    }

    public synchronized void cancelConnect() {
        if (connectThread != null) {
            try { connectThread.cancel(); } catch (Exception ignored) {}
            connectThread = null;
        }
        // Return to listening state
        if (connectedThread == null) {
            if (acceptThread == null) {
                acceptThread = new AcceptThread();
                acceptThread.start();
            }
            setState(STATE_LISTEN);
        }
    }

    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        setState(STATE_NONE);
    }

    public void write(byte[] out) {
        synchronized (this) {
            if (state == STATE_CONNECTED) {
                ConnectedThread r = connectedThread;
                if (r != null) {
                    r.write(out);
                }
            } else {
                // Queue message if not connected
                messageQueue.offer(out);
                Log.d(TAG, "Message queued, queue size: " + messageQueue.size());
            }
        }
    }

    private void connectionFailed() {
        setState(STATE_LISTEN);
        android.os.Bundle b = new android.os.Bundle();
        b.putString(TOAST, "Unable to connect device");
        android.os.Message m = handler.obtainMessage(MESSAGE_TOAST);
        m.setData(b);
        m.sendToTarget();
    }

    private void connectionLost() {
        setState(STATE_LISTEN);
        android.os.Bundle b = new android.os.Bundle();
        b.putString(TOAST, "Device connection was lost");
        android.os.Message m = handler.obtainMessage(MESSAGE_TOAST);
        m.setData(b);
        m.sendToTarget();
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + this.state + " -> " + state);
        this.state = state;
        handler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public int getState() {
        return state;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    private void flushMessageQueue() {
        synchronized (this) {
            while (!messageQueue.isEmpty() && state == STATE_CONNECTED && connectedThread != null) {
                byte[] message = messageQueue.poll();
                if (message != null) {
                    connectedThread.write(message);
                    Log.d(TAG, "Flushed queued message");
                }
            }
        }
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            tryCreateServerSocket();
        }

        private void tryCreateServerSocket() {
            BluetoothServerSocket tmp = null;
            try {
                // Secure server socket first
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
                Log.d(TAG, "Secure server socket created");
            } catch (SecurityException e) {
                Log.e(TAG, "listen() failed due to missing permission", e);
            } catch (IOException e) {
                Log.w(TAG, "Secure listen() failed, will try insecure", e);
            }
            if (tmp == null) {
                try {
                    // Fallback to insecure server socket for wider compatibility
                    tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, MY_UUID);
                    Log.d(TAG, "Insecure server socket created");
                } catch (IOException ex) {
                    Log.e(TAG, "Insecure listen() failed", ex);
                }
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "BEGIN mAcceptThread" + this);
            BluetoothSocket socket = null;

            while (state != STATE_CONNECTED) {
                try {
                    socket = mmServerSocket != null ? mmServerSocket.accept() : null;
                } catch (IOException e) {
                    Log.w(TAG, "accept() failed on current server socket, attempting insecure fallback", e);
                    // Try to recreate server socket once more using insecure mode
                    if (mmServerSocket != null) {
                        try {
                            mmServerSocket.close();
                        } catch (IOException closeEx) {
                            Log.w(TAG, "Failed to close server socket after accept failure", closeEx);
                        }
                    }
                    tryCreateServerSocket();
                    continue;
                }

                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                // Try secure RFCOMM first
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.d(TAG, "Secure RFCOMM socket created");
            } catch (IOException e) {
                Log.w(TAG, "Secure create() failed, will try insecure later", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Note: cancelDiscovery may require BLUETOOTH_SCAN permission on Android 12+
            // but we're handling this in the Activity level
            try {
                bluetoothAdapter.cancelDiscovery();
            } catch (SecurityException e) {
                Log.w(TAG, "cancelDiscovery failed due to missing permission");
            }

            try {
                if (mmSocket != null) {
                    mmSocket.connect();
                } else {
                    throw new IOException("Socket not created");
                }
            } catch (IOException connectException) {
                Log.w(TAG, "Secure connect failed, trying insecure", connectException);
                // Fallback to insecure RFCOMM
                try {
                    BluetoothSocket insecure = mmDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                    mmSocket = insecure;
                    mmSocket.connect();
                    Log.d(TAG, "Insecure connect succeeded");
                } catch (IOException insecureEx) {
                    Log.e(TAG, "Insecure connect failed", insecureEx);
                    connectionFailed();
                    try {
                        if (mmSocket != null) mmSocket.close();
                    } catch (IOException closeException) {
                        Log.e(TAG, "unable to close() socket during connection failure", closeException);
                    }
                    return;
                }
            }

            synchronized (BluetoothService.this) {
                connectThread = null;
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    byte[] readCopy = Arrays.copyOf(buffer, bytes);
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, readCopy).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                byte[] writeCopy = Arrays.copyOf(buffer, buffer.length);
                handler.obtainMessage(MESSAGE_WRITE, -1, -1, writeCopy).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
