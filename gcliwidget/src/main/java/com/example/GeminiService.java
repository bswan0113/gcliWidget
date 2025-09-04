package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;  
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class GeminiService {

    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;

    public GeminiService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public CompletableFuture<String> generateContent(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

                String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String systemPrompt = "You are a helpful assistant. When the user wants to add a schedule, respond in the following JSON format: {\"action\": \"add_event\", \"title\": \"...\", \"date\": \"YYYY-MM-DD\", \"time\": \"HH:MM\"}. The current date is " + currentDate + " and the current time is " + currentTime + ". User's request: " + prompt;

        JsonObject part = new JsonObject();
        part.addProperty("text", systemPrompt);

        JsonArray parts = new JsonArray();
        parts.add(part);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contents);

        String jsonBody = gson.toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::parseResponse);
    }

    public CompletableFuture<String> listModels() {
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }

    private String parseResponse(String responseBody) {
        System.out.println("Gemini API Response: " + responseBody);
        try {
            JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
            if (responseJson.has("candidates")) {
                return responseJson.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();
            } else if (responseJson.has("error")) {
                String errorMessage = responseJson.getAsJsonObject("error").get("message").getAsString();
                return "Error from Gemini API: " + errorMessage;
            } else {
                return "Error: Unexpected response from Gemini API.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing response from Gemini API.";
        }
    }
}
