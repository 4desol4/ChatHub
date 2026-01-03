package com.chatapp.server;

import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.util.NetworkUtil;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ServerHandler implements Runnable {
    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private final UserManager userManager;
    private volatile boolean running;

    public ServerHandler(Socket socket) {
        this.socket = socket;
        this.userManager = UserManager.getInstance();
        this.running = true;
    }

    @Override
    public void run() {
        try {
            // Initialize streams - INPUT FIRST to prevent deadlock
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            System.out.println("New client connected from: " + socket.getInetAddress());

            // Handle client authentication
            if (!handleAuthentication()) {
                return;
            }

            // Send offline messages if any
            sendOfflineMessages();

            // Send current online users list
            sendOnlineUsersList();

            // Notify others about new user
            broadcastUserJoined();

            // Main message loop
            while (running) {
                try {
                    Message message = NetworkUtil.receiveMessage(in);
                    handleMessage(message);
                } catch (EOFException e) {
                    break; // Client disconnected
                } catch (ClassNotFoundException e) {
                    System.err.println("Error deserializing message: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("Connection error for user " + username + ": " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    private boolean handleAuthentication() throws IOException {
        try {
            // Receive auth type (LOGIN or REGISTER)
            String authType = (String) in.readObject();
            String receivedUsername = (String) in.readObject();
            String password = (String) in.readObject();

            boolean success = false;
            String responseMessage = "";

            if ("REGISTER".equals(authType)) {
                String email = (String) in.readObject();
                User newUser = new User(receivedUsername, password, email);
                success = userManager.registerUser(newUser);
                responseMessage = success ? "Registration successful" : "Username already exists";
            } else if ("LOGIN".equals(authType)) {
                User user = userManager.authenticateUser(receivedUsername, password);
                success = (user != null);
                responseMessage = success ? "Login successful" : "Invalid credentials";
            }

            // Send response
            out.writeObject(success);
            out.writeObject(responseMessage);
            out.flush();

            if (success) {
                this.username = receivedUsername;
                userManager.addOnlineUser(username, this);
                System.out.println("User authenticated: " + username);

                sendOnlineUsersList();

                broadcastUserListToAll();

                return true;
            } else {
                System.out.println("Authentication failed for: " + receivedUsername);
                return false;
            }

        } catch (ClassNotFoundException e) {
            System.err.println("Authentication error: " + e.getMessage());
            return false;
        }
    }

    private void handleMessage(Message message) throws IOException {
        System.out.println("Received message from " + message.getSender() +
                " [" + message.getType() + "]: " +
                (message.getContent() != null ? message.getContent() : message.getFileName()));

        switch (message.getType()) {
            case TEXT:
                // Broadcast to all users
                userManager.broadcastMessage(message, username);
                break;

            case PRIVATE:
                // Send to specific user
                handlePrivateMessage(message);
                break;

            case FILE:
                // Handle file transfer
                handleFileTransfer(message);
                break;

            case TYPING:
                // Broadcast typing indicator
                userManager.broadcastMessage(message, username);
                break;

            default:
                System.out.println("Unknown message type: " + message.getType());
        }
    }

    private void handlePrivateMessage(Message message) throws IOException {
        String receiver = message.getReceiver();
        ServerHandler receiverHandler = userManager.getOnlineUserHandler(receiver);

        if (receiverHandler != null) {
            // User is online, send immediately
            receiverHandler.sendUserMessage(message);

            // Send confirmation to sender
            Message confirmation = new Message("SYSTEM",
                    "Private message delivered to " + receiver,
                    Message.MessageType.SYSTEM);
            sendUserMessage(confirmation);
        } else {
            // User is offline, queue message
            userManager.addOfflineMessage(receiver, message);

            // Notify sender
            Message notification = new Message("SYSTEM",
                    receiver + " is offline. Message will be delivered when they come online.",
                    Message.MessageType.SYSTEM);
            sendUserMessage(notification);
        }
    }

    private void handleFileTransfer(Message message) throws IOException {
        String receiver = message.getReceiver();

        if (receiver == null) {
            // Broadcast file to all users
            userManager.broadcastMessage(message, username);
        } else {
            // Send to specific user
            ServerHandler receiverHandler = userManager.getOnlineUserHandler(receiver);
            if (receiverHandler != null) {
                receiverHandler.sendMessage(message);
            } else {
                userManager.addOfflineMessage(receiver, message);
            }
        }
    }

    private void sendOfflineMessages() throws IOException {
        List<Message> offlineMessages = userManager.getOfflineMessages(username);
        if (!offlineMessages.isEmpty()) {
            System.out.println("Sending " + offlineMessages.size() + " offline messages to: " + username);
            for (Message msg : offlineMessages) {
                sendUserMessage(msg);
            }
        }
    }


    private void sendOnlineUsersList() throws IOException {
        List<User> onlineUsers = userManager.getOnlineUsers();
        synchronized (out) {
            out.writeObject("USERS_LIST");
            out.writeObject(onlineUsers);
            out.flush();
        }
        System.out.println("Sent user list to " + username + " (" + onlineUsers.size() + " users)");
    }

    private void sendUserMessage(Message message) throws IOException {
        synchronized (out) {
            out.writeObject("MESSAGE");
            out.writeObject(message);
            out.flush();
        }
    }


    private void broadcastUserJoined() {
        Message joinMessage = new Message(username,
                username + " has joined the chat",
                Message.MessageType.USER_JOIN);
        userManager.broadcastMessage(joinMessage, username);
        

        broadcastUserListToAll();
    }
    private void broadcastUserListToAll() {
        List<User> onlineUsers = userManager.getOnlineUsers();

        // Get all online handlers including this one
        for (String user : userManager.getAllOnlineUsernames()) {
            ServerHandler handler = userManager.getOnlineUserHandler(user);
            if (handler != null) {
                try {
                    handler.sendUserListUpdate(onlineUsers);
                } catch (IOException e) {
                    System.err.println("Failed to send user list to: " + user);
                }
            }
        }
    }
    private void sendUserListUpdate(List<User> users) throws IOException {
        synchronized (out) {
            out.writeObject("USERS_LIST");
            out.writeObject(users);
            out.flush();
        }
    }
    private void broadcastUserLeft() {
        Message leaveMessage = new Message(username,
                username + " has left the chat",
                Message.MessageType.USER_LEAVE);
        userManager.broadcastMessage(leaveMessage, username);
    }

    public void sendMessage(Message message) throws IOException {
        synchronized (out) {
            out.writeObject("MESSAGE");
            out.writeObject(message);
            out.flush();
        }
    }

    private void cleanup() {
        running = false;

        if (username != null) {
            userManager.removeOnlineUser(username);
            broadcastUserLeft();

            broadcastUserListToAll();
        }

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }

        System.out.println("Client disconnected: " + (username != null ? username : "unknown"));
    }

    public void shutdown() {
        running = false;
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Error shutting down handler: " + e.getMessage());
        }
    }
}