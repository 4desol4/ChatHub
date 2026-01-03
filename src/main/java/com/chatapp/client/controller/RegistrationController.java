package com.chatapp.client.controller;

import com.chatapp.client.ChatClient;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.regex.Pattern;

public class RegistrationController {

    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label messageLabel;

    private ChatClient chatClient;
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML
    public void initialize() {
        chatClient = ChatClient.getInstance();

        // Play entrance animation
        playEntranceAnimation();

        // Add real-time validation feedback
        setupValidationListeners();
        
        // Setup responsive layout
        setupResponsiveLayout();
    }

    private void setupResponsiveLayout() {
        // Get the stage and listen for size changes
        usernameField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getWindow().widthProperty().addListener((obs2, oldWidth, newWidth) -> {
                    adjustLayoutForWidth(newWidth.doubleValue());
                });
            }
        });
    }

    private void adjustLayoutForWidth(double width) {
        if (width < 400) {
            // Mobile layout
            messageLabel.setStyle(messageLabel.getStyle() + "; -fx-font-size: 11px;");
            usernameField.setStyle(usernameField.getStyle() + "; -fx-font-size: 12px;");
        } else {
            // Desktop layout
            messageLabel.setStyle(messageLabel.getStyle().replaceAll("-fx-font-size: \\d+px;", "") + "-fx-font-size: 13px;");
            usernameField.setStyle(usernameField.getStyle().replaceAll("-fx-font-size: \\d+px;", "") + "-fx-font-size: 14px;");
        }
    }

    @FXML
    private void handleRegister() {
        // Get values
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate all fields
        if (!validateInputs(username, email, password, confirmPassword)) {
            return;
        }

        // Disable inputs during registration
        setInputsDisabled(true);
        showMessage("Creating your account...", MessageType.INFO);

        // Attempt connection and registration
        new Thread(() -> {
            try {
                // Connect to server
                boolean connected = chatClient.connect(SERVER_HOST, SERVER_PORT);

                if (!connected) {
                    Platform.runLater(() -> {
                        showMessage("❌ Cannot connect to server. Please ensure the server is running.",
                                MessageType.ERROR);
                        setInputsDisabled(false);
                    });
                    return;
                }

                // Attempt registration
                boolean registerSuccess = chatClient.register(username, password, email);

                Platform.runLater(() -> {
                    if (registerSuccess) {
                        showMessage("✅ Account created successfully! Loading chat...",
                                MessageType.SUCCESS);

                        // Delay before loading chat
                        PauseTransition pause = new PauseTransition(Duration.seconds(1));
                        pause.setOnFinished(e -> loadChatScene());
                        pause.play();
                    } else {
                        showMessage("❌ Username already exists. Please choose another username.",
                                MessageType.ERROR);
                        setInputsDisabled(false);
                        shakeNode(usernameField);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMessage("❌ Registration failed: " + e.getMessage(),
                            MessageType.ERROR);
                    setInputsDisabled(false);
                    e.printStackTrace();
                });
            }
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();

            // Fade out animation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300),
                    stage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com/chatapp/view/login.fxml")
                    );
                    Parent root = loader.load();
                    Scene scene = new Scene(root, 500, 650);

                    root.setOpacity(0);
                    stage.setScene(scene);

                    // Fade in
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            fadeOut.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleRegister();
        }
    }

    private boolean validateInputs(String username, String email,
                                   String password, String confirmPassword) {
        // Username validation
        if (username.isEmpty()) {
            showMessage("❌ Please enter a username", MessageType.ERROR);
            shakeNode(usernameField);
            usernameField.requestFocus();
            return false;
        }

        if (username.length() < 3) {
            showMessage("❌ Username must be at least 3 characters long", MessageType.ERROR);
            shakeNode(usernameField);
            usernameField.requestFocus();
            return false;
        }

        if (username.length() > 20) {
            showMessage("❌ Username must be less than 20 characters", MessageType.ERROR);
            shakeNode(usernameField);
            usernameField.requestFocus();
            return false;
        }

        if (!username.matches("[a-zA-Z0-9_]+")) {
            showMessage("❌ Username can only contain letters, numbers, and underscores",
                    MessageType.ERROR);
            shakeNode(usernameField);
            usernameField.requestFocus();
            return false;
        }

        // Email validation
        if (email.isEmpty()) {
            showMessage("❌ Please enter an email address", MessageType.ERROR);
            shakeNode(emailField);
            emailField.requestFocus();
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showMessage("❌ Please enter a valid email address", MessageType.ERROR);
            shakeNode(emailField);
            emailField.requestFocus();
            return false;
        }

        // Password validation
        if (password.isEmpty()) {
            showMessage("❌ Please enter a password", MessageType.ERROR);
            shakeNode(passwordField);
            passwordField.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            showMessage("❌ Password must be at least 6 characters long", MessageType.ERROR);
            shakeNode(passwordField);
            passwordField.requestFocus();
            return false;
        }

        if (password.length() > 50) {
            showMessage("❌ Password must be less than 50 characters", MessageType.ERROR);
            shakeNode(passwordField);
            passwordField.requestFocus();
            return false;
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            showMessage("❌ Please confirm your password", MessageType.ERROR);
            shakeNode(confirmPasswordField);
            confirmPasswordField.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("❌ Passwords do not match", MessageType.ERROR);
            shakeNode(confirmPasswordField);
            confirmPasswordField.requestFocus();
            return false;
        }

        return true;
    }

    private void setupValidationListeners() {
        // Real-time username validation
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("[a-zA-Z0-9_]*")) {
                usernameField.setText(oldVal);
            }
        });

        // Real-time password match indicator
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                if (newVal.equals(passwordField.getText())) {
                    confirmPasswordField.setStyle(confirmPasswordField.getStyle() +
                            "; -fx-border-color: #00D9A5; -fx-border-width: 2;");
                } else {
                    confirmPasswordField.setStyle(confirmPasswordField.getStyle() +
                            "; -fx-border-color: #FF4757; -fx-border-width: 2;");
                }
            }
        });
    }

    private void loadChatScene() {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/chatapp/view/chat.fxml")
            );
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 750);

            // Fade in animation
            root.setOpacity(0);
            root.setScaleX(0.8);
            root.setScaleY(0.8);

            stage.setScene(scene);
            stage.setTitle("ChatHub - " + chatClient.getUsername());

            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(500), root);
            scaleUp.setFromX(0.8);
            scaleUp.setFromY(0.8);
            scaleUp.setToX(1.0);
            scaleUp.setToY(1.0);

            ParallelTransition transition = new ParallelTransition(fadeIn, scaleUp);
            transition.play();

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("❌ Failed to load chat interface", MessageType.ERROR);
        }
    }

    private void playEntranceAnimation() {
        VBox parent = (VBox) usernameField.getParent().getParent();
        parent.setOpacity(0);
        parent.setTranslateY(30);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), parent);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        TranslateTransition slideUp = new TranslateTransition(Duration.millis(800), parent);
        slideUp.setFromY(30);
        slideUp.setToY(0);

        ParallelTransition entrance = new ParallelTransition(fadeIn, slideUp);
        entrance.setDelay(Duration.millis(100));
        entrance.play();
    }

    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    private enum MessageType {
        ERROR, SUCCESS, INFO
    }

    private void showMessage(String text, MessageType type) {
        messageLabel.setText(text);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        switch (type) {
            case ERROR:
                messageLabel.setStyle("-fx-text-fill: #FF4757; -fx-background-color: rgba(255,71,87,0.1); " +
                        "-fx-padding: 10; -fx-background-radius: 8;");
                break;
            case SUCCESS:
                messageLabel.setStyle("-fx-text-fill: #00D9A5; -fx-background-color: rgba(0,217,165,0.1); " +
                        "-fx-padding: 10; -fx-background-radius: 8;");
                break;
            case INFO:
                messageLabel.setStyle("-fx-text-fill: #667eea; -fx-background-color: rgba(102,126,234,0.1); " +
                        "-fx-padding: 10; -fx-background-radius: 8;");
                break;
        }

        // Fade in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), messageLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void setInputsDisabled(boolean disabled) {
        usernameField.setDisable(disabled);
        emailField.setDisable(disabled);
        passwordField.setDisable(disabled);
        confirmPasswordField.setDisable(disabled);
    }
}