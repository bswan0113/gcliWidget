package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public CompletableFuture<String> generateContent(String prompt, String scheduleContext) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

                LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String currentDate = now.format(formatter);
        
        String systemPrompt = "You are a powerful calendar assistant. Your response MUST be a single JSON object. "
                + "The root of the object must contain one key: 'actions', which is an array of action objects. "
                + "The current date is " + currentDate + ".\n\n"

                + "## CURRENT SCHEDULE (for context) ##\n"
                + scheduleContext + "\n\n"

                + "## **Core Rules** ##\n"
                + "1.  **Event Identification Rule:** When the user asks to 'complete', 'delete', or 'modify' an event, you MUST first look at the `CURRENT SCHEDULE` context. Identify the *exact title* and *date* of the event the user is referring to. Use that exact information to construct the JSON. **DO NOT use the user's descriptive phrase (like '오늘 등록된 식사') as the `title` in the JSON.**\n"
                + "2.  **Title Refinement Rule:** When 'add_events', refine the user's language into a concise title (e.g., '...밥먹기로 함' -> '식사').\n\n"

                + "## ACTIONS FORMAT ##\n"
                + "1. Add: {\"action\": \"add_events\", \"events\": [{\"title\": \"...\", \"date\": \"...\", \"time\": \"...\"}]}\n"
                + "2. Complete: {\"action\": \"complete_events\", \"events\": [{\"title\": \"...\", \"date\": \"...\"}]}\n"
                + "3. Delete: {\"action\": \"delete_events\", \"events\": [{\"title\": \"...\", \"date\": \"...\"}]}\n"
                + "4. Copy: {\"action\": \"copy_events\", \"source_date\": \"...\", \"destination_date\": \"...\"}\n\n"

                + "## EXAMPLES ##\n"
                + "Let's assume the CURRENT SCHEDULE is:\n"
                + "2024-05-22\n"
                + "- [ ] 팀 미팅 (15:00)\n"
                + "- [ ] 운동 (18:00)\n"
                + "2024-05-23\n"
                + "- [ ] 프로젝트 보고서 제출\n\n"
                
                + "User: 오늘 19시에 친구랑 저녁약속\n"
                + "Assistant: {\"actions\": [{\"action\": \"add_events\", \"events\": [{\"title\": \"친구와 저녁 약속\", \"date\": \"" + currentDate + "\", \"time\": \"19:00\"}]}]}\n\n"

                + "User: 오늘 운동 끝났어 체크해줘\n"
                + "Assistant: {\"actions\": [{\"action\": \"complete_events\", \"events\": [{\"title\": \"운동\", \"date\": \"" + currentDate + "\"}]}]}\n\n"

                + "User: 오늘 팀 미팅 취소해줘\n"
                + "Assistant: {\"actions\": [{\"action\": \"delete_events\", \"events\": [{\"title\": \"팀 미팅\", \"date\": \"" + currentDate + "\"}]}]}\n\n"

                + "User: 내일 보고서 제출하는거 삭제해줘\n"
                + "Assistant: {\"actions\": [{\"action\": \"delete_events\", \"events\": [{\"title\": \"프로젝트 보고서 제출\", \"date\": \"" + LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE) + "\"}]}]}\n\n"

                + "User: 어제 일정을 내일로 복사해줘\n"
                + "Assistant: {\"actions\": [{\"action\": \"copy_events\", \"source_date\": \"" + LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE) + "\", \"destination_date\": \"" + LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE) + "\"}]}\n\n"

                + "Process the user's request based on the rules and CURRENT SCHEDULE: " + prompt;

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