package com.chatapp.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class EnvConfig {
    private static final Map<String, String> envVars = new HashMap<>();
    private static final String ENV_FILE = ".env";

    static {
        loadEnvFile();
    }


    private static void loadEnvFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(ENV_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse key=value pairs
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();

                        // Remove quotes if present
                        value = value.replaceAll("^\"|\"$", "")
                                      .replaceAll("^'|'$", "");

                        envVars.put(key, value);
                    }
                }
            }
            System.out.println("✅ Environment configuration loaded from .env file");

        } catch (IOException e) {
            System.err.println("⚠️  .env file not found. Using fallback/default values.");
            System.err.println("   Please create a .env file in your project root.");
            System.err.println("   You can copy from .env.example");
        }
    }


    public static String get(String key) {
        // First try .env file
        if (envVars.containsKey(key)) {
            return envVars.get(key);
        }

        // Fallback to system environment variables
        String value = System.getenv(key);
        if (value != null) {
            return value;
        }

        System.err.println("⚠️  Environment variable not found: " + key);
        return null;
    }


    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    public static Map<String, String> getAll() {
        return new HashMap<>(envVars);
    }
}
