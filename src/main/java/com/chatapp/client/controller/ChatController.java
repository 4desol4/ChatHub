package com.chatapp.client.controller;

import com.chatapp.client.ChatClient;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.chatapp.database.MessageDAO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.FlowPane;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChatController {

    @FXML
    private Label currentUserLabel;
    @FXML
    private StackPane userAvatarPane;
    @FXML
    private Label userAvatarLabel;
    @FXML
    private ListView<User> usersList;
    @FXML
    private Label onlineCountLabel;
    @FXML
    private Label onlineUsersCountLabel;
    @FXML
    private VBox messagesContainer;
    @FXML
    private ScrollPane messagesScrollPane;
    @FXML
    private TextArea messageInput;
    @FXML
    private HBox typingIndicatorBox;
    @FXML
    private TextField searchField;
    @FXML
    private Label chatHeaderTitle;
    @FXML
    private Label chatHeaderStatus;
    @FXML
    private StackPane chatHeaderAvatar;
    @FXML
    private Circle chatHeaderStatusIndicator;

    private ChatClient chatClient;
    private Timer typingTimer;
    private boolean isTyping = false;
    private String selectedUser = null;
    private static final int MAX_MESSAGES_DISPLAYED = 100;
    private MessageDAO messageDAO = new MessageDAO();

    // Store messages per user - Map<username, List of messages with that user>
    private Map<String, List<javafx.scene.Node>> userMessages = new HashMap<>();

    @FXML
    public void initialize() {
        chatClient = ChatClient.getInstance();

        // Set current user info
        currentUserLabel.setText(chatClient.getUsername());
        userAvatarLabel.setText(getInitials(chatClient.getUsername()));

        // Set avatar color
        String avatarColor = generateAvatarColor(chatClient.getUsername());
        userAvatarPane.setStyle("-fx-background-color: " + avatarColor + ";");

        // Configure messages scroll pane
        messagesScrollPane.setVvalue(1.0);
        messagesScrollPane.setFitToWidth(true);
        VBox.setVgrow(messagesScrollPane, Priority.ALWAYS);

        // Setup user list
        setupUsersList();

        // Setup message input
        setupMessageInput();

        // Setup search functionality
        setupSearch();

        // Setup responsive layout
        setupResponsiveLayout();

        // ⚠️ IMPORTANT: Register listeners IMMEDIATELY (before server sends data)
        System.out.println("🎯 Registering listeners...");
        registerListeners();

        // Welcome message
        addSystemMessage("Welcome to ChatHub, " + chatClient.getUsername() + "! 🎉");
        addSystemMessage("💬 Select a user from the list to start a one-on-one conversation");

        // Request fresh user list after everything is set up
        Platform.runLater(() -> {
            System.out.println("📋 Requesting user list...");
            chatClient.requestUserList();
        });

        // Entrance animation
        playEntranceAnimation();
    }

    /**
     * Register all listeners for messages and user status
     */
    private void registerListeners() {
        System.out.println("🎯 Registering message listener...");
        chatClient.addMessageListener(this::handleIncomingMessage);

        System.out.println("🎯 Registering user status listener...");
        chatClient.addUserStatusListener(new ChatClient.UserStatusListener() {
            @Override
            public void onUserListUpdated(List<User> users) {
                System.out.println("🎯 UserStatusListener triggered: " + users.size() + " users");
                Platform.runLater(() -> {
                    System.out.println("🎯 Updating UI with users...");
                    updateUsersList(users);
                });
            }

            @Override
            public void onUserJoined(String username) {
                System.out.println("🎯 User joined: " + username);
                Platform.runLater(() -> {
                    addSystemMessage(username + " joined the chat");
                });
            }

            @Override
            public void onUserLeft(String username) {
                System.out.println("🎯 User left: " + username);
                Platform.runLater(() -> {
                    addSystemMessage(username + " left the chat");
                });
            }
        });

        System.out.println("✅ All listeners registered!");
    }

    private void setupResponsiveLayout() {
        messagesScrollPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Delay to ensure window is initialized
                javafx.application.Platform.runLater(() -> {
                    if (newScene.getWindow() != null) {
                        newScene.getWindow().widthProperty().addListener((obs2, oldWidth, newWidth) -> {
                            adjustLayoutForWindowSize(newWidth.doubleValue());
                        });
                    }
                });
            }
        });
    }

    private void adjustLayoutForWindowSize(double width) {
        if (width < 800) {
            // Small window - reduce padding
            messagesContainer.setPadding(new Insets(10));
        } else if (width < 1200) {
            // Medium window
            messagesContainer.setPadding(new Insets(15));
        } else {
            // Large window
            messagesContainer.setPadding(new Insets(20));
        }
    }

    private void setupUsersList() {
        usersList.setCellFactory(param -> new ListCell<User>() {
            private final HBox container = new HBox(12);
            private final StackPane avatarPane = new StackPane();
            private final Label avatarLabel = new Label();
            private final VBox infoBox = new VBox(2);
            private final Label nameLabel = new Label();
            private final Label statusLabel = new Label();
            private final Circle statusIndicator = new Circle(4);

            {
                // Setup avatar
                avatarPane.setMinSize(45, 45);
                avatarPane.setMaxSize(45, 45);
                avatarPane.getStyleClass().add("avatar");
                avatarLabel.getStyleClass().add("avatar-text");
                avatarPane.getChildren().add(avatarLabel);

                // Setup info
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
                statusLabel.setStyle("-fx-font-size: 11px;");
                infoBox.getChildren().addAll(nameLabel, statusLabel);

                // Setup status indicator
                statusIndicator.setEffect(new javafx.scene.effect.DropShadow(8, Color.web("#00D9A5", 0.6)));

                container.setAlignment(Pos.CENTER_LEFT);
                container.getChildren().addAll(avatarPane, infoBox, new Region(), statusIndicator);
                HBox.setHgrow(container.getChildren().get(2), Priority.ALWAYS);
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);

                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    nameLabel.setText(user.getUsername());
                    avatarLabel.setText(getInitials(user.getUsername()));
                    avatarPane.setStyle("-fx-background-color: " + generateAvatarColor(user.getUsername()) + ";");


                    if (user.getStatus() == User.Status.ONLINE) {
                        statusLabel.setText("Online");
                        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #00D9A5;");
                        statusIndicator.setFill(Color.web("#00D9A5"));
                        statusIndicator.setVisible(true);

                        // Make it slightly brighter
                        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
                        setOpacity(1.0);

                    } else {
                        statusLabel.setText("Offline");
                        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B6B8A;");
                        statusIndicator.setFill(Color.web("#6B6B8A"));
                        statusIndicator.setVisible(true);

                        // Dim offline users
                        nameLabel.setStyle("-fx-font-weight: normal; -fx-font-size: 14px; -fx-text-fill: #A0A0B8;");
                        setOpacity(0.6);
                    }

                    setGraphic(container);
                }
            }
        });

        // Handle user selection - same as before
        usersList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = newVal.getUsername();
                messageInput.setPromptText("Message to " + selectedUser + "...");

                // Update chat header
                updateChatHeader(selectedUser);

                // Load chat history
                loadChatHistoryForUser(selectedUser);
            } else {
                selectedUser = null;
                messageInput.setPromptText("Type your message...");

                messagesContainer.getChildren().clear();
                addSystemMessage("💬 Select a user to start chatting");
            }
        });
    }

    private void loadChatHistoryForUser(String otherUsername) {
        System.out.println("📚 Loading chat history with: " + otherUsername);

        // Clear current messages
        messagesContainer.getChildren().clear();

        // Show loading indicator
        addSystemMessage("Loading chat history...");

        // Load history in background thread
        new Thread(() -> {
            try {
                List<Message> history = messageDAO.getChatHistory(
                        chatClient.getUsername(),
                        otherUsername,
                        100  // Load last 100 messages
                );

                Platform.runLater(() -> {
                    // Clear loading message
                    messagesContainer.getChildren().clear();

                    if (history.isEmpty()) {
                        addSystemMessage("No previous messages with " + otherUsername + ". Start the conversation! 👋");
                    } else {
                        // Display messages grouped by date
                        displayChatHistory(history);

                        // Auto-scroll to bottom
                        messagesScrollPane.setVvalue(1.0);
                    }
                });

            } catch (Exception e) {
                System.err.println("❌ Error loading chat history: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    messagesContainer.getChildren().clear();
                    addSystemMessage("⚠️ Could not load chat history");
                });
            }
        }).start();
    }

    /**
     * Display chat history with date separators
     */
    private void displayChatHistory(List<Message> messages) {
        LocalDate lastDate = null;

        for (Message message : messages) {
            // Check if we need a date separator
            LocalDate messageDate = message.getTimestamp().toLocalDate();

            if (lastDate == null || !lastDate.equals(messageDate)) {
                addDateSeparator(messageDate);
                lastDate = messageDate;
            }

            // Display the message
            if (message.getSender().equals(chatClient.getUsername())) {
                // My message
                addHistoricalMyMessage(message);
            } else {
                // Other user's message
                addHistoricalOtherMessage(message);
            }
        }
    }

    /**
     * Add date separator to chat
     */
    private void addDateSeparator(LocalDate date) {
        HBox separatorBox = new HBox();
        separatorBox.setAlignment(Pos.CENTER);
        separatorBox.setPadding(new Insets(15, 0, 15, 0));

        String dateText;
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (date.equals(today)) {
            dateText = "Today";
        } else if (date.equals(yesterday)) {
            dateText = "Yesterday";
        } else {
            dateText = date.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        }

        Label dateLabel = new Label(dateText);
        dateLabel.setStyle(
                "-fx-background-color: rgba(102, 126, 234, 0.2); " +
                        "-fx-text-fill: #667eea; " +
                        "-fx-padding: 6 15; " +
                        "-fx-background-radius: 15; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold;"
        );

        separatorBox.getChildren().add(dateLabel);
        messagesContainer.getChildren().add(separatorBox);
    }

    /**
     * Add historical message from me
     */
    private void addHistoricalMyMessage(Message message) {
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 0, 5, 50));

        VBox bubble = createMessageBubble(
                message.getContent(),
                chatClient.getUsername(),
                message.getTimestamp(),
                true,
                true  // isPrivate
        );

        messageBox.getChildren().add(bubble);
        messagesContainer.getChildren().add(messageBox);
    }

    /**
     * Add historical message from other user
     */
    private void addHistoricalOtherMessage(Message message) {
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 50, 5, 0));

        // Add avatar
        StackPane avatarPane = new StackPane();
        avatarPane.setMinSize(35, 35);
        avatarPane.setMaxSize(35, 35);
        avatarPane.getStyleClass().add("avatar");
        avatarPane.setStyle("-fx-background-color: " + generateAvatarColor(message.getSender()) + ";");

        Label avatarLabel = new Label(getInitials(message.getSender()));
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        avatarPane.getChildren().add(avatarLabel);

        VBox bubble = createMessageBubble(
                message.getContent(),
                message.getSender(),
                message.getTimestamp(),
                false,
                true  // isPrivate
        );

        messageBox.getChildren().addAll(avatarPane, bubble);
        messagesContainer.getChildren().add(messageBox);
    }

    /**
     * Delete chat history with selected user
     */
    @FXML
    private void handleDeleteChatHistory() {
        if (selectedUser == null) {
            showAlert("No Chat Selected", "Please select a user to delete chat history.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Chat History");
        confirmDialog.setHeaderText("Delete conversation with " + selectedUser + "?");
        confirmDialog.setContentText("This will permanently delete all messages. This action cannot be undone.");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    boolean deleted = messageDAO.deleteChatHistory(
                            chatClient.getUsername(),
                            selectedUser,
                            chatClient.getUsername()
                    );

                    Platform.runLater(() -> {
                        if (deleted) {
                            messagesContainer.getChildren().clear();
                            addSystemMessage("🗑️ Chat history deleted");
                        } else {
                            showAlert("Error", "Failed to delete chat history");
                        }
                    });
                }).start();
            }
        });
    }

    private void setupMessageInput() {
        // Auto-resize text area
        messageInput.textProperty().addListener((obs, oldText, newText) -> {
            // Send typing indicator
            if (!newText.isEmpty() && !isTyping) {
                isTyping = true;
                chatClient.sendTypingIndicator();

                // Reset typing after 3 seconds
                if (typingTimer != null) {
                    typingTimer.cancel();
                }
                typingTimer = new Timer();
                typingTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        isTyping = false;
                    }
                }, 3000);
            }
        });

        // Limit height
        messageInput.setPrefRowCount(1);
        messageInput.setWrapText(true);
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                // Show all users
                updateUsersList(usersList.getItems());
            } else {
                // Filter users
                List<User> allUsers = new ArrayList<>(usersList.getItems());
                List<User> filtered = allUsers.stream()
                        .filter(user -> user.getUsername().toLowerCase().contains(newVal.toLowerCase()))
                        .collect(Collectors.toList());

                usersList.getItems().setAll(filtered);

                // Update count
                long onlineCount = filtered.stream()
                        .filter(u -> u.getStatus() == User.Status.ONLINE)
                        .count();

                onlineCountLabel.setText("(" + onlineCount + ")");

                if (filtered.isEmpty()) {
                    // Show "no results" message
                    Platform.runLater(() -> {
                        if (usersList.getItems().isEmpty()) {
                            // You could add an empty state label here
                        }
                    });
                }
            }
        });
    }

    @FXML
    private void handleSendMessage() {
        String text = messageInput.getText().trim();

        if (text.isEmpty()) {
            return;
        }


        if (selectedUser != null && !selectedUser.isEmpty()) {
            // Find the user in the list
            User selectedUserObj = usersList.getItems().stream()
                    .filter(u -> u.getUsername().equals(selectedUser))
                    .findFirst()
                    .orElse(null);

            if (selectedUserObj != null && selectedUserObj.getStatus() == User.Status.OFFLINE) {
                // Show offline warning
                addSystemMessage("⚠️ " + selectedUser + " is offline. Message will be delivered when they come online.");
            }

            // Send private message
            chatClient.sendPrivateMessage(selectedUser, text);
            addMyMessage(text, true);
        } else {
            // Send broadcast message
            chatClient.sendMessage(text);
            addMyMessage(text, false);
        }

        // Clear input
        messageInput.clear();
        messageInput.requestFocus();
    }

    @FXML
    private void handleKeyPressed(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            if (event.isShiftDown()) {
                // Allow new line with Shift+Enter
                messageInput.appendText("\n");
            } else {
                // Send message with Enter
                handleSendMessage();
                event.consume();
            }
        }
    }

    @FXML
    private void handleAttachFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Send");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mkv"),
                new FileChooser.ExtensionFilter("Archives", "*.zip", "*.rar", "*.7z")
        );

        Stage stage = (Stage) messageInput.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());

                if (fileData.length > 50 * 1024 * 1024) {
                    showAlert("File too large", "Please select a file smaller than 50MB");
                    return;
                }

                chatClient.sendFile(file.getName(), fileData, selectedUser);
                addFileMessage(file.getName(), fileData.length, true, fileData);

            } catch (IOException e) {
                showAlert("Error", "Failed to read file: " + e.getMessage());
            }
        }
    }

    private void addFileMessage(String fileName, long fileSize, boolean isMyMessage, byte[] fileData) {
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(isMyMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, isMyMessage ? 0 : 50, 5, isMyMessage ? 50 : 0));

        VBox fileBubble = new VBox(8);
        fileBubble.getStyleClass().add("file-bubble");
        fileBubble.setMaxWidth(350);
        fileBubble.setPadding(new Insets(15));

        if (isMyMessage) {
            fileBubble.setStyle(fileBubble.getStyle() + "; -fx-background-color: linear-gradient(to right, #667eea, #764ba2);");
        } else {
            fileBubble.setStyle(fileBubble.getStyle() + "; -fx-background-color: #2A2D3A;");
        }

        // File icon and name
        HBox fileInfo = new HBox(10);
        fileInfo.setAlignment(Pos.CENTER_LEFT);

        Label fileIcon = new Label(getFileIcon(fileName));
        fileIcon.setStyle("-fx-font-size: 28px;");

        VBox fileDetails = new VBox(2);
        Label fileNameLabel = new Label(fileName);
        fileNameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
        fileNameLabel.setWrapText(true);

        Label fileSizeLabel = new Label(formatFileSize(fileSize));
        fileSizeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px;");

        fileDetails.getChildren().addAll(fileNameLabel, fileSizeLabel);
        fileInfo.getChildren().addAll(fileIcon, fileDetails);

        fileBubble.getChildren().add(fileInfo);

        //  ADD DOWNLOAD BUTTON if file data is available
        if (fileData != null && fileData.length > 0) {
            Button downloadBtn = new Button("⬇️ Download");
            downloadBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.2); " +
                            "-fx-text-fill: white; " +
                            "-fx-cursor: hand; " +
                            "-fx-padding: 8 15; " +
                            "-fx-background-radius: 5;"
            );

            downloadBtn.setOnAction(e -> downloadFile(fileName, fileData));

            // Hover effect
            downloadBtn.setOnMouseEntered(e ->
                    downloadBtn.setStyle(downloadBtn.getStyle() + "; -fx-background-color: rgba(255,255,255,0.3);")
            );
            downloadBtn.setOnMouseExited(e ->
                    downloadBtn.setStyle(downloadBtn.getStyle().replace("0.3", "0.2"))
            );

            fileBubble.getChildren().add(downloadBtn);
        }

        messageBox.getChildren().add(fileBubble);
        messagesContainer.getChildren().add(messageBox);
        animateMessage(messageBox);
    }

    /**
     * Get appropriate icon for file type
     */
    private String getFileIcon(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        return switch (ext) {
            case "pdf" -> "📄";
            case "doc", "docx" -> "📝";
            case "xls", "xlsx" -> "📊";
            case "ppt", "pptx" -> "📊";
            case "zip", "rar", "7z" -> "📦";
            case "jpg", "jpeg", "png", "gif" -> "🖼️";
            case "mp4", "avi", "mkv" -> "🎥";
            case "mp3", "wav" -> "🎵";
            case "txt" -> "📃";
            default -> "📄";
        };
    }

    /**
     * Download file to user's computer
     */
    private void downloadFile(String fileName, byte[] fileData) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.setInitialFileName(fileName);

        // Set initial directory to Downloads
        String userHome = System.getProperty("user.home");
        File downloadsDir = new File(userHome, "Downloads");
        if (downloadsDir.exists()) {
            fileChooser.setInitialDirectory(downloadsDir);
        }

        Stage stage = (Stage) messageInput.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                Files.write(file.toPath(), fileData);

                // Show success message
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Download Complete");
                alert.setHeaderText("File saved successfully!");
                alert.setContentText("Saved to: " + file.getAbsolutePath());
                alert.showAndWait();

                System.out.println("✅ File saved: " + file.getAbsolutePath());

            } catch (IOException e) {
                showAlert("Download Error", "Failed to save file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    @FXML
    private void handleEmojiPicker(javafx.scene.input.MouseEvent event) {

        ContextMenu emojiMenu = new ContextMenu();
        emojiMenu.setAutoHide(true);


        emojiMenu.setOnShown(e -> {
            if (emojiMenu.getSkin() != null) {
                Node root = emojiMenu.getSkin().getNode();
                if (root instanceof Region region) {
                    applyGlassmorphism(region);
                }
            }
        });

        // Emoji categories
        Map<String, String[]> emojiCategories = new LinkedHashMap<>();
        emojiCategories.put("😊 Smileys", new String[]{"😊", "😂", "🤣", "😍", "🥰", "😘", "😎", "🤔", "😏", "😢"});
        emojiCategories.put("👍 Gestures", new String[]{"👍", "👎", "👌", "✌️", "🤞", "🤝", "👏", "🙌", "🤲", "👋"});
        emojiCategories.put("❤️ Hearts", new String[]{"❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔"});
        emojiCategories.put("🎉 Objects", new String[]{"🎉", "🎊", "🎈", "🎁", "🏆", "🥇", "⭐", "✨", "💯", "🔥"});
        emojiCategories.put("🌍 Nature", new String[]{"🌍", "🌈", "⚡", "☀️", "🌙", "⭐", "💫", "🌟", "🌺", "🌸"});

        for (Map.Entry<String, String[]> category : emojiCategories.entrySet()) {


            Label categoryLabel = new Label(category.getKey());
            categoryLabel.setStyle(
                    "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 11px;" +
                            "-fx-background-color: rgba(255,255,255,0.08);" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 6 12;"
            );

            CustomMenuItem categoryItem = new CustomMenuItem(categoryLabel);
            categoryItem.setHideOnClick(false);
            emojiMenu.getItems().add(categoryItem);

            // Emoji grid
            FlowPane emojiPane = new FlowPane();
            emojiPane.setHgap(8);
            emojiPane.setVgap(8);
            emojiPane.setPrefWrapLength(300);
            emojiPane.setPadding(new Insets(6, 12, 10, 12));

            for (String emoji : category.getValue()) {

                Button emojiBtn = new Button(emoji);
                emojiBtn.setFocusTraversable(false);
                emojiBtn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-font-size: 20px;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 6;" +
                                "-fx-background-radius: 8;"
                );

                emojiBtn.getStyleClass().add("emoji-picker-button");

                emojiBtn.setOnMouseEntered(e ->
                        emojiBtn.setStyle(
                                "-fx-background-color: rgba(255,255,255,0.18);" +
                                        "-fx-font-size: 20px;" +
                                        "-fx-background-radius: 8;"
                        )
                );

                emojiBtn.setOnMouseExited(e ->
                        emojiBtn.setStyle(
                                "-fx-background-color: transparent;" +
                                        "-fx-font-size: 20px;"
                        )
                );

                emojiBtn.setOnAction(e -> {
                    messageInput.appendText(emoji);
                    emojiMenu.hide();
                });

                emojiPane.getChildren().add(emojiBtn);
            }

            CustomMenuItem emojisItem = new CustomMenuItem(emojiPane);
            emojisItem.setHideOnClick(false);
            emojiMenu.getItems().add(emojisItem);

            if (!category.getKey().equals("🌍 Nature")) {
                emojiMenu.getItems().add(new SeparatorMenuItem());
            }
        }

        emojiMenu.show(messageInput, event.getScreenX(), event.getScreenY());
    }

    private void applyGlassmorphism(Region node) {

        if (!node.getStyleClass().contains("glass-context-menu")) {
            node.getStyleClass().add("glass-context-menu");
        }

        // Effects are safe
        GaussianBlur blur = new GaussianBlur(18);

        DropShadow shadow = new DropShadow();
        shadow.setRadius(30);
        shadow.setSpread(0.12);
        shadow.setColor(Color.rgb(0, 0, 0, 0.5));

        blur.setInput(shadow);
        node.setEffect(blur);
    }



    @FXML
    private void handleSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText("Chat Settings");
        alert.setContentText("Settings functionality coming soon!\n\n" +
                "Features:\n" +
                "• Theme customization\n" +
                "• Notification preferences\n" +
                "• Video and Audio Call\n" +
                "• Audio message\n" +
                "• Privacy settings");
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be disconnected from the chat.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                chatClient.disconnect();

                // Go back to login screen
                try {
                    Stage stage = (Stage) currentUserLabel.getScene().getWindow();
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                            getClass().getResource("/com/chatapp/view/login.fxml")
                    );
                    javafx.scene.Parent root = loader.load();
                    javafx.scene.Scene scene = new javafx.scene.Scene(root, 500, 650);
                    stage.setScene(scene);
                    stage.setTitle("ChatHub - Login");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Message handling
    private void handleIncomingMessage(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case TEXT:
                    addOtherMessage(message);
                    break;
                case PRIVATE:
                    addPrivateMessage(message);
                    break;
                case FILE:
                    System.out.println("📎 Adding file message");
                    addFileMessage(
                            message.getFileName(),
                            message.getFileData() != null ? message.getFileData().length : 0,
                            false,
                            message.getFileData()
                    );
                    break;
                case SYSTEM:
                case USER_JOIN:
                case USER_LEAVE:
                    addSystemMessage(message.getContent());
                    break;
                case TYPING:
                    showTypingIndicator(message.getSender());
                    break;
            }
        });
    }

    private void addMyMessage(String text, boolean isPrivate) {
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 0, 5, 50));

        VBox bubble = createMessageBubble(text, chatClient.getUsername(),
                LocalDateTime.now(), true, isPrivate);

        messageBox.getChildren().add(bubble);

        // Only add to container if user is selected (or it's a private message and user is selected)
        if (selectedUser != null) {
            messagesContainer.getChildren().add(messageBox);

            // Store message for this user
            storeMessageForUser(selectedUser, messageBox);

            // Remove oldest message if limit exceeded (prevent memory leak)
            if (messagesContainer.getChildren().size() > MAX_MESSAGES_DISPLAYED) {
                messagesContainer.getChildren().remove(0);
            }

            animateMessage(messageBox);
            messagesScrollPane.setVvalue(1.0); // Auto-scroll to bottom
        }
    }

    private void addOtherMessage(Message message) {
        // Skip broadcast messages in private chat mode
        if (selectedUser == null) {
            return; // Don't show broadcast messages when no user selected
        }

        HBox messageBox = new HBox(10);
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 50, 5, 0));

        // Add avatar
        StackPane avatarPane = new StackPane();
        avatarPane.setMinSize(35, 35);
        avatarPane.setMaxSize(35, 35);
        avatarPane.getStyleClass().add("avatar");
        avatarPane.setStyle("-fx-background-color: " + generateAvatarColor(message.getSender()) + ";");

        Label avatarLabel = new Label(getInitials(message.getSender()));
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        avatarPane.getChildren().add(avatarLabel);

        VBox bubble = createMessageBubble(message.getContent(), message.getSender(),
                message.getTimestamp(), false, false);

        messageBox.getChildren().addAll(avatarPane, bubble);

        messagesContainer.getChildren().add(messageBox);

        // Store for this user
        storeMessageForUser(message.getSender(), messageBox);

        // Remove oldest message if limit exceeded
        if (messagesContainer.getChildren().size() > MAX_MESSAGES_DISPLAYED) {
            messagesContainer.getChildren().remove(0);
        }

        animateMessage(messageBox);
        messagesScrollPane.setVvalue(1.0);
    }

    private void addPrivateMessage(Message message) {
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 50, 5, 0));

        // Add avatar
        StackPane avatarPane = new StackPane();
        avatarPane.setMinSize(35, 35);
        avatarPane.setMaxSize(35, 35);
        avatarPane.getStyleClass().add("avatar");
        avatarPane.setStyle("-fx-background-color: " + generateAvatarColor(message.getSender()) + ";");

        Label avatarLabel = new Label(getInitials(message.getSender()));
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        avatarPane.getChildren().add(avatarLabel);

        VBox bubble = createMessageBubble("🔒 " + message.getContent(), message.getSender(),
                message.getTimestamp(), false, true);

        messageBox.getChildren().addAll(avatarPane, bubble);

        // Only display if this user is selected
        if (selectedUser != null && selectedUser.equals(message.getSender())) {
            messagesContainer.getChildren().add(messageBox);

            // Store message for this user
            storeMessageForUser(message.getSender(), messageBox);

            // Remove oldest message if limit exceeded
            if (messagesContainer.getChildren().size() > MAX_MESSAGES_DISPLAYED) {
                messagesContainer.getChildren().remove(0);
            }

            animateMessage(messageBox);
            messagesScrollPane.setVvalue(1.0);
        } else {
            // Still store it even if not currently viewing
            storeMessageForUser(message.getSender(), messageBox);
        }
    }

    private VBox createMessageBubble(String text, String sender, LocalDateTime time,
                                     boolean isMyMessage, boolean isPrivate) {
        VBox bubble = new VBox(5);
        bubble.setMaxWidth(400);
        bubble.getStyleClass().add("message-bubble");

        if (isMyMessage) {
            bubble.getStyleClass().add("my-message");
        } else {
            bubble.getStyleClass().add("other-message");
        }

        if (isPrivate) {
            bubble.setStyle(bubble.getStyle() + "; -fx-border-color: #FF6B9D; -fx-border-width: 2;");
        }

        // Sender name (only for other messages)
        if (!isMyMessage) {
            Label senderLabel = new Label(sender);
            senderLabel.getStyleClass().add("message-sender");
            bubble.getChildren().add(senderLabel);
        }

        // Message text
        TextFlow textFlow = new TextFlow();
        Text messageText = new Text(text);
        messageText.getStyleClass().add("message-text");
        textFlow.getChildren().add(messageText);
        bubble.getChildren().add(textFlow);
        // Time
        Label timeLabel = new Label(formatTime(time));
        timeLabel.getStyleClass().add("message-time");
        bubble.getChildren().add(timeLabel);

        bubble.setPadding(new Insets(12, 16, 12, 16));

        return bubble;
    }

    private void addSystemMessage(String text) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setPadding(new Insets(10, 0, 10, 0));

        Label systemLabel = new Label("ℹ️ " + text);
        systemLabel.getStyleClass().add("message-bubble");
        systemLabel.getStyleClass().add("system-message");
        systemLabel.setStyle(systemLabel.getStyle() + "; -fx-padding: 8 15;");
        systemLabel.setTextAlignment(TextAlignment.CENTER);

        messageBox.getChildren().add(systemLabel);

        messagesContainer.getChildren().add(messageBox);

        // Remove oldest message if limit exceeded
        if (messagesContainer.getChildren().size() > MAX_MESSAGES_DISPLAYED) {
            messagesContainer.getChildren().remove(0);
        }

        animateMessage(messageBox);
        messagesScrollPane.setVvalue(1.0);
    }

    private void addFileMessage(String fileName, long fileSize, boolean isMyMessage) {
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(isMyMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, isMyMessage ? 0 : 50, 5, isMyMessage ? 50 : 0));

        VBox fileBubble = new VBox(8);
        fileBubble.getStyleClass().add("file-bubble");
        fileBubble.setMaxWidth(350);
        fileBubble.setPadding(new Insets(15));

        // File icon and name
        HBox fileInfo = new HBox(10);
        fileInfo.setAlignment(Pos.CENTER_LEFT);

        Label fileIcon = new Label("📄");
        fileIcon.setStyle("-fx-font-size: 28px;");

        VBox fileDetails = new VBox(2);
        Label fileNameLabel = new Label(fileName);
        fileNameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");

        Label fileSizeLabel = new Label(formatFileSize(fileSize));
        fileSizeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px;");

        fileDetails.getChildren().addAll(fileNameLabel, fileSizeLabel);
        fileInfo.getChildren().addAll(fileIcon, fileDetails);

        fileBubble.getChildren().add(fileInfo);

        messageBox.getChildren().add(fileBubble);
        messagesContainer.getChildren().add(messageBox);
        animateMessage(messageBox);
    }

    private void updateUsersList(List<User> users) {
        // Filter out current user from the list
        List<User> otherUsers = users.stream()
                .filter(u -> !u.getUsername().equals(chatClient.getUsername()))
                .collect(Collectors.toList());

        usersList.getItems().setAll(otherUsers);

        // Count only ONLINE users
        long onlineCount = otherUsers.stream()
                .filter(u -> u.getStatus() == User.Status.ONLINE)
                .count();

        onlineCountLabel.setText("(" + onlineCount + ")");

        if (onlineUsersCountLabel != null) {
            if (otherUsers.isEmpty()) {
                onlineUsersCountLabel.setText("No other users yet");
            } else {
                onlineUsersCountLabel.setText(onlineCount + " / " + otherUsers.size() + " users online");
            }
        }

        // Animate list update
        for (int i = 0; i < usersList.getItems().size(); i++) {
            final int index = i;
            Platform.runLater(() -> {
                javafx.scene.Node cell = usersList.lookup(".list-cell:nth-child(" + (index + 1) + ")");
                if (cell != null) {
                    FadeTransition fade = new FadeTransition(Duration.millis(200), cell);
                    fade.setFromValue(0);
                    fade.setToValue(1);
                    fade.setDelay(Duration.millis(index * 50));
                    fade.play();
                }
            });
        }
    }

    /**
     * Update the chat header with selected user info
     */
    private void updateChatHeader(String username) {
        // Find the user in the list
        User selectedUserObj = usersList.getItems().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);

        if (selectedUserObj != null) {
            // Update header with user name
            chatHeaderTitle.setText(username);


            if (selectedUserObj.getStatus() == User.Status.ONLINE) {
                chatHeaderStatus.setText("Online");
                chatHeaderStatus.setStyle("-fx-text-fill: #00D9A5;");
                chatHeaderStatusIndicator.setFill(Color.web("#00D9A5"));
            } else {
                chatHeaderStatus.setText("Offline");
                chatHeaderStatus.setStyle("-fx-text-fill: #6B6B8A;");
                chatHeaderStatusIndicator.setFill(Color.web("#6B6B8A"));
            }

            // Update avatar
            Label avatarLabel = new Label(getInitials(username));
            avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");

            Circle avatarCircle = new Circle(25);
            avatarCircle.setFill(Color.web(generateAvatarColor(username)));

            chatHeaderAvatar.getChildren().clear();
            chatHeaderAvatar.getChildren().addAll(avatarCircle, avatarLabel);
        }
    }

    /**
     * Display only messages for the selected user
     */
    private void displayMessagesForUser(String username) {
        messagesContainer.getChildren().clear();

        // Get messages for this user
        List<javafx.scene.Node> userMessageList = userMessages.getOrDefault(username, new java.util.ArrayList<>());

        if (userMessageList.isEmpty()) {
            addSystemMessage("No messages yet with " + username + ". Start the conversation! 👋");
        } else {
            // Add all messages for this user
            for (javafx.scene.Node messageNode : userMessageList) {
                messagesContainer.getChildren().add(messageNode);
            }
        }

        // Auto-scroll to bottom
        messagesScrollPane.setVvalue(1.0);
    }

    /**
     * Store a message node for a specific user
     */
    private void storeMessageForUser(String otherUsername, javafx.scene.Node messageNode) {
        userMessages.computeIfAbsent(otherUsername, k -> new java.util.ArrayList<>())
                .add(messageNode);

        // Enforce max messages per user to prevent memory leak
        List<javafx.scene.Node> messages = userMessages.get(otherUsername);
        while (messages.size() > MAX_MESSAGES_DISPLAYED) {
            messages.remove(0);
        }
    }

    private void showTypingIndicator(String username) {
        typingIndicatorBox.setVisible(true);
        typingIndicatorBox.setManaged(true);

        // Hide after 3 seconds
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            typingIndicatorBox.setVisible(false);
            typingIndicatorBox.setManaged(false);
        }));
        timeline.play();
    }

    private void animateMessage(javafx.scene.Node message) {
        message.setOpacity(0);
        message.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(300), message);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(300), message);
        slide.setFromY(20);
        slide.setToY(0);

        ParallelTransition animation = new ParallelTransition(fade, slide);
        animation.play();
    }

    private void playEntranceAnimation() {
        messagesContainer.setOpacity(0);
        usersList.setOpacity(0);

        FadeTransition fadeMessages = new FadeTransition(Duration.millis(600), messagesContainer);
        fadeMessages.setFromValue(0);
        fadeMessages.setToValue(1);
        fadeMessages.setDelay(Duration.millis(200));

        FadeTransition fadeUsers = new FadeTransition(Duration.millis(600), usersList);
        fadeUsers.setFromValue(0);
        fadeUsers.setToValue(1);
        fadeUsers.setDelay(Duration.millis(400));

        new SequentialTransition(fadeMessages, fadeUsers).play();
    }

    // Utility methods
    private String getInitials(String username) {
        if (username == null || username.isEmpty()) return "?";
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
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

    private String formatTime(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        else return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
