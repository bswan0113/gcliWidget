package com.example;

import java.awt.AWTException;
import java.awt.Canvas;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

public class NotificationService {
    private static final NotificationService instance = new NotificationService();
    private ScheduledExecutorService scheduler;
    private final CalendarDataManager dataManager;
    private final Set<String> notifiedEventIds;

    private static final long NOTIFICATION_LEAD_TIME_MINUTES = 60; // 실제 운영시 60분
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private NotificationService() {
        this.dataManager = CalendarDataManager.getInstance();
        this.notifiedEventIds = new HashSet<>();
    }

    public static NotificationService getInstance() {
        return instance;
    }

    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkAndNotify, 0, 1, TimeUnit.MINUTES);
        System.out.println("NotificationService started.");
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            System.out.println("NotificationService stopped.");
        }
    }

    // In NotificationService.java

private void checkAndNotify() {
    Platform.runLater(() -> {
        try {
            LocalDateTime now = LocalDateTime.now();
            Map<LocalDate, List<Event>> allEvents = dataManager.getAllEvents();

            for (Map.Entry<LocalDate, List<Event>> entry : allEvents.entrySet()) {
                LocalDate date = entry.getKey();
                if (date.isBefore(now.toLocalDate())) continue;

                for (Event event : entry.getValue()) {
                    if (notifiedEventIds.contains(event.getId()) || event.isCompleted()) {
                        continue;
                    }
                    
                    getEventDateTime(date, event).ifPresent(eventDateTime -> {
                        long minutesUntilEvent = ChronoUnit.MINUTES.between(now, eventDateTime);

                        if (minutesUntilEvent <= NOTIFICATION_LEAD_TIME_MINUTES && minutesUntilEvent >= -1) { 
                            showNotification(event, minutesUntilEvent);
                            notifiedEventIds.add(event.getId());
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error during notification check: " + e.getMessage());
            e.printStackTrace();
        }
    });
}
    
    private Optional<LocalDateTime> getEventDateTime(LocalDate date, Event event) {
        if (event.getTime() == null || event.getTime().isBlank()) {
            return Optional.empty();
        }
        try {
            LocalTime time = LocalTime.parse(event.getTime(), TIME_FORMATTER);
            return Optional.of(LocalDateTime.of(date, time));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    // ---▼▼▼ [핵심 수정] private -> public 으로 변경 ▼▼▼---
    // In NotificationService.java

private void showNotification(Event event, long minutesUntil) {
    if (SystemTray.isSupported()) {
        try {
            SystemTray tray = SystemTray.getSystemTray();
            URL imageUrl = NotificationService.class.getResource("/bell.png");
            if (imageUrl == null) {
                 System.err.println("Notification icon 'bell.png' not found in resources directory.");
                 // 아이콘 없이도 알림은 보내도록 대체 텍스트 아이콘 생성 (혹시 모를 에러 대비)
                 TrayIcon errorIcon = new TrayIcon(new Canvas().createImage(1, 1), "gcliwidget");
                 errorIcon.displayMessage("리소스 오류", "아이콘 파일 'bell.png'를 찾을 수 없습니다.", TrayIcon.MessageType.ERROR);
                 tray.remove(errorIcon);
                 return;
            }
            Image image = Toolkit.getDefaultToolkit().createImage(imageUrl);

            // TrayIcon 생성자의 두 번째 인수가 툴팁(마우스 올렸을 때) 텍스트가 됨
            TrayIcon trayIcon = new TrayIcon(image, "gcliwidget 일정 알림");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            
            // ---▼▼▼ [핵심 수정] 알림 메시지 로직 변경 ▼▼▼---
            String title = "📌 잠시 후 예정된 일정";
            String message;

            // HH:mm 형식의 시간 정보가 있을 경우
            if (event.getTime() != null && !event.getTime().isBlank()) {
                if (minutesUntil <= 0) { // 정확히 정각이거나 약간 지났을 때
                    message = String.format("지금 [%s] 일정을 시작할 시간입니다.", event.getTitle());
                } else {
                    message = String.format("%s에 시작: %s (%d분 전)", event.getTime(), event.getTitle(), minutesUntil);
                }
            } else { // 시간 정보가 없는 경우 (예: "보고서 제출")
                 message = String.format("오늘 예정된 '%s' 일정이 있습니다.", event.getTitle());
            }
            // ---▲▲▲ [핵심 수정] ▲▲▲---
            
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
            
            // 10초 뒤에 시스템 트레이에서 아이콘을 제거하여 깔끔하게 유지
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    // UI 스레드에서 제거해야 안전함
                    Platform.runLater(() -> tray.remove(trayIcon));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (AWTException e) {
            System.err.println("Could not initialize system tray.");
            e.printStackTrace();
        }
    } else {
        System.err.println("System tray not supported!");
    }
}