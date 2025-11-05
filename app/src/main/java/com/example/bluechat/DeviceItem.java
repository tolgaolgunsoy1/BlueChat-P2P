package com.example.bluechat;

public class DeviceItem {
    private String icon;
    private String name;
    private String address;
    private boolean active;
    private float distance;
    private String nickname;
    private boolean paired;
    private String preview;
    private String time;
    private int badgeCount;

    public DeviceItem(String icon, String name, String address, boolean active, float distance) {
        this.icon = icon;
        this.name = name;
        this.address = address;
        this.active = active;
        this.distance = distance;
    }

    public DeviceItem(String icon, String name, String address, boolean active, float distance, String nickname, boolean paired, String preview, String time, int badgeCount) {
        this.icon = icon;
        this.name = name;
        this.address = address;
        this.active = active;
        this.distance = distance;
        this.nickname = nickname;
        this.paired = paired;
        this.preview = preview;
        this.time = time;
        this.badgeCount = badgeCount;
    }

    public String getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }

    public float getDistance() {
        return distance;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isPaired() {
        return paired;
    }

    public String getPreview() {
        return preview;
    }

    public String getTime() {
        return time;
    }

    public int getBadgeCount() {
        return badgeCount;
    }
}
