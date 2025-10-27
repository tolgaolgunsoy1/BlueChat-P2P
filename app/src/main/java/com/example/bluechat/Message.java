package com.example.bluechat;

import java.util.Date;

public class Message {
    private String text;
    private Date timestamp;
    private boolean isSent; // true if sent by this device, false if received

    public Message() {
        // Default constructor for Room
    }

    public Message(String text, boolean isSent) {
        this.text = text;
        this.isSent = isSent;
        this.timestamp = new Date();
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
}
