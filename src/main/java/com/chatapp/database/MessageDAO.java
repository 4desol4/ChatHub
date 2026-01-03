package com.chatapp.database;

import com.chatapp.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {


    public boolean saveOfflineMessage(Message message) {
        String sql = "INSERT INTO offline_messages (sender_username, receiver_username, content, message_type, file_name, file_data) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getSender());
            stmt.setString(2, message.getReceiver());
            stmt.setString(3, message.getContent());
            stmt.setString(4, message.getType().name());
            stmt.setString(5, message.getFileName());
            stmt.setBytes(6, message.getFileData());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error saving offline message: " + e.getMessage());
        }

        return false;
    }

    // Get offline messages for user
    public List<Message> getOfflineMessages(String username) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM offline_messages WHERE receiver_username = ? AND delivered = FALSE";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String sender = rs.getString("sender_username");
                String content = rs.getString("content");
                Message.MessageType type = Message.MessageType.valueOf(rs.getString("message_type"));

                Message message;
                if (type == Message.MessageType.FILE) {
                    String fileName = rs.getString("file_name");
                    byte[] fileData = rs.getBytes("file_data");
                    message = new Message(sender, fileName, fileData);
                } else {
                    message = new Message(sender, username, content);
                }

                messages.add(message);
            }

            // Mark messages as delivered
            markMessagesAsDelivered(username);

        } catch (SQLException e) {
            System.err.println("❌ Error fetching offline messages: " + e.getMessage());
        }

        return messages;
    }

    private void markMessagesAsDelivered(String username) {
        String sql = "UPDATE offline_messages SET delivered = TRUE WHERE receiver_username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Error marking messages as delivered: " + e.getMessage());
        }
    }


    public boolean saveToChatHistory(Message message) {
        String sql = "INSERT INTO chat_history (sender_username, receiver_username, content, message_type, file_name) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getSender());
            stmt.setString(2, message.getReceiver());
            stmt.setString(3, message.getContent());
            stmt.setString(4, message.getType().name());
            stmt.setString(5, message.getFileName());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error saving to chat history: " + e.getMessage());
        }

        return false;
    }

    // Get chat history between two users
    public List<Message> getChatHistory(String user1, String user2, int limit) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM chat_history " +
                "WHERE (sender_username = ? AND receiver_username = ?) " +
                "OR (sender_username = ? AND receiver_username = ?) " +
                "OR (sender_username = ? AND receiver_username IS NULL) " +
                "OR (sender_username = ? AND receiver_username IS NULL) " +
                "ORDER BY created_at DESC LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user1);
            stmt.setString(2, user2);
            stmt.setString(3, user2);
            stmt.setString(4, user1);
            stmt.setString(5, user1);
            stmt.setString(6, user2);
            stmt.setInt(7, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String sender = rs.getString("sender_username");
                String receiver = rs.getString("receiver_username");
                String content = rs.getString("content");
                Message.MessageType type = Message.MessageType.valueOf(rs.getString("message_type"));

                Message message;
                if (receiver != null) {
                    message = new Message(sender, receiver, content);
                } else {
                    message = new Message(sender, content, type);
                }

                messages.add(message);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching chat history: " + e.getMessage());
        }

        return messages;
    }
}