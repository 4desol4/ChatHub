package com.chatapp.admin;

import com.chatapp.database.DatabaseConfig;
import com.chatapp.database.UserDAO;
import com.chatapp.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/* Run this to manage users, view stats, and perform maintenance*/
public class DatabaseManager {

    private static final Scanner scanner = new Scanner(System.in);
    private static final UserDAO userDAO = new UserDAO();

    public static void main(String[] args) {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║          ChatHub Database Manager                      ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        boolean running = true;
        while (running) {
            showMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> listAllUsers();
                case "2" -> searchUser();
                case "3" -> deleteUser();
                case "4" -> updateUserPassword();
                case "5" -> viewUserStats();
                case "6" -> viewMessageStats();
                case "7" -> cleanupDatabase();
                case "8" -> exportData();
                case "9" -> viewOnlineUsers();
                case "10" -> blockUser();
                case "0" -> {
                    System.out.println("\n👋 Goodbye!");
                    running = false;
                }
                default -> System.out.println("❌ Invalid option. Please try again.");
            }
        }

        scanner.close();
    }

    private static void showMenu() {
        System.out.println("\n┌──────────────────────────────────────┐");
        System.out.println("│          Main Menu                   │");
        System.out.println("├──────────────────────────────────────┤");
        System.out.println("│ 1. List All Users                    │");
        System.out.println("│ 2. Search User                       │");
        System.out.println("│ 3. Delete User                       │");
        System.out.println("│ 4. Update User Password              │");
        System.out.println("│ 5. View User Statistics              │");
        System.out.println("│ 6. View Message Statistics           │");
        System.out.println("│ 7. Cleanup Database                  │");
        System.out.println("│ 8. Export Data                       │");
        System.out.println("│ 9. View Online Users                 │");
        System.out.println("│ 10. Block/Unblock User               │");
        System.out.println("│ 0. Exit                              │");
        System.out.println("└──────────────────────────────────────┘");
        System.out.print("\nEnter your choice: ");
    }

    private static void listAllUsers() {
        System.out.println("\n📋 ALL USERS");
        System.out.println("═══════════════════════════════════════════════════════════════");

        String sql = "SELECT username, email, status, created_at, last_seen FROM users ORDER BY created_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;
            while (rs.next()) {
                count++;
                String username = rs.getString("username");
                String email = rs.getString("email");
                String status = rs.getString("status");
                Timestamp created = rs.getTimestamp("created_at");
                Timestamp lastSeen = rs.getTimestamp("last_seen");

                System.out.printf("\n%d. Username: %s\n", count, username);
                System.out.printf("   Email: %s\n", email);
                System.out.printf("   Status: %s\n", status);
                System.out.printf("   Registered: %s\n", formatTimestamp(created));
                System.out.printf("   Last Seen: %s\n", formatTimestamp(lastSeen));
                System.out.println("   ─────────────────────────────────────");
            }

            System.out.println("\nTotal Users: " + count);

        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void searchUser() {
        System.out.print("\n🔍 Enter username to search: ");
        String username = scanner.nextLine().trim();

        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n✅ USER FOUND");
                System.out.println("═══════════════════════════════════════");
                System.out.println("Username: " + rs.getString("username"));
                System.out.println("Email: " + rs.getString("email"));
                System.out.println("Status: " + rs.getString("status"));
                System.out.println("Avatar Color: " + rs.getString("avatar_color"));
                System.out.println("Created: " + formatTimestamp(rs.getTimestamp("created_at")));
                System.out.println("Last Seen: " + formatTimestamp(rs.getTimestamp("last_seen")));

                // Get message count
                int messageCount = getMessageCount(username);
                System.out.println("Total Messages Sent: " + messageCount);

            } else {
                System.out.println("❌ User not found!");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void deleteUser() {
        System.out.print("\n🗑️  Enter username to delete: ");
        String username = scanner.nextLine().trim();

        System.out.print("⚠️  Are you sure? This will delete all user data. (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes")) {
            boolean deleted = userDAO.deleteUser(username);

            if (deleted) {
                System.out.println("✅ User deleted successfully!");
            } else {
                System.out.println("❌ Failed to delete user. User may not exist.");
            }
        } else {
            System.out.println("❌ Deletion cancelled.");
        }
    }

    private static void updateUserPassword() {
        System.out.print("\n🔑 Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter new password: ");
        String newPassword = scanner.nextLine().trim();

        if (newPassword.length() < 6) {
            System.out.println("❌ Password must be at least 6 characters!");
            return;
        }

        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hash the new password
            String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt(12));

            stmt.setString(1, hashedPassword);
            stmt.setString(2, username);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Password updated successfully!");
            } else {
                System.out.println("❌ User not found!");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void viewUserStats() {
        System.out.println("\n📊 USER STATISTICS");
        System.out.println("═══════════════════════════════════════════════════════════════");

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // Total users
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users");
            if (rs.next()) {
                System.out.println("Total Users: " + rs.getInt("count"));
            }

            // Online users
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users WHERE status = 'ONLINE'");
            if (rs.next()) {
                System.out.println("Online Users: " + rs.getInt("count"));
            }

            // Users registered today
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users WHERE DATE(created_at) = CURDATE()");
            if (rs.next()) {
                System.out.println("New Users Today: " + rs.getInt("count"));
            }

            // Users registered this week
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)");
            if (rs.next()) {
                System.out.println("New Users This Week: " + rs.getInt("count"));
            }

            // Most active user
            rs = stmt.executeQuery(
                    "SELECT sender_username, COUNT(*) as msg_count " +
                            "FROM chat_history " +
                            "GROUP BY sender_username " +
                            "ORDER BY msg_count DESC " +
                            "LIMIT 1"
            );
            if (rs.next()) {
                System.out.println("\nMost Active User: " + rs.getString("sender_username") +
                        " (" + rs.getInt("msg_count") + " messages)");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void viewMessageStats() {
        System.out.println("\n💬 MESSAGE STATISTICS");
        System.out.println("═══════════════════════════════════════════════════════════════");

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // Total messages
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM chat_history");
            if (rs.next()) {
                System.out.println("Total Messages: " + rs.getInt("count"));
            }

            // Messages today
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM chat_history WHERE DATE(created_at) = CURDATE()");
            if (rs.next()) {
                System.out.println("Messages Today: " + rs.getInt("count"));
            }

            // Messages this week
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM chat_history WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)");
            if (rs.next()) {
                System.out.println("Messages This Week: " + rs.getInt("count"));
            }

            // File transfers
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM chat_history WHERE message_type = 'FILE'");
            if (rs.next()) {
                System.out.println("Total File Transfers: " + rs.getInt("count"));
            }

            // Average messages per user
            rs = stmt.executeQuery(
                    "SELECT AVG(msg_count) as avg_msgs FROM " +
                            "(SELECT sender_username, COUNT(*) as msg_count FROM chat_history GROUP BY sender_username) as subquery"
            );
            if (rs.next()) {
                System.out.printf("Average Messages per User: %.2f\n", rs.getDouble("avg_msgs"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void cleanupDatabase() {
        System.out.println("\n🧹 DATABASE CLEANUP");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("1. Delete old offline messages (delivered)");
        System.out.println("2. Delete messages older than 90 days");
        System.out.println("3. Delete inactive users (not logged in for 180 days)");
        System.out.println("0. Cancel");
        System.out.print("\nEnter choice: ");

        String choice = scanner.nextLine().trim();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            int rowsAffected = 0;

            switch (choice) {
                case "1" -> {
                    rowsAffected = stmt.executeUpdate("DELETE FROM offline_messages WHERE delivered = TRUE");
                    System.out.println("✅ Deleted " + rowsAffected + " delivered offline messages");
                }
                case "2" -> {
                    rowsAffected = stmt.executeUpdate(
                            "DELETE FROM chat_history WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY)"
                    );
                    System.out.println("✅ Deleted " + rowsAffected + " old messages");
                }
                case "3" -> {
                    rowsAffected = stmt.executeUpdate(
                            "DELETE FROM users WHERE last_seen < DATE_SUB(NOW(), INTERVAL 180 DAY) AND status = 'OFFLINE'"
                    );
                    System.out.println("✅ Deleted " + rowsAffected + " inactive users");
                }
                case "0" -> System.out.println("❌ Cancelled");
                default -> System.out.println("❌ Invalid option");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void exportData() {
        System.out.println("\n📤 EXPORT DATA");
        System.out.println("This feature will export all users to a CSV file.");
        System.out.print("Enter filename (without .csv): ");
        String filename = scanner.nextLine().trim() + ".csv";

        String sql = "SELECT username, email, status, created_at, last_seen FROM users";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);
             java.io.PrintWriter writer = new java.io.PrintWriter(filename)) {

            // Write header
            writer.println("Username,Email,Status,Created,LastSeen");

            // Write data
            while (rs.next()) {
                writer.printf("%s,%s,%s,%s,%s\n",
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("last_seen")
                );
            }

            System.out.println("✅ Data exported to " + filename);

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void viewOnlineUsers() {
        System.out.println("\n🟢 ONLINE USERS");
        System.out.println("═══════════════════════════════════════════════════════════════");

        String sql = "SELECT username, email, last_seen FROM users WHERE status = 'ONLINE'";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;
            while (rs.next()) {
                count++;
                System.out.printf("%d. %s (%s) - Last seen: %s\n",
                        count,
                        rs.getString("username"),
                        rs.getString("email"),
                        formatTimestamp(rs.getTimestamp("last_seen"))
                );
            }

            if (count == 0) {
                System.out.println("No users currently online.");
            } else {
                System.out.println("\nTotal: " + count + " users online");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    private static void blockUser() {
        System.out.print("\n🚫 Enter username to block/unblock: ");
        String username = scanner.nextLine().trim();

        // Note: You'd need to add a 'blocked' column to users table for this
        System.out.println("⚠️  This feature requires adding a 'blocked' column to the users table.");
        System.out.println("Run this SQL first:");
        System.out.println("ALTER TABLE users ADD COLUMN blocked BOOLEAN DEFAULT FALSE;");
    }

    private static int getMessageCount(String username) {
        String sql = "SELECT COUNT(*) as count FROM chat_history WHERE sender_username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }

        return 0;
    }

    private static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "Never";
        LocalDateTime ldt = timestamp.toLocalDateTime();
        return ldt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }
}