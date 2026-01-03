package com.chatapp.client.controller;

import com.chatapp.client.ChatClient;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class LoginController {

    @FXML private VBox loginForm;
    @FXML private VBox registrationForm;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField regUsernameField;
    @FXML private TextField regEmailField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField regConfirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label regErrorLabel;
    @FXML private Label toggleLabel;
    @FXML private Button toggleButton;

    private ChatClient chatClient;
    private boolean isLoginMode = true;
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    @FXML
    public void initialize() {
        chatClient = ChatClient.getInstance();

        // Add enter key support for login
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Play entrance animation
        playEntranceAnimation();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields", errorLabel);
            shakeNode(loginForm);
            return;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters", errorLabel);
            shakeNode(usernameField);
            return;
        }

        // Disable inputs during login
        setInputsDisabled(true);

        // Attempt connection and login
        new Thread(() -> {
            try {
                boolean connected = chatClient.connect(SERVER_HOST, SERVER_PORT);

                if (!connected) {
                    Platform.runLater(() -> {
                        showError("Cannot connect to server. Make sure the server is running.", errorLabel);
                        setInputsDisabled(false);
                    });
                    return;
                }

                boolean loginSuccess = chatClient.login(username, password);

                Platform.runLater(() -> {
                    if (loginSuccess) {
                        showSuccess("Login successful! Loading chat...", errorLabel);

                        // Animate transition to chat
                        fadeOutAndLoadChat();
                    } else {
                        showError("Invalid username or password", errorLabel);
                        setInputsDisabled(false);
                        shakeNode(loginForm);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Connection error: " + e.getMessage(), errorLabel);
                    setInputsDisabled(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleRegister() {
        String username = regUsernameField.getText().trim();
        String email = regEmailField.getText().trim();
        String password = regPasswordField.getText();
        String confirmPassword = regConfirmPasswordField.getText();

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields", regErrorLabel);
            shakeNode(registrationForm);
            return;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters", regErrorLabel);
            shakeNode(regUsernameField);
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showError("Please enter a valid email address", regErrorLabel);
            shakeNode(regEmailField);
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters", regErrorLabel);
            shakeNode(regPasswordField);
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match", regErrorLabel);
            shakeNode(regConfirmPasswordField);
            return;
        }

        // Disable inputs during registration
        setInputsDisabled(true);

        // Attempt connection and registration
        new Thread(() -> {
            try {
                boolean connected = chatClient.connect(SERVER_HOST, SERVER_PORT);

                if (!connected) {
                    Platform.runLater(() -> {
                        showError("Cannot connect to server. Make sure the server is running.", regErrorLabel);
                        setInputsDisabled(false);
                    });
                    return;
                }

                boolean registerSuccess = chatClient.register(username, password, email);

                Platform.runLater(() -> {
                    if (registerSuccess) {
                        showSuccess("Registration successful! Loading chat...", regErrorLabel);

                        // Animate transition to chat
                        fadeOutAndLoadChat();
                    } else {
                        showError("Username already exists. Please choose another.", regErrorLabel);
                        setInputsDisabled(false);
                        shakeNode(registrationForm);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Connection error: " + e.getMessage(), regErrorLabel);
                    setInputsDisabled(false);
                });
            }
        }).start();
    }

    @FXML
    private void toggleForm() {
        if (isLoginMode) {
            // Switch to registration
            animateFormTransition(loginForm, registrationForm);
            toggleLabel.setText("Already have an account?");
            toggleButton.setText("Sign In");
            isLoginMode = false;
            clearErrors();
        } else {
            // Switch to login
            animateFormTransition(registrationForm, loginForm);
            toggleLabel.setText("Don't have an account?");
            toggleButton.setText("Sign Up");
            isLoginMode = true;
            clearErrors();
        }
    }

    private void animateFormTransition(VBox fromForm, VBox toForm) {
        // Fade out current form
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), fromForm);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        // Slide out
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), fromForm);
        slideOut.setFromX(0);
        slideOut.setToX(-50);

        ParallelTransition hideTransition = new ParallelTransition(fadeOut, slideOut);

        hideTransition.setOnFinished(e -> {
            fromForm.setVisible(false);
            fromForm.setManaged(false);
            toForm.setVisible(true);
            toForm.setManaged(true);

            // Fade in new form
            toForm.setOpacity(0);
            toForm.setTranslateX(50);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toForm);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), toForm);
            slideIn.setFromX(50);
            slideIn.setToX(0);

            ParallelTransition showTransition = new ParallelTransition(fadeIn, slideIn);
            showTransition.play();
        });

        hideTransition.play();
    }

    private void fadeOutAndLoadChat() {
        Stage stage = (Stage) usernameField.getScene().getWindow();

        // Fade out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), stage.getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        // Scale down animation
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(500), stage.getScene().getRoot());
        scaleDown.setFromX(1.0);
        scaleDown.setFromY(1.0);
        scaleDown.setToX(0.8);
        scaleDown.setToY(0.8);

        ParallelTransition transition = new ParallelTransition(fadeOut, scaleDown);

        transition.setOnFinished(e -> {
            try {
                loadChatScene(stage);
            } catch (IOException ex) {
                ex.printStackTrace();
                showError("Failed to load chat interface", errorLabel);
            }
        });

        transition.play();
    }

    private void loadChatScene(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chatapp/view/chat.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1400, 800);

        // Fade in animation
        root.setOpacity(0);
        root.setScaleX(0.8);
        root.setScaleY(0.8);

        stage.setScene(scene);
        stage.setTitle("ChatHub - " + chatClient.getUsername());
        
        // Make fullscreen and center
        stage.setWidth(1400);
        stage.setHeight(800);
        stage.centerOnScreen();
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setMaximized(true);

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
    }

    private void playEntranceAnimation() {
        VBox loginBox = (VBox) loginForm.getParent();
        loginBox.setOpacity(0);
        loginBox.setTranslateY(30);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), loginBox);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        TranslateTransition slideUp = new TranslateTransition(Duration.millis(800), loginBox);
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

    private void showError(String message, Label label) {
        label.setText("❌ " + message);
        label.setStyle("-fx-text-fill: #FF4757;");
        label.setVisible(true);
        label.setManaged(true);

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), label);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void showSuccess(String message, Label label) {
        label.setText("✅ " + message);
        label.setStyle("-fx-text-fill: #00D9A5;");
        label.setVisible(true);
        label.setManaged(true);

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), label);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void clearErrors() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        regErrorLabel.setVisible(false);
        regErrorLabel.setManaged(false);
    }

    private void setInputsDisabled(boolean disabled) {
        if (isLoginMode) {
            usernameField.setDisable(disabled);
            passwordField.setDisable(disabled);
        } else {
            regUsernameField.setDisable(disabled);
            regEmailField.setDisable(disabled);
            regPasswordField.setDisable(disabled);
            regConfirmPasswordField.setDisable(disabled);
        }
        toggleButton.setDisable(disabled);
    }
}