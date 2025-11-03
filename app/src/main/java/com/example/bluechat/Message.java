package com.example.bluechat;

import java.util.Date;

public class Message {
    private String text;
    private Date timestamp;
    private boolean isSent; // true if sent by this device, false if received
    private String messageType; // "text", "image", "file"
    private String filePath;
    private String reactions; // JSON string of emoji reactions

    public Message() {
        // Default constructor for Room
    }

    public Message(String text, boolean isSent) {
        this.text = text;
        this.isSent = isSent;
        this.timestamp = new Date();
        this.messageType = "text";
        this.reactions = "";
    }

    public Message(String text, boolean isSent, String messageType, String filePath) {
        this.text = text;
        this.isSent = isSent;
        this.timestamp = new Date();
        this.messageType = messageType;
        this.filePath = filePath;
        this.reactions = "";
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getReactions() {
        return reactions;
    }

    public void setReactions(String reactions) {
        this.reactions = reactions;
    }
}
