package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarDataManager {

    private static final String NOTES_FILE = "calendar_notes.json";
    private static CalendarDataManager instance;
    private Map<LocalDate, List<Event>> notesData;
    private final Gson gson;

    private CalendarDataManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (localDate, type, jsonSerializationContext) ->
                        localDate == null ? null : new com.google.gson.JsonPrimitive(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (jsonElement, type, jsonDeserializationContext) ->
                        jsonElement == null ? null : LocalDate.parse(jsonElement.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
                .create();

        this.notesData = loadNotes();
    }

    public static synchronized CalendarDataManager getInstance() {
        if (instance == null) {
            instance = new CalendarDataManager();
        }
        return instance;
    }

    public List<Event> getEventsForDate(LocalDate date) {
        return notesData.getOrDefault(date, new ArrayList<>());
    }

    public void addEventForDate(LocalDate date, Event event) {
        List<Event> events = notesData.getOrDefault(date, new ArrayList<>());
        events.add(event);
        notesData.put(date, events);
        saveNotes();
    }

    private Map<LocalDate, List<Event>> loadNotes() {
        try (FileReader reader = new FileReader(NOTES_FILE)) {
            Type type = new TypeToken<HashMap<LocalDate, List<Event>>>() {}.getType();
            Map<LocalDate, List<Event>> loadedNotes = gson.fromJson(reader, type);
            return loadedNotes != null ? loadedNotes : new HashMap<>();
        } catch (IOException e) {
            System.err.println("Note file not found or could not be read. Starting with an empty map.");
            return new HashMap<>();
        }
    }

    private void saveNotes() {
        try (FileWriter writer = new FileWriter(NOTES_FILE)) {
            System.out.println("Saving notes...");
            System.out.println("Notes data: " + gson.toJson(notesData));
            gson.toJson(notesData, writer);
            System.out.println("Notes saved successfully.");
        } catch (IOException e) {
            System.err.println("Failed to save notes to file: " + NOTES_FILE);
            e.printStackTrace();
        }
    }
}