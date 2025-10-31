package com.example.bluechat;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String content;
    private long timestamp;
    private boolean isSent;
    private String deviceAddress;
    private String messageType; // "text", "image", "file"
    private String filePath;
    private String reactions; // JSON string of emoji reactions
    private boolean isTyping; // For typing indicators

    public MessageEntity(String content, boolean isSent, String deviceAddress) {
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.isSent = isSent;
        this.deviceAddress = deviceAddress;
        this.messageType = "text";
        this.reactions = "";
        this.isTyping = false;
    }

    // Constructor for file messages
    @androidx.room.Ignore
    public MessageEntity(String content, boolean isSent, String deviceAddress, String messageType, String filePath) {
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.isSent = isSent;
        this.deviceAddress = deviceAddress;
        this.messageType = messageType;
        this.filePath = filePath;
        this.reactions = "";
        this.isTyping = false;
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSent() { return isSent; }
    public void setSent(boolean sent) { isSent = sent; }

    public String getDeviceAddress() { return deviceAddress; }
    public void setDeviceAddress(String deviceAddress) { this.deviceAddress = deviceAddress; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getReactions() { return reactions; }
    public void setReactions(String reactions) { this.reactions = reactions; }

    public boolean isTyping() { return isTyping; }
    public void setTyping(boolean typing) { isTyping = typing; }

    // Legacy getters for backward compatibility
    public String getText() { return content; }
    public void setText(String text) { this.content = text; }
}
