package com.example.bluechat;

public class DeviceItem {
    public final String name;
    public final String address;
    public final short rssi;
    public final boolean paired;
    public final String icon;
    public String nickname;

    public DeviceItem(String name, String address, short rssi, boolean paired, String icon, String nickname) {
        this.name = name;
        this.address = address;
        this.rssi = rssi;
        this.paired = paired;
        this.icon = icon;
        this.nickname = nickname;
    }

    public DeviceItem(String name, String address, short rssi, boolean paired, String icon) {
        this(name, address, rssi, paired, icon, null);
    }
}
