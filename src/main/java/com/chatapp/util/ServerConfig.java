package com.chatapp.util;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;



public class ServerConfig {
    private static final String CONFIG_FILE = "server.properties";
    private static Properties properties = new Properties();


    private static final String DEFAULT_SERVER_IP = "192.168.12.153";
    private static final String DEFAULT_SERVER = "169.254.150.63";
    private static final int DEFAULT_PORT = 5000;
    private static final int CONNECTION_TIMEOUT = 3000; // 3 seconds

    private static String activeHost = null;
    private static int activePort = DEFAULT_PORT;

    static {
        loadConfig();
    }


    private static void loadConfig() {
        // Load config file if it exists
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                System.out.println("✅ Loaded server.properties");
            } catch (IOException e) {
                System.err.println("⚠️  Could not load server.properties: " + e.getMessage());
            }
        } else {
            System.out.println("ℹ️  No server.properties found, using auto-detection");
        }

        // Determine best host
        activeHost = findBestHost();
        activePort = getPortFromConfig();

        System.out.println("✅ Active connection: " + activeHost + ":" + activePort);
    }


    private static String findBestHost() {
        System.out.println("\n🔍 Finding best server connection...");

        // Strategy 1: Try config file first
        String configHost = properties.getProperty("server.host");
        if (configHost != null && !configHost.trim().isEmpty()) {
            System.out.println("📋 Trying configured host: " + configHost);
            if (testConnection(configHost, getPortFromConfig())) {
                System.out.println("✅ Connected to configured server: " + configHost);
                return configHost;
            } else {
                System.out.println("❌ Configured server not reachable");
            }
        }

        // Strategy 2: Try localhost (same PC)
        System.out.println("📋 Trying localhost...");
        if (testConnection("localhost", getPortFromConfig())) {
            System.out.println("✅ Connected to localhost");
            return "localhost";
        } else {
            System.out.println("❌ Localhost not reachable");
        }

        // Strategy 3: Try default server IP (network)
        System.out.println("📋 Trying network server: " + DEFAULT_SERVER_IP);
        if (testConnection(DEFAULT_SERVER_IP, getPortFromConfig())) {
            System.out.println("✅ Connected to network server: " + DEFAULT_SERVER_IP);
            return DEFAULT_SERVER_IP;
        } else {
            System.out.println("❌ Network server not reachable");
        }

        System.out.println("📋 Trying network server: " + DEFAULT_SERVER);
        if (testConnection(DEFAULT_SERVER, getPortFromConfig())) {
            System.out.println("✅ Connected to network server: " + DEFAULT_SERVER);
            return DEFAULT_SERVER;
        } else {
            System.out.println("❌ Network server not reachable");
        }


        // Fallback: Use localhost anyway (will show error when connecting)
        System.out.println("⚠️  No server found, defaulting to localhost");
        System.out.println("💡 Server must be started before connecting");
        return "localhost";
    }

    /**
     * Test if server is reachable
     */
    private static boolean testConnection(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get port from config or use default
     */
    private static int getPortFromConfig() {
        String portStr = properties.getProperty("server.port", String.valueOf(DEFAULT_PORT));
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }


    public static String getServerHost() {
        return activeHost;
    }

    /**
     * Get the active server port
     */
    public static int getServerPort() {
        return activePort;
    }

    /**
     * Get connection timeout
     */
    public static int getConnectionTimeout() {
        String timeoutStr = properties.getProperty("connection.timeout", "30000");
        try {
            return Integer.parseInt(timeoutStr);
        } catch (NumberFormatException e) {
            return 30000;
        }
    }

    /**
     * Force re-detection of best server
     */
    public static void refreshConnection() {
        activeHost = findBestHost();
        activePort = getPortFromConfig();
    }

    /**
     * Create a default configuration file with instructions
     */
    public static void createDefaultConfig() {
        Properties defaultProps = new Properties();
        defaultProps.setProperty("server.host", DEFAULT_SERVER_IP);
        defaultProps.setProperty("server.port", String.valueOf(DEFAULT_PORT));
        defaultProps.setProperty("connection.timeout", "30000");

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            String comments = """
                    ChatHub Server Configuration (OPTIONAL)
                    ========================================
                    
                    NOTE: This file is OPTIONAL!
                    ChatHub will automatically try to find the server:
                      1. First tries this configured IP
                      2. Then tries localhost (same PC)
                      3. Then tries default network IP
                    
                    Only create this file if auto-detection doesn't work.
                    
                    To connect to a specific server, uncomment and edit:
                    """;

            defaultProps.store(fos, comments);
            System.out.println("✅ Created template server.properties");

        } catch (IOException e) {
            System.err.println("❌ Could not create config file: " + e.getMessage());
        }
    }

    /**
     * Display current configuration
     */
    public static void printConfig() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║    ChatHub Connection Settings         ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.printf("║ Active Host: %-25s ║%n", activeHost);
        System.out.printf("║ Active Port: %-25d ║%n", activePort);
        System.out.printf("║ Timeout:     %-25dms ║%n", getConnectionTimeout());
        System.out.println("╚════════════════════════════════════════╝\n");
    }


    public static String getServerAddress() {
        return activeHost + ":" + activePort;
    }


    public static boolean isLocalhost() {
        return "localhost".equals(activeHost) || "127.0.0.1".equals(activeHost);
    }

    public static void setServer(String host, int port) {
        if (testConnection(host, port)) {
            activeHost = host;
            activePort = port;
            System.out.println("✅ Manually set server: " + host + ":" + port);
        } else {
            System.err.println("❌ Cannot connect to: " + host + ":" + port);
        }
    }
}