package com.chatapp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        ONLINE, AWAY, BUSY, OFFLINE
    }

    private String username;
    private String password;
    private String email;
    private Status status;
    private LocalDateTime lastSeen;
    private String avatarColor;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.status = Status.OFFLINE;
        this.lastSeen = LocalDateTime.now();
        this.avatarColor = generateAvatarColor();
    }

    public User(String username, String password, String email) {
        this(username, password);
        this.email = email;
    }

    private String generateAvatarColor() {
        String[] colors = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A",
                "#98D8C8", "#F7B731", "#5F27CD", "#00D2D3"};
        int index = Math.abs(username.hashCode()) % colors.length;
        return colors[index];
    }


    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public Status getStatus() { return status; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public String getAvatarColor() { return avatarColor; }

    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }
    public void setStatus(Status status) {
        this.status = status;
        this.lastSeen = LocalDateTime.now();
    }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public String getInitials() {
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public String toString() {
        return username + " (" + status + ")";
    }
}