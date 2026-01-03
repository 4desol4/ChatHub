package com.chatapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ChatApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load login screen
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chatapp/view/login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 500, 650);

        primaryStage.setTitle("ChatHub - Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);

        primaryStage.centerOnScreen();

        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(550);



        primaryStage.show();
    }



    @Override
    public void stop() {

        System.out.println("Application closing...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}

