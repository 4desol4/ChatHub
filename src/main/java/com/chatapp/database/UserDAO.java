package com.chatapp.database;

import com.chatapp.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // Create new user
    public boolean createUser(String username, String password, String email) {
        String sql = "INSERT INTO users (username, password_hash, email, avatar_color) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {


            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));


            String avatarColor = generateAvatarColor(username);

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, email);
            stmt.setString(4, avatarColor);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ User registered: " + username);
                return true;
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("❌ Username or email already exists: " + username);
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // Authenticate user
    public User authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                // Verify password with BCrypt
                if (BCrypt.checkpw(password, storedHash)) {
                    User user = new User(
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("email")
                    );


                    user.setStatus(User.Status.valueOf(rs.getString("status")));

                    System.out.println("✅ User authenticated: " + username);
                    return user;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Authentication error: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // Update user status
    public boolean updateUserStatus(String username, User.Status status) {
        String sql = "UPDATE users SET status = ? WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setString(2, username);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error updating status: " + e.getMessage());
        }

        return false;
    }

    // Get all online users
    public List<User> getOnlineUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE status = 'ONLINE'";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User(
                        rs.getString("username"),
                        "", // Don't include password
                        rs.getString("email")
                );
                user.setStatus(User.Status.ONLINE);
                users.add(user);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching online users: " + e.getMessage());
        }

        return users;
    }

    // Get all users
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User(
                        rs.getString("username"),
                        "",
                        rs.getString("email")
                );
                user.setStatus(User.Status.valueOf(rs.getString("status")));
                users.add(user);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching users: " + e.getMessage());
        }

        return users;
    }

    // Check if user exists
    public boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error checking user existence: " + e.getMessage());
        }

        return false;
    }

    // Get user by username
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                        rs.getString("username"),
                        "",
                        rs.getString("email")
                );
                user.setStatus(User.Status.valueOf(rs.getString("status")));
                return user;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching user: " + e.getMessage());
        }

        return null;
    }

    // Delete user (for testing)
    public boolean deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error deleting user: " + e.getMessage());
        }

        return false;
    }

    private String generateAvatarColor(String username) {
        String[] colors = {
                "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A",
                "#98D8C8", "#F7B731", "#5F27CD", "#00D2D3",
                "#FF6348", "#2ECC71", "#3498DB", "#9B59B6"
        };
        int index = Math.abs(username.hashCode()) % colors.length;
        return colors[index];
    }
}