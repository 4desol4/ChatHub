module com.chatapp.chatapplication {
    // JavaFX Modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // Database and Utilities
    requires com.google.gson;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires jbcrypt;
    requires org.slf4j;

    // Open packages for Reflection (Required for FXML and Gson)
    opens com.chatapp to javafx.fxml, javafx.graphics;
    opens com.chatapp.client.controller to javafx.fxml;
    opens com.chatapp.model to com.google.gson;

    // Export packages to allow access
    exports com.chatapp;
    exports com.chatapp.client;
    exports com.chatapp.client.controller;
    exports com.chatapp.model;
    exports com.chatapp.server;
}