package com.chatapp.util;

import com.chatapp.model.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NetworkUtil {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                            context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                            LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    public static void sendMessage(ObjectOutputStream out, Message message) throws IOException {
        String json = gson.toJson(message);
        out.writeObject(json);
        out.flush();
    }

    public static Message receiveMessage(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String json = (String) in.readObject();
        return gson.fromJson(json, Message.class);
    }

    public static void sendObject(ObjectOutputStream out, Object obj) throws IOException {
        String json = gson.toJson(obj);
        out.writeObject(json);
        out.flush();
    }

    public static <T> T receiveObject(ObjectInputStream in, Class<T> classType)
            throws IOException, ClassNotFoundException {
        String json = (String) in.readObject();
        return gson.fromJson(json, classType);
    }
}