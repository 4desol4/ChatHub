module com.chatapp.chatapplication {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires jbcrypt;
    requires org.slf4j;

    opens com.chatapp to javafx.fxml;
    opens com.chatapp.client.controller to javafx.fxml;
    opens com.chatapp.model to com.google.gson;

    exports com.chatapp;
    exports com.chatapp.client;
    exports com.chatapp.client.controller;
    exports com.chatapp.model;
    exports com.chatapp.server;
}