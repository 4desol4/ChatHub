package com.chatapp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private static final int PORT = 5000;
    private static final int MAX_CLIENTS = 100;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private List<ServerHandler> clientHandlers;
    private volatile boolean running;
    private UserManager userManager;

    public ChatServer() {
        clientHandlers = new ArrayList<>();
        threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        userManager = UserManager.getInstance();
        running = false;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;

            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║     CHAT SERVER STARTED                ║");
            System.out.println("║     Port: " + PORT + "                         ║");
            System.out.println("║     Waiting for clients...             ║");
            System.out.println("╚════════════════════════════════════════╝");

            // Accept clients in loop
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ServerHandler handler = new ServerHandler(clientSocket);
                    clientHandlers.add(handler);
                    threadPool.execute(handler);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        System.out.println("\nShutting down server...");
        running = false;

        // Close all client connections
        for (ServerHandler handler : clientHandlers) {
            handler.shutdown();
        }
        clientHandlers.clear();

        // Shutdown thread pool
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        // Shutdown user manager
        userManager.shutdown();

        System.out.println("Server shutdown complete");
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown signal received...");
            server.shutdown();
        }));

        server.start();
    }
}
