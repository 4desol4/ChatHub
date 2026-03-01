package com.chatapp.database;

import com.chatapp.model.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    /**
     * Save message to chat history
     */
    public boolean saveToChatHistory(Message message) {
        if (message.getType() == Message.MessageType.SYSTEM ||
                message.getType() == Message.MessageType.TYPING ||
                message.getType() == Message.MessageType.USER_JOIN ||
                message.getType() == Message.MessageType.USER_LEAVE) {
            // Don't save these to history
            return true;
        }
        if (message.getType() == Message.MessageType.TEXT &&
                (message.getReceiver() == null || message.getReceiver().isEmpty())) {
            // Don't save broadcast messages
            return true;
        }

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
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get chat history between two users
     */
    public List<Message> getChatHistory(String user1, String user2, int limit) {
        List<Message> messages = new ArrayList<>();

        String sql = "SELECT * FROM chat_history " +
                "WHERE is_deleted = FALSE " +
                "AND ((sender_username = ? AND receiver_username = ?) " +
                "OR (sender_username = ? AND receiver_username = ?)) " +
                "ORDER BY created_at ASC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user1);
            stmt.setString(2, user2);
            stmt.setString(3, user2);
            stmt.setString(4, user1);
            stmt.setInt(5, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Message message = buildMessageFromResultSet(rs);
                messages.add(message);
            }

            System.out.println("✅ Loaded " + messages.size() + " messages for " + user1 + " <-> " + user2);

        } catch (SQLException e) {
            System.err.println("❌ Error fetching chat history: " + e.getMessage());
            e.printStackTrace();
        }

        return messages;
    }

    /**
     * Delete chat history for a user (soft delete)
     */
    public boolean deleteChatHistory(String user1, String user2, String deletedBy) {
        String sql = "UPDATE chat_history SET " +
                "is_deleted = TRUE, " +
                "deleted_by = ?, " +
                "deleted_at = CURRENT_TIMESTAMP " +
                "WHERE ((sender_username = ? AND receiver_username = ?) " +
                "OR (sender_username = ? AND receiver_username = ?))";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, deletedBy);
            stmt.setString(2, user1);
            stmt.setString(3, user2);
            stmt.setString(4, user2);
            stmt.setString(5, user1);

            int rowsAffected = stmt.executeUpdate();
            System.out.println("🗑️ Deleted " + rowsAffected + " messages between " + user1 + " and " + user2);
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting chat history: " + e.getMessage());
        }

        return false;
    }

    /**
     * Get message count between two users
     */
    public int getMessageCount(String user1, String user2) {
        String sql = "SELECT COUNT(*) as count FROM chat_history " +
                "WHERE is_deleted = FALSE " +
                "AND ((sender_username = ? AND receiver_username = ?) " +
                "OR (sender_username = ? AND receiver_username = ?))";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user1);
            stmt.setString(2, user2);
            stmt.setString(3, user2);
            stmt.setString(4, user1);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error counting messages: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Save offline message
     */
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

    /**
     * Get offline messages for user
     */
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

    /**
     * Build Message object from ResultSet
     */
    private Message buildMessageFromResultSet(ResultSet rs) throws SQLException {
        String sender = rs.getString("sender_username");
        String receiver = rs.getString("receiver_username");
        String content = rs.getString("content");
        Message.MessageType type = Message.MessageType.valueOf(rs.getString("message_type"));
        Timestamp timestamp = rs.getTimestamp("created_at");

        Message message;
        if (type == Message.MessageType.FILE) {
            String fileName = rs.getString("file_name");
            message = new Message(sender, fileName, new byte[0]); // Don't load file data for history
            message.setReceiver(receiver);
        } else {
            message = new Message(sender, receiver, content);
        }

        // Set timestamp from database
        if (timestamp != null) {
            message.setTimestamp(timestamp.toLocalDateTime());
        }

        return message;
    }
}