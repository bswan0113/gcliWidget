package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
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
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        String systemPrompt = "You are a powerful calendar assistant. Your response MUST be a single JSON object. "
                + "The root of the object must contain one key: 'actions', which is an array of action objects. "
                + "The current date is " + currentDate + ".\n"

                + "## **Title Refinement Rule** ##\n"
                + "When creating a title for an 'add_events' action, you MUST refine the user's informal language into a concise and formal title. "
                + "Extract the core activity. Remove unnecessary verbs like '...will go', '...plan to', '...going to'. "
                + "Use respectful language (e.g., 'Mom' -> 'Mother').\n"

                + "ACTIONS FORMAT:\n"
                + "1. Add Events: {\"action\": \"add_events\", \"events\": [{\"title\": \"...\", \"date\": \"...\", \"time\": \"...\"}, ...]}\n"
                + "2. Complete Events: {\"action\": \"complete_events\", \"events\": [{\"title\": \"...\", \"date\": \"...\"}, {\"title\": \"all\", \"date\": \"...\"}]}\n"
                + "3. Delete Events: {\"action\": \"delete_events\", \"events\": [{\"title\": \"...\", \"date\": \"...\"}, {\"title\": \"all\", \"date\": \"...\"}]}\n"
                + "4. Copy Events: {\"action\": \"copy_events\", \"source_date\": \"...\", \"destination_date\": \"...\"}\n"
                + "\n"

                + "EXAMPLES (Pay close attention to title refinement):\n"
                + "User: 내일 오후 3시에 팀 미팅 추가해줘\n"
                + "Assistant: {\"actions\": [{\"action\": \"add_events\", \"events\": [{\"title\": \"팀 미팅\", \"date\": \"" + LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE) + "\", \"time\": \"15:00\"}]}]}\n"
                + "User: 오늘 18시에 운동 갈 예정\n"
                + "Assistant: {\"actions\": [{\"action\": \"add_events\", \"events\": [{\"title\": \"운동\", \"date\": \"" + currentDate + "\", \"time\": \"18:00\"}]}]}\n"
                + "User: 19시에 엄마랑 밥먹기로 함\n"
                + "Assistant: {\"actions\": [{\"action\": \"add_events\", \"events\": [{\"title\": \"어머니와 식사\", \"date\": \"" + currentDate + "\", \"time\": \"19:00\"}]}]}\n"
                + "User: 17시에 운동하고 19시에 친구랑 저녁약속\n"
                + "Assistant: {\"actions\": [{\"action\": \"add_events\", \"events\": [{\"title\": \"운동\", \"date\": \"" + currentDate + "\", \"time\": \"17:00\"}, {\"title\": \"친구와 저녁 약속\", \"date\": \"" + currentDate + "\", \"time\": \"19:00\"}]}]}\n"
                + "User: 오늘 일정을 다음주 오늘로 복사해줘\n"
                + "Assistant: {\"actions\": [{\"action\": \"copy_events\", \"source_date\": \"" + currentDate + "\", \"destination_date\": \"" + LocalDate.now().plusWeeks(1).format(DateTimeFormatter.ISO_LOCAL_DATE) + "\"}]}\n"
                + "\n"

                + "Process the user's request: " + prompt;

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
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("response_mime_type", "application/json");
        requestBody.add("generationConfig", generationConfig);

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
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").GET().build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body);
    }

    private String parseResponse(String responseBody) {
        // System.out.println("Gemini API Response: " + responseBody); // Debugging
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
                return "{\"error\": \"API Error: " + responseJson.getAsJsonObject("error").get("message").getAsString() + "\"}";
            } else {
                return "{\"error\": \"Unexpected response format from Gemini API.\"}";
            }
        } catch (Exception e) {
            return "{\"error\": \"Failed to parse response from Gemini API.\", \"original_response\": \"" + responseBody.replace("\"", "\\\"") + "\"}";
        }
    }
}