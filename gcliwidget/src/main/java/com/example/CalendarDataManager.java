package com.example;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class CalendarDataManager {

    private static final String NOTES_FILE = System.getProperty("user.home") + File.separator + ".gcliwidget_calendar_notes.json";
    private static CalendarDataManager instance;
    private Map<LocalDate, List<Event>> eventsData;
    private final Gson gson;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private CalendarDataManager() {
        // ... (생성자 수정 없음)
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (localDate, type, jsonSerializationContext) ->
                        localDate == null ? null : new com.google.gson.JsonPrimitive(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (jsonElement, type, jsonDeserializationContext) ->
                        jsonElement == null ? null : LocalDate.parse(jsonElement.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
                .create();
        this.eventsData = loadNotes();
    }

    public static synchronized CalendarDataManager getInstance() {
        if (instance == null) {
            instance = new CalendarDataManager();
        }
        return instance;
    }
    
    // ... (getEventsForDate, addEventForDate, toggleEventCompletion 등 기존 메서드 수정 없음)
    public List<Event> getEventsForDate(LocalDate date) { /* ... no changes ... */
        List<Event> events = eventsData.getOrDefault(date, new ArrayList<>());
        if (events.isEmpty()) return events;
        return events.stream().sorted(Comparator.comparing(event -> { String timeStr = event.getTime(); if (timeStr == null || timeStr.isBlank()) return LocalTime.MIN; try { return LocalTime.parse(timeStr, TIME_FORMATTER); } catch (DateTimeParseException e) { return LocalTime.MAX; }})).collect(Collectors.toList());
    }
    public void addEventForDate(LocalDate date, Event event) { eventsData.computeIfAbsent(date, k -> new ArrayList<>()).add(event); saveNotes(); }
    public boolean toggleEventCompletion(LocalDate date, String eventTitle) { /* ... no changes ... */
        if (eventsData.containsKey(date)) { Optional<Event> eventToToggle = eventsData.get(date).stream().filter(event -> event.getTitle().equalsIgnoreCase(eventTitle)).findFirst(); if (eventToToggle.isPresent()) { Event event = eventToToggle.get(); event.setCompleted(!event.isCompleted()); saveNotes(); return true; } } return false;
    }

    // ---▼▼▼ [수정] 일정 복사 메서드 추가 ▼▼▼---
    public int copyEvents(LocalDate sourceDate, LocalDate destinationDate) {
        List<Event> sourceEvents = getEventsForDate(sourceDate);
        if (sourceEvents.isEmpty()) {
            return 0;
        }

        List<Event> destinationEvents = eventsData.computeIfAbsent(destinationDate, k -> new ArrayList<>());
        int count = 0;
        for (Event eventToCopy : sourceEvents) {
            // 새로운 Event 객체를 만들어 ID가 중복되지 않도록 함
            Event newEvent = new Event(eventToCopy.getTitle(), eventToCopy.getTime());
            // 복사된 일정은 항상 '미완료' 상태로 시작
            newEvent.setCompleted(false);
            destinationEvents.add(newEvent);
            count++;
        }
        saveNotes();
        return count;
    }
    // ---▲▲▲ [수정] ▲▲▲---


    public boolean deleteEventByTitle(LocalDate date, String eventTitle) {
        if (eventsData.containsKey(date)) {
            boolean removed = eventsData.get(date).removeIf(event -> event.getTitle().equalsIgnoreCase(eventTitle));
            if (removed) { if (eventsData.get(date).isEmpty()) { eventsData.remove(date); } saveNotes(); return true; }
        }
        return false;
    }
    public void deleteAllEventsForDate(LocalDate date) {
        if (eventsData.containsKey(date)) { eventsData.remove(date); saveNotes(); }
    }

    private Map<LocalDate, List<Event>> loadNotes() { /* ... no changes ... */
        try (FileReader reader = new FileReader(NOTES_FILE)) { Type type = new TypeToken<HashMap<LocalDate, List<Event>>>() {}.getType(); Map<LocalDate, List<Event>> loadedNotes = gson.fromJson(reader, type); if (loadedNotes == null) return new HashMap<>(); loadedNotes.values().forEach(eventList -> eventList.forEach(Event::ensureId)); return loadedNotes; } catch (IOException e) { return new HashMap<>(); }
    }
    private void saveNotes() { /* ... no changes ... */
        try (FileWriter writer = new FileWriter(NOTES_FILE)) { gson.toJson(eventsData, writer); } catch (IOException e) { e.printStackTrace(); }
    }
    public void deleteEvent(LocalDate date, String eventId) { /* ... no changes ... */
        if (eventsData.containsKey(date)) { eventsData.get(date).removeIf(event -> event.getId().equals(eventId)); if (eventsData.get(date).isEmpty()) eventsData.remove(date); saveNotes(); }
    }
    public void updateEvent(LocalDate date, Event updatedEvent) { /* ... no changes ... */
        if (eventsData.containsKey(date)) { List<Event> dayEvents = eventsData.get(date); for (int i = 0; i < dayEvents.size(); i++) { if (dayEvents.get(i).getId().equals(updatedEvent.getId())) { dayEvents.set(i, updatedEvent); saveNotes(); return; } } }
    }
    public void moveEvent(String eventId, LocalDate newDate) { /* ... no changes ... */
        findDateAndEvent(eventId).ifPresent(pair -> { LocalDate oldDate = pair.getKey(); Event eventToMove = pair.getValue(); deleteEvent(oldDate, eventId); addEventForDate(newDate, eventToMove); });
    }
    private Optional<Map.Entry<LocalDate, Event>> findDateAndEvent(String eventId) { /* ... no changes ... */
        return eventsData.entrySet().stream().flatMap(entry -> entry.getValue().stream().filter(event -> eventId.equals(event.getId())).map(event -> Map.entry(entry.getKey(), event))).findFirst();
    }
     public Map<LocalDate, List<Event>> getAllEvents() {
        return this.eventsData;
    }
        public String getScheduleAsTextContext() {
        if (eventsData == null || eventsData.isEmpty()) {
            return "No scheduled events.";
        }

        StringBuilder contextBuilder = new StringBuilder();
        eventsData.keySet().stream().sorted().forEach(date -> {
            List<Event> events = getEventsForDate(date);
            if (!events.isEmpty()) {
                contextBuilder.append(date.format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
                events.forEach(event -> {
                    String status = event.isCompleted() ? "[x]" : "[ ]";
                    String timeInfo = (event.getTime() != null && !event.getTime().isBlank()) ? " (" + event.getTime() + ")" : "";
                    contextBuilder.append(String.format("- %s %s%s\n", status, event.getTitle(), timeInfo));
                });
            }
        });

        return contextBuilder.toString();
    }
}