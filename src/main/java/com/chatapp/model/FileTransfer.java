package com.chatapp.model;

import java.io.Serializable;

public class FileTransfer implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileId;
    private String fileName;
    private long fileSize;
    private String sender;
    private String receiver;
    private byte[] fileData;
    private TransferStatus status;

    public enum TransferStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    }

    public FileTransfer(String fileName, long fileSize, String sender, String receiver) {
        this.fileId = generateFileId();
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.sender = sender;
        this.receiver = receiver;
        this.status = TransferStatus.PENDING;
    }

    private String generateFileId() {
        return "FILE_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // Getters and Setters
    public String getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public byte[] getFileData() { return fileData; }
    public TransferStatus getStatus() { return status; }

    public void setFileData(byte[] fileData) { this.fileData = fileData; }
    public void setStatus(TransferStatus status) { this.status = status; }

    public String getFileSizeFormatted() {
        if (fileSize < 1024) return fileSize + " B";
        else if (fileSize < 1024 * 1024) return String.format("%.2f KB", fileSize / 1024.0);
        else if (fileSize < 1024 * 1024 * 1024) return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        else return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
    }
}