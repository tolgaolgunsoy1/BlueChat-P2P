package com.example.bluechat;

public class ChatSummary {
    private String deviceAddress;
    private long lastTimestamp;
    private int messageCount;

    public ChatSummary(String deviceAddress, long lastTimestamp, int messageCount) {
        this.deviceAddress = deviceAddress;
        this.lastTimestamp = lastTimestamp;
        this.messageCount = messageCount;
    }

    public String getDeviceAddress() { return deviceAddress; }
    public void setDeviceAddress(String deviceAddress) { this.deviceAddress = deviceAddress; }

    public long getLastTimestamp() { return lastTimestamp; }
    public void setLastTimestamp(long lastTimestamp) { this.lastTimestamp = lastTimestamp; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
}
