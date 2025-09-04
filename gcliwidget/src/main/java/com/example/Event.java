package com.example;

public class Event {
    private String title;
    private String time; // HH:MM format

    public Event(String title, String time) {
        this.title = title;
        this.time = time;
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

    @Override
    public String toString() {
        if (time != null && !time.isEmpty()) {
            return title + " at " + time;
        }
        return title;
    }
}
