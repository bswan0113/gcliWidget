package com.example;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class CalendarDataManager {

    private static final String NOTES_FILE = "calendar_notes.json";
    private Map<LocalDate, String> notesData;
    private final Gson gson;

    public CalendarDataManager() {
        // LocalDate를 처리하는 JsonSerializer와 JsonDeserializer를 등록하여 GsonBuilder를 구성합니다.
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (localDate, type, jsonSerializationContext) ->
                        localDate == null ? null : new com.google.gson.JsonPrimitive(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (jsonElement, type, jsonDeserializationContext) ->
                        jsonElement == null ? null : LocalDate.parse(jsonElement.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
                .create();

        this.notesData = loadNotes();
    }

    public Map<LocalDate, String> getNotes() {
        return notesData;
    }

    public String getNoteForDate(LocalDate date) {
        return notesData.getOrDefault(date, "");
    }

    public void setNoteForDate(LocalDate date, String note) {
        if (note == null || note.isBlank()) {
            notesData.remove(date);
        } else {
            notesData.put(date, note);
        }
        saveNotes();
    }

    private Map<LocalDate, String> loadNotes() {
        try (FileReader reader = new FileReader(NOTES_FILE)) {
            Type type = new TypeToken<HashMap<LocalDate, String>>() {}.getType();
            Map<LocalDate, String> loadedNotes = gson.fromJson(reader, type);
            return loadedNotes != null ? loadedNotes : new HashMap<>();
        } catch (IOException e) {
            System.err.println("Note file not found or could not be read. Starting with an empty map.");
            return new HashMap<>();
        }
    }

    private void saveNotes() {
        try (FileWriter writer = new FileWriter(NOTES_FILE)) {
            gson.toJson(notesData, writer);
        } catch (IOException e) {
            System.err.println("Failed to save notes to file: " + NOTES_FILE);
            e.printStackTrace();
            // 실제 애플리케이션에서는 이 시점에서 사용자에게 알림을 제공하거나 로그를 기록해야 합니다.
        }
    }
}