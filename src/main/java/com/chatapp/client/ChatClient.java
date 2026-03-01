package com.chatapp.client;

import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.util.NetworkUtil;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javafx.application.Platform;

public class ChatClient {
    private static ChatClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private volatile boolean connected;
    private Thread listenerThread;

    private List<MessageListener> messageListeners;
    private List<UserStatusListener> userStatusListeners;

    // Interfaces for callbacks
    public interface MessageListener {
        void onMessageReceived(Message message);
    }

    public interface UserStatusListener {
        void onUserListUpdated(List<User> users);

        void onUserJoined(String username);

        void onUserLeft(String username);
    }

    private ChatClient() {
        messageListeners = new CopyOnWriteArrayList<>();
        userStatusListeners = new CopyOnWriteArrayList<>();
        connected = false;
    }

    public static synchronized ChatClient getInstance() {
        if (instance == null) {
            instance = new ChatClient();
        }
        return instance;
    }

    // Connection Management
    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            connected = true;
            System.out.println("✅ Connected to server: " + host + ":" + port);
            return true;

        } catch (IOException e) {
            System.err.println("❌ Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    // Authentication
    public boolean login(String username, String password) throws IOException, ClassNotFoundException {
        out.writeObject("LOGIN");
        out.writeObject(username);
        out.writeObject(password);
        out.flush();

        boolean success = (boolean) in.readObject();
        String message = (String) in.readObject();

        if (success) {
            this.username = username;
            System.out.println("✅ Login successful: " + username);
            startMessageListener();
        } else {
            System.out.println("❌ Login failed: " + message);
        }

        return success;
    }

    public boolean register(String username, String password, String email)
            throws IOException, ClassNotFoundException {
        out.writeObject("REGISTER");
        out.writeObject(username);
        out.writeObject(password);
        out.writeObject(email);
        out.flush();

        boolean success = (boolean) in.readObject();
        String message = (String) in.readObject();

        if (success) {
            this.username = username;
            System.out.println("✅ Registration successful: " + username);
            startMessageListener();
        } else {
            System.out.println("❌ Registration failed: " + message);
        }

        return success;
    }

    // Start listening for messages from server
    private void startMessageListener() {
        System.out.println("🎧 Starting message listener for: " + username);
        listenerThread = new Thread(new ClientHandler());
        listenerThread.setDaemon(true);
        listenerThread.start();
    }


    private class ClientHandler implements Runnable {
        @Override
        public void run() {
            System.out.println("🟢 ClientHandler thread started for: " + username);

            try {
                while (connected) {
                    try {
                        String command = (String) in.readObject();
                        System.out.println("📨 Command received: " + command);

                        if ("USERS_LIST".equals(command)) {
                            @SuppressWarnings("unchecked")
                            List<User> users = (List<User>) in.readObject();
                            System.out.println("✅ Received user list: " + users.size() + " users");
                            for (User user : users) {
                                System.out.println("   👤 " + user.getUsername() + " [" + user.getStatus() + "]");
                            }
                            notifyUserListUpdated(users);
                        } else if ("MESSAGE".equals(command)) {
                            Message message = (Message) in.readObject();
                            System.out.println("📩 Received message: " + message.getType() + " from " + message.getSender());
                            handleIncomingMessage(message);
                        } else {
                            System.err.println("⚠️ Unknown command: " + command);
                        }

                    } catch (EOFException e) {
                        System.out.println("🔌 Server closed connection");
                        break;
                    } catch (ClassNotFoundException e) {
                        System.err.println("❌ Class not found: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("❌ Connection error: " + e.getMessage());
                    handleDisconnection();
                }
            } finally {
                System.out.println("🔴 ClientHandler stopped for: " + username);
            }
        }

        private void handleIncomingMessage(Message message) {
            System.out.println("📩 Processing message: " + message.getType() + " from " + message.getSender());

            // Notify all message listeners
            notifyMessageListeners(message);

            // Handle user status changes
            if (message.getType() == Message.MessageType.USER_JOIN) {
                notifyUserJoined(message.getSender());
            } else if (message.getType() == Message.MessageType.USER_LEAVE) {
                notifyUserLeft(message.getSender());
            }
        }
    }

    // Message Sending
    public void sendMessage(String content) {
        try {
            Message message = new Message(username, content, Message.MessageType.TEXT);
            NetworkUtil.sendMessage(out, message);
            System.out.println("📤 Sent message: " + content);
        } catch (IOException e) {
            System.err.println("❌ Failed to send message: " + e.getMessage());
            handleDisconnection();
        }
    }

    public void sendPrivateMessage(String receiver, String content) {
        try {
            Message message = new Message(username, receiver, content);
            NetworkUtil.sendMessage(out, message);
            System.out.println("📤 Sent private message to " + receiver);
        } catch (IOException e) {
            System.err.println("❌ Failed to send private message: " + e.getMessage());
            handleDisconnection();
        }
    }

    public void sendFile(String fileName, byte[] fileData, String receiver) {
        try {
            Message message = new Message(username, fileName, fileData);
            if (receiver != null && !receiver.isEmpty()) {
                message.setReceiver(receiver);
            }
            NetworkUtil.sendMessage(out, message);
            System.out.println("📤 Sent file: " + fileName);
        } catch (IOException e) {
            System.err.println("❌ Failed to send file: " + e.getMessage());
            handleDisconnection();
        }
    }

    public void sendTypingIndicator() {
        try {
            Message message = new Message(username, "", Message.MessageType.TYPING);
            NetworkUtil.sendMessage(out, message);
        } catch (IOException e) {
            // Silently fail for typing indicators
        }
    }

    // Listener Management
    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
        System.out.println("✅ Message listener added");
    }

    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }

    public void addUserStatusListener(UserStatusListener listener) {
        userStatusListeners.add(listener);
        System.out.println("✅ User status listener added");
    }

    public void removeUserStatusListener(UserStatusListener listener) {
        userStatusListeners.remove(listener);
    }

    private void notifyMessageListeners(Message message) {
        System.out.println("🔔 Notifying " + messageListeners.size() + " message listeners");
        List<MessageListener> snapshot = new ArrayList<>(messageListeners);
        for (MessageListener listener : snapshot) {
            Platform.runLater(() -> {
                try {
                    listener.onMessageReceived(message);
                    System.out.println("✅ Message listener notified successfully");
                } catch (Exception e) {
                    System.err.println("❌ Error in message listener: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
    /**
     * Request updated user list from server
     */
    public void requestUserList() {
        try {
            System.out.println("📤 Requesting user list from server...");
            Message request = new Message(username, "REQUEST_USERS", Message.MessageType.SYSTEM);
            NetworkUtil.sendMessage(out, request);
            System.out.println("✅ User list request sent");
        } catch (IOException e) {
            System.err.println("❌ Failed to request user list: " + e.getMessage());
        }
    }
    private void notifyUserListUpdated(List<User> users) {
        System.out.println("🔔 Notifying " + userStatusListeners.size() + " listeners about user list update");
        List<UserStatusListener> snapshot = new ArrayList<>(userStatusListeners);
        for (UserStatusListener listener : snapshot) {
            Platform.runLater(() -> {
                try {
                    listener.onUserListUpdated(users);
                    System.out.println("✅ User list listener notified successfully");
                } catch (Exception e) {
                    System.err.println("❌ Error in user list listener: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    private void notifyUserJoined(String username) {
        List<UserStatusListener> snapshot = new ArrayList<>(userStatusListeners);
        for (UserStatusListener listener : snapshot) {
            listener.onUserJoined(username);
        }
    }

    private void notifyUserLeft(String username) {
        List<UserStatusListener> snapshot = new ArrayList<>(userStatusListeners);
        for (UserStatusListener listener : snapshot) {
            listener.onUserLeft(username);
        }
    }

    private void handleDisconnection() {
        connected = false;
        System.out.println("🔌 Disconnected from server");

        // Notify about disconnection with a system message
        Message disconnectMsg = new Message("SYSTEM",
                "Connection lost. Please restart the application.",
                Message.MessageType.SYSTEM);

        for (MessageListener listener : messageListeners) {
            listener.onMessageReceived(disconnectMsg);
        }
    }

    // Getters
    public boolean isConnected() {
        return connected && socket != null && socket.isConnected();
    }

    public String getUsername() {
        return username;
    }

    // Disconnect
    public void disconnect() {
        connected = false;

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }

        System.out.println("Client disconnected");
    }
}