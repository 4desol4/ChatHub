package com.chatapp.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        TEXT, FILE, SYSTEM, USER_JOIN, USER_LEAVE, TYPING, PRIVATE
    }

    private String messageId;
    private String sender;
    private String receiver; // null for broadcast, username for private
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private String fileName;
    private byte[] fileData;

    // Constructor for text messages
    public Message(String sender, String content, MessageType type) {
        this.messageId = generateMessageId();
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    // Constructor for private messages
    public Message(String sender, String receiver, String content) {
        this(sender, content, MessageType.PRIVATE);
        this.receiver = receiver;
    }

    // Constructor for file transfers
    public Message(String sender, String fileName, byte[] fileData) {
        this.messageId = generateMessageId();
        this.sender = sender;
        this.fileName = fileName;
        this.fileData = fileData;
        this.type = MessageType.FILE;
        this.timestamp = LocalDateTime.now();
    }

    private String generateMessageId() {
        return System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getContent() { return content; }
    public MessageType getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getFileName() { return fileName; }
    public byte[] getFileData() { return fileData; }

    public void setSender(String sender) { this.sender = sender; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public void setContent(String content) { this.content = content; }
    public void setType(MessageType type) { this.type = type; }

    public String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return timestamp.format(formatter);
    }

    public String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return timestamp.format(formatter);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", getFormattedTimestamp(), sender, content);
    }
}