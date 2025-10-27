package com.example.bluechat;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String text;
    private long timestamp;
    private boolean isSent;
    private String deviceAddress;

    public MessageEntity(String text, boolean isSent, String deviceAddress) {
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.isSent = isSent;
        this.deviceAddress = deviceAddress;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSent() { return isSent; }
    public void setSent(boolean sent) { isSent = sent; }

    public String getDeviceAddress() { return deviceAddress; }
    public void setDeviceAddress(String deviceAddress) { this.deviceAddress = deviceAddress; }
}
