package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class App extends Application {

    private TextArea terminalOutput;
    private TextField commandInput;
    private GeminiService geminiService;
    private final Gson gson = new Gson();
    private CalendarView calendarView;
    private BorderPane contentPane;
    private VBox terminalPane;

    private enum TerminalPosition {LEFT, RIGHT, BOTTOM}

    private final Delta dragDelta = new Delta();
    private double initialX, initialY, initialWidth, initialHeight;
    private int resizeMode = 0;
    private static final int RESIZE_BORDER_WIDTH = 10;
    private static final double MIN_WIDTH = 600;
    private static final double MIN_HEIGHT = 500;

    static class Delta {
        double x, y;
    }

    @Override
    public void start(final Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);

        contentPane = new BorderPane();
        contentPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-background-radius: 10;");

        calendarView = new CalendarView(LocalDate.now());
        contentPane.setCenter(calendarView);

        terminalPane = new VBox(5);
        terminalPane.setPadding(new Insets(10));
        terminalPane.setStyle("-fx-background-color: rgba(20, 20, 20, 0.7); -fx-background-radius: 8;");

        terminalOutput = new TextArea();
        terminalOutput.setEditable(false);
        terminalOutput.setWrapText(true);
        terminalOutput.setStyle("-fx-font-family: 'monospaced'; -fx-control-inner-background: rgba(0,0,0,0.5); -fx-text-fill: white;");
        terminalOutput.setText("Welcome to gcliwidget!\n");

        commandInput = new TextField();
        commandInput.setStyle("-fx-font-family: 'monospaced'; -fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white;");
        commandInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String command = commandInput.getText().trim();
                if (!command.isBlank()) {
                    terminalOutput.appendText("> " + command + "\n");
                    executeCommand(command);
                    commandInput.clear();
                }
            }
        });

        setupGeminiService();

        Slider transparencySlider = new Slider(0.1, 1.0, 0.7);
        transparencySlider.setBlockIncrement(0.1);
        transparencySlider.valueProperty().addListener((obs, oldVal, newVal) -> stage.setOpacity(newVal.doubleValue()));

        HBox layoutControls = createLayoutButtons();

        terminalPane.getChildren().addAll(layoutControls, terminalOutput, commandInput, transparencySlider);

        setTerminalPosition(TerminalPosition.BOTTOM);

        Button closeButton = new Button("X");
        closeButton.setStyle("-fx-background-color: rgba(255, 0, 0, 0.7); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 5 10 5 10; -fx-background-radius: 0 10 0 10;");
        closeButton.setOnAction(e -> Platform.exit());

        StackPane root = new StackPane(contentPane, closeButton);
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);

        Scene scene = new Scene(root, 1200, 800, Color.TRANSPARENT);
        setupWindowHandlers(scene, stage);

        stage.setScene(scene);
        stage.setTitle("gcliwidget - Calendar & Terminal");
        stage.show();
        NotificationService.getInstance().start();
    }
    @Override
    public void stop() throws Exception {
        NotificationService.getInstance().stop();
        super.stop();
    }
    
    // ... setup/helper 메서드 (createLayoutButtons, setTerminalPosition, setupWindowHandlers, ...) 수정 없음 ...
    private HBox createLayoutButtons() { /* ... no changes ... */
        Button leftButton = new Button("좌"); Button bottomButton = new Button("하"); Button rightButton = new Button("우");
        String buttonStyle = "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 6 2 6;";
        leftButton.setStyle(buttonStyle); bottomButton.setStyle(buttonStyle); rightButton.setStyle(buttonStyle);
        leftButton.setOnAction(e -> setTerminalPosition(TerminalPosition.LEFT)); bottomButton.setOnAction(e -> setTerminalPosition(TerminalPosition.BOTTOM)); rightButton.setOnAction(e -> setTerminalPosition(TerminalPosition.RIGHT));
        HBox layoutControls = new HBox(5, leftButton, bottomButton, rightButton); layoutControls.setAlignment(Pos.CENTER_RIGHT); return layoutControls;
    }
    private void setTerminalPosition(TerminalPosition position) { /* ... no changes ... */
        contentPane.setBottom(null); contentPane.setLeft(null); contentPane.setRight(null);
        switch (position) { case LEFT: terminalPane.setPrefWidth(350); terminalPane.setPrefHeight(-1); terminalOutput.setPrefHeight(Double.MAX_VALUE); contentPane.setLeft(terminalPane); break; case RIGHT: terminalPane.setPrefWidth(350); terminalPane.setPrefHeight(-1); terminalOutput.setPrefHeight(Double.MAX_VALUE); contentPane.setRight(terminalPane); break; case BOTTOM: default: terminalPane.setPrefWidth(-1); terminalPane.setPrefHeight(300); terminalOutput.setPrefHeight(200); contentPane.setBottom(terminalPane); break; }
    }
    private void setupWindowHandlers(Scene scene, Stage stage) { /* ... no changes ... */ 
        scene.setOnMouseMoved(mouseEvent -> { double x = mouseEvent.getX(); double y = mouseEvent.getY(); double width = stage.getWidth(); double height = stage.getHeight(); boolean onTop = y < RESIZE_BORDER_WIDTH; boolean onBottom = y > height - RESIZE_BORDER_WIDTH; boolean onLeft = x < RESIZE_BORDER_WIDTH; boolean onRight = x > width - RESIZE_BORDER_WIDTH; if (onTop && onLeft) { scene.setCursor(Cursor.NW_RESIZE); resizeMode = 5; } else if (onTop && onRight) { scene.setCursor(Cursor.NE_RESIZE); resizeMode = 6; } else if (onBottom && onLeft) { scene.setCursor(Cursor.SW_RESIZE); resizeMode = 7; } else if (onBottom && onRight) { scene.setCursor(Cursor.SE_RESIZE); resizeMode = 8; } else if (onTop) { scene.setCursor(Cursor.N_RESIZE); resizeMode = 1; } else if (onBottom) { scene.setCursor(Cursor.S_RESIZE); resizeMode = 2; } else if (onRight) { scene.setCursor(Cursor.E_RESIZE); resizeMode = 3; } else if (onLeft) { scene.setCursor(Cursor.W_RESIZE); resizeMode = 4; } else { scene.setCursor(Cursor.DEFAULT); resizeMode = 0; } }); scene.setOnMousePressed(mouseEvent -> { if (resizeMode == 0) { dragDelta.x = stage.getX() - mouseEvent.getScreenX(); dragDelta.y = stage.getY() - mouseEvent.getScreenY(); } else { initialX = mouseEvent.getScreenX(); initialY = mouseEvent.getScreenY(); initialWidth = stage.getWidth(); initialHeight = stage.getHeight(); } }); scene.setOnMouseDragged(mouseEvent -> { if (resizeMode != 0) { double newWidth = initialWidth, newHeight = initialHeight, newX = stage.getX(), newY = stage.getY(); if (resizeMode == 3 || resizeMode == 6 || resizeMode == 8) newWidth = initialWidth + (mouseEvent.getScreenX() - initialX); if (resizeMode == 4 || resizeMode == 5 || resizeMode == 7) { newWidth = initialWidth - (mouseEvent.getScreenX() - initialX); newX = initialX + (mouseEvent.getScreenX() - initialX); } if (resizeMode == 2 || resizeMode == 7 || resizeMode == 8) newHeight = initialHeight + (mouseEvent.getScreenY() - initialY); if (resizeMode == 1 || resizeMode == 5 || resizeMode == 6) { newHeight = initialHeight - (mouseEvent.getScreenY() - initialY); newY = initialY + (mouseEvent.getScreenY() - initialY); } if (newWidth >= MIN_WIDTH) { stage.setWidth(newWidth); if (resizeMode == 4 || resizeMode == 5 || resizeMode == 7) stage.setX(newX); } if (newHeight >= MIN_HEIGHT) { stage.setHeight(newHeight); if (resizeMode == 1 || resizeMode == 5 || resizeMode == 6) stage.setY(newY); } } else { stage.setX(mouseEvent.getScreenX() + dragDelta.x); stage.setY(mouseEvent.getScreenY() + dragDelta.y); } });
    }
    private void setupGeminiService() { /* ... no changes ... */ 
        String apiKey = ApiKeyManager.loadApiKey(); if (apiKey == null || apiKey.isBlank()) { commandInput.setDisable(true); commandInput.setPromptText("API Key required. Restart or enter 'setkey' command."); terminalOutput.appendText("Google AI API Key is not set.\n"); Platform.runLater(this::showApiKeyDialog); } else { initializeGeminiService(apiKey); }
    }
    private void showApiKeyDialog() { /* ... no changes ... */ 
        TextInputDialog dialog = new TextInputDialog(); dialog.initStyle(StageStyle.UTILITY); dialog.setTitle("API Key Required"); dialog.setHeaderText("Please enter your Google AI API Key."); dialog.setContentText("API Key:"); Optional<String> result = dialog.showAndWait(); result.ifPresent(key -> { if (!key.isBlank()) { ApiKeyManager.saveApiKey(key); initializeGeminiService(key); } });
    }
    private void initializeGeminiService(String apiKey) { /* ... no changes ... */ 
        geminiService = new GeminiService(apiKey); commandInput.setDisable(false); commandInput.setPromptText("Enter command and press Enter..."); terminalOutput.appendText("Gemini service is ready.\n");
    }
    private void executeCommand(String command) { /* ... no changes ... */ 
        Set<String> shellCommands = new HashSet<>(Arrays.asList("dir", "ls", "echo", "ping", "whoami", "cls", "clear")); String[] parts = command.split("\\s+", 2); String baseCommand = parts[0].toLowerCase(); 
        if ("testnotify".equalsIgnoreCase(baseCommand)) {
            terminalOutput.appendText("Sending a test notification now...\n");
            // 테스트를 위한 가짜 이벤트 객체 생성
            Event testEvent = new Event("This is a Test Notification!", "Now");
            // NotificationService의 메서드를 직접 호출
            NotificationService.getInstance().showNotification(testEvent, 0);
        } 
        if (baseCommand.equals("cls") || baseCommand.equals("clear")) { terminalOutput.clear(); } else if (shellCommands.contains(baseCommand)) { executeShellCommand(command); } else if ("setkey".equalsIgnoreCase(baseCommand)) { Platform.runLater(this::showApiKeyDialog); } else { if (geminiService == null) { Platform.runLater(() -> terminalOutput.appendText("Error: Gemini API key is not set. Use 'setkey' command.\n")); return; } callGeminiApi(command); }
    }
    private void executeShellCommand(String command) { /* ... no changes ... */ 
        new Thread(() -> { try { ProcessBuilder processBuilder; if (System.getProperty("os.name").toLowerCase().contains("win")) { processBuilder = new ProcessBuilder("cmd.exe", "/c", "chcp 65001 > nul && " + command); } else { processBuilder = new ProcessBuilder("bash", "-c", command); } Process process = processBuilder.start(); appendStreamToTerminal(new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)), ""); appendStreamToTerminal(new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)), "ERROR: "); int exitCode = process.waitFor(); Platform.runLater(() -> terminalOutput.appendText("\nProcess finished with exit code: " + exitCode + "\n")); } catch (Exception e) { Platform.runLater(() -> terminalOutput.appendText("Exception: " + e.getMessage() + "\n")); } }).start();
    }
    private void appendStreamToTerminal(BufferedReader reader, String prefix) { /* ... no changes ... */
        reader.lines().forEach(line -> Platform.runLater(() -> terminalOutput.appendText(prefix + line + "\n")));
    }


    private void callGeminiApi(String prompt) {
        Platform.runLater(() -> terminalOutput.appendText("Generating response...\n"));
        geminiService.generateContent(prompt).whenComplete((response, error) -> {
            if (error != null) {
                Platform.runLater(() -> terminalOutput.appendText("Error: " + error.getMessage() + "\n"));
                return;
            }
            Platform.runLater(() -> {
                try {
                    String jsonResponse = response.strip();
                    if (jsonResponse.startsWith("```json")) {
                        jsonResponse = jsonResponse.substring(7, jsonResponse.length() - 3).strip();
                    }

                    JsonObject responseObject = gson.fromJson(jsonResponse, JsonObject.class);

                    // ---▼▼▼ [핵심 수정] "actions" 배열을 처리하는 로직으로 변경 ▼▼▼---
                    if (responseObject.has("error")) {
                         terminalOutput.appendText("Error from Gemini: " + responseObject.get("error").getAsString() + "\n");
                         if(responseObject.has("original_response")){
                            terminalOutput.appendText("Original response: " + responseObject.get("original_response").getAsString() + "\n");
                         }
                         return;
                    }
                    
                    if (!responseObject.has("actions")) {
                        throw new JsonSyntaxException("Response is missing 'actions' array.");
                    }

                    JsonArray actions = responseObject.getAsJsonArray("actions");
                    CalendarDataManager manager = CalendarDataManager.getInstance();
                    boolean changesMade = false;

                    for (JsonElement actionElement : actions) {
                        JsonObject actionObject = actionElement.getAsJsonObject();
                        String action = actionObject.get("action").getAsString();
                        
                        switch (action) {
                            case "add_events": {
                                JsonArray eventsToAdd = actionObject.getAsJsonArray("events");
                                for (JsonElement eventEl : eventsToAdd) {
                                    JsonObject eventData = eventEl.getAsJsonObject();
                                    String title = eventData.get("title").getAsString();
                                    LocalDate date = LocalDate.parse(eventData.get("date").getAsString());
                                    String time = eventData.has("time") ? eventData.get("time").getAsString() : "";
                                    manager.addEventForDate(date, new Event(title, time));
                                    terminalOutput.appendText("Event added: '" + title + "' on " + date + "\n");
                                    changesMade = true;
                                }
                                break;
                            }
                            case "complete_events": {
                                JsonArray eventsToComplete = actionObject.getAsJsonArray("events");
                                for (JsonElement eventEl : eventsToComplete) {
                                    JsonObject eventInfo = eventEl.getAsJsonObject();
                                    String title = eventInfo.get("title").getAsString();
                                    LocalDate date = LocalDate.parse(eventInfo.get("date").getAsString());
                                    if ("all".equalsIgnoreCase(title)) {
                                        List<Event> allEvents = manager.getEventsForDate(date);
                                        for (Event event : allEvents) {
                                            if (!event.isCompleted()) {
                                                manager.toggleEventCompletion(date, event.getTitle());
                                            }
                                        }
                                        terminalOutput.appendText("All events on " + date + " marked as complete.\n");
                                    } else {
                                        manager.toggleEventCompletion(date, title);
                                        terminalOutput.appendText("Event '" + title + "' on " + date + " status toggled.\n");
                                    }
                                    changesMade = true;
                                }
                                break;
                            }
                            case "delete_events": {
                                JsonArray eventsToDelete = actionObject.getAsJsonArray("events");
                                for (JsonElement eventEl : eventsToDelete) {
                                    JsonObject eventInfo = eventEl.getAsJsonObject();
                                    String title = eventInfo.get("title").getAsString();
                                    LocalDate date = LocalDate.parse(eventInfo.get("date").getAsString());
                                    if ("all".equalsIgnoreCase(title)) {
                                        manager.deleteAllEventsForDate(date);
                                        terminalOutput.appendText("All events on " + date + " deleted.\n");
                                    } else {
                                        manager.deleteEventByTitle(date, title);
                                        terminalOutput.appendText("Event '" + title + "' on " + date + " deleted.\n");
                                    }
                                    changesMade = true;
                                }
                                break;
                            }
                            case "copy_events": {
                                LocalDate sourceDate = LocalDate.parse(actionObject.get("source_date").getAsString());
                                LocalDate destDate = LocalDate.parse(actionObject.get("destination_date").getAsString());
                                int count = manager.copyEvents(sourceDate, destDate);

                                if (count > 0) {
                                    terminalOutput.appendText(count + " event(s) copied from " + sourceDate + " to " + destDate + ".\n");
                                    changesMade = true;
                                } else {
                                    terminalOutput.appendText("No events to copy from " + sourceDate + ".\n");
                                }
                                break;
                            }
                            default:
                                terminalOutput.appendText("Gemini (unhandled action): " + action + "\n");
                                break;
                        }
                    }

                    if (changesMade) {
                        calendarView.redraw();
                    }
                    // ---▲▲▲ [핵심 수정] ▲▲▲---
                    
                } catch (Exception e) {
                    terminalOutput.appendText("Error processing response: " + e.getMessage() + "\n");
                    terminalOutput.appendText("Original response: " + response + "\n");
                }
            });
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}