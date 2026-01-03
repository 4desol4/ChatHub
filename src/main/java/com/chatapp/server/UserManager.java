package com.chatapp.server;

import com.chatapp.database.MessageDAO;
import com.chatapp.database.UserDAO;
import com.chatapp.model.User;
import com.chatapp.model.Message;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private static UserManager instance;
    private final Map<String, ServerHandler> onlineUsers; // username -> ServerHandler

    // Database DAOs
    private final UserDAO userDAO;
    private final MessageDAO messageDAO;

    private UserManager() {
        onlineUsers = new ConcurrentHashMap<>();
        userDAO = new UserDAO();
        messageDAO = new MessageDAO();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    // User Registration - NOW USES DATABASE
    public synchronized boolean registerUser(User user) {
        boolean success = userDAO.createUser(
                user.getUsername(),
                user.getPassword(),
                user.getEmail()
        );

        if (success) {
            System.out.println("✅ User registered in database: " + user.getUsername());
        }

        return success;
    }

    // User Authentication - NOW USES DATABASE
    public synchronized User authenticateUser(String username, String password) {
        return userDAO.authenticateUser(username, password);
    }

    // Online User Management (still in-memory for performance)
    public synchronized void addOnlineUser(String username, ServerHandler handler) {
        onlineUsers.put(username, handler);
        userDAO.updateUserStatus(username, User.Status.ONLINE);
        System.out.println("User online: " + username + " (Total online: " + onlineUsers.size() + ")");
    }

    public synchronized void removeOnlineUser(String username) {
        onlineUsers.remove(username);
        userDAO.updateUserStatus(username, User.Status.OFFLINE);
        System.out.println("User offline: " + username + " (Total online: " + onlineUsers.size() + ")");
    }

    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    public ServerHandler getOnlineUserHandler(String username) {
        return onlineUsers.get(username);
    }

    public List<User> getOnlineUsers() {
        return userDAO.getOnlineUsers();
    }

    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    // ✅ ADD THIS METHOD
    public Set<String> getAllOnlineUsernames() {
        return new HashSet<>(onlineUsers.keySet());
    }

    // Offline Message Queue - NOW USES DATABASE
    public synchronized void addOfflineMessage(String username, Message message) {
        messageDAO.saveOfflineMessage(message);
        System.out.println("Offline message saved to database for: " + username);
    }

    public synchronized List<Message> getOfflineMessages(String username) {
        return messageDAO.getOfflineMessages(username);
    }

    // Broadcast to all online users
    public void broadcastMessage(Message message, String excludeUsername) {
        // Save to history
        messageDAO.saveToChatHistory(message);

        // Broadcast to online users
        for (Map.Entry<String, ServerHandler> entry : onlineUsers.entrySet()) {
            if (!entry.getKey().equals(excludeUsername)) {
                try {
                    entry.getValue().sendMessage(message);
                } catch (IOException e) {
                    System.err.println("Failed to send message to: " + entry.getKey());
                }
            }
        }
    }

    public void shutdown() {
        // Update all online users to offline
        for (String username : onlineUsers.keySet()) {
            userDAO.updateUserStatus(username, User.Status.OFFLINE);
        }
        onlineUsers.clear();
        System.out.println("UserManager shutdown complete");
    }
}