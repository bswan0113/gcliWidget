package com.example;

import java.util.UUID;

public class Event {
    private String id;
    private String title;
    private String time; // HH:MM format
    private boolean completed; // <<< [추가] 완료 여부 필드

    public Event(String title, String time) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.time = time;
        this.completed = false; // 기본값은 '미완료'
    }
    
    public void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
    
    // <<< [추가] completed 필드의 getter와 setter
    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        if (time != null && !time.isBlank()) {
            return time + " - " + title;
        }
        return title;
    }
}