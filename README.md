# BlueChat - Bluetooth Chat Messenger

A modern Android Bluetooth chat application built with Java, featuring persistent message history using Room database.

## 📱 Features

- **Real-time Bluetooth Messaging**: Connect and chat with nearby Bluetooth devices
- **Persistent Message History**: All messages are saved locally using Room database
- **Modern UI**: Clean Material Design interface with RecyclerView
- **Device Discovery**: Scan and pair with available Bluetooth devices
- **Android 12+ Compatible**: Full support for latest Android permissions
- **Java-based Architecture**: Pure Java implementation for better compatibility

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android device with Bluetooth support (API 21+)
- JDK 11 or higher

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/BlueChat.git
cd BlueChat
```

2. Open in Android Studio and sync Gradle files

3. Build and run on two Android devices

### Testing

1. Install the app on two Bluetooth-enabled Android devices
2. Enable Bluetooth and location permissions on both devices
3. On one device, scan for devices and connect
4. On the other device, accept the connection
5. Start chatting - messages will be saved automatically

## 🏗️ Architecture

### Core Components

- **MainActivity**: Entry point with permission handling
- **DeviceListActivity**: Bluetooth device discovery and selection
- **ChatActivity**: Main chat interface with message history
- **BluetoothService**: Bluetooth connection management
- **MessageAdapter**: RecyclerView adapter for chat messages
- **Room Database**: Local message persistence
  - `MessageEntity`: Database schema
  - `MessageDao`: Data access operations
  - `AppDatabase`: Room database instance

### Database Schema

```sql
CREATE TABLE messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    text TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    isSent INTEGER NOT NULL,
    deviceAddress TEXT NOT NULL
);
```

## 🔧 Build & Deployment

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Generate Signed APK
1. Create keystore: `keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias`
2. Configure signing in `app/build.gradle.kts`
3. Build signed APK: `Build > Build Bundle(s)/APK(s) > Build APK(s)`

## 📋 Permissions

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-feature android:name="android.hardware.bluetooth" android:required="true" />
```

## 🧪 Testing Checklist

- [ ] Bluetooth connection establishment
- [ ] Message sending and receiving
- [ ] Message history persistence
- [ ] Permission handling (Android 12+)
- [ ] Connection recovery after disconnection
- [ ] UI responsiveness during data operations

## 🔮 Future Enhancements

- **Notifications**: Background message notifications
- **Dark Mode**: Theme switching support
- **File Sharing**: Media file transfer over Bluetooth
- **Multi-language**: Turkish/English localization
- **Cloud Sync**: Firebase integration for cross-device sync
- **Foreground Service**: Persistent background connections

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 Support

For questions or issues:
- Create an issue on GitHub
- Check the troubleshooting section in our wiki

---

**Note**: This app requires two Android devices with Bluetooth support for full functionality testing.
