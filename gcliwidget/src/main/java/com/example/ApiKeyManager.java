package com.example;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ApiKeyManager {

    private static final String API_KEY_FILE = System.getProperty("user.home") + File.separator + ".gcliwidget_api_key";

    public static void saveApiKey(String apiKey) {
        try {
            Files.write(Paths.get(API_KEY_FILE), apiKey.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String loadApiKey() {
        try {
            if (!Files.exists(Paths.get(API_KEY_FILE))) {
                return null;
            }
            return new String(Files.readAllBytes(Paths.get(API_KEY_FILE)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}