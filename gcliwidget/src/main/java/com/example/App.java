
package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class App extends Application {

    // --- 클래스 멤버 변수로 통합 ---
    private TextArea terminalOutput;
    private TextField commandInput;
    private GeminiService geminiService;
    private final Gson gson = new Gson();
    private CalendarView calendarView;

    // 창 드래그 및 리사이징을 위한 변수들
    private final Delta dragDelta = new Delta();
    private double initialX, initialY, initialWidth, initialHeight;
    private int resizeMode = 0; // 0: none, 1-8: resize directions

    // 리사이징 관련 상수
    private static final int RESIZE_BORDER_WIDTH = 10;
    private static final double MIN_WIDTH = 400;
    private static final double MIN_HEIGHT = 300;

    // 중첩 클래스: 드래그 좌표 저장용
    static class Delta {
        double x, y;
    }

    @Override
    public void start(final Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);

        BorderPane contentPane = new BorderPane();
        contentPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); -fx-background-radius: 10;");

        calendarView = new CalendarView();
        contentPane.setCenter(calendarView);

        VBox terminalPane = new VBox(5);
        terminalPane.setPadding(new Insets(10));
        terminalPane.setStyle("-fx-background-color: rgba(20, 20, 20, 0.6); -fx-background-radius: 8;");

        terminalOutput = new TextArea();
        terminalOutput.setEditable(false);
        terminalOutput.setWrapText(true);
        terminalOutput.setPrefHeight(200);
        terminalOutput.setStyle("-fx-font-family: 'monospaced'; -fx-control-inner-background: rgba(0,0,0,0.5); -fx-text-fill: white;");
        terminalOutput.setText("Welcome to gcliwidget!\n");

        commandInput = new TextField();
        commandInput.setStyle("-fx-font-family: 'monospaced'; -fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white;");
        commandInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String command = commandInput.getText();
                if (command != null && !command.isBlank()) {
                    terminalOutput.appendText("> " + command + "\n");
                    executeCommand(command);
                    commandInput.clear();
                }
            }
        });

        setupGeminiService(); // API 키 설정 및 commandInput 활성화/비활성화 처리

        Slider transparencySlider = new Slider(0.1, 1.0, 0.7);
        transparencySlider.setBlockIncrement(0.1);
        transparencySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            stage.setOpacity(newVal.doubleValue());
        });

        terminalPane.getChildren().addAll(terminalOutput, commandInput, transparencySlider);
        contentPane.setBottom(terminalPane);

        Button closeButton = new Button("X");
        closeButton.setStyle("-fx-background-color: rgba(255, 0, 0, 0.7); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 5 10 5 10; -fx-background-radius: 0 10 0 10;");
        closeButton.setOnAction(e -> Platform.exit());

        StackPane root = new StackPane();
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        root.getChildren().addAll(contentPane, closeButton);

        Scene scene = new Scene(root, 1200, 800, Color.TRANSPARENT);

        // --- 창 드래그 및 리사이징 로직 통합 ---
        setupWindowHandlers(scene, stage);

        stage.setScene(scene);
        stage.setTitle("gcliwidget - Calendar & Terminal");
        stage.show();
    }

    private void setupWindowHandlers(Scene scene, Stage stage) {
        scene.setOnMouseMoved(mouseEvent -> {
            double x = mouseEvent.getX();
            double y = mouseEvent.getY();
            double width = stage.getWidth();
            double height = stage.getHeight();

            boolean onTop = y < RESIZE_BORDER_WIDTH;
            boolean onBottom = y > height - RESIZE_BORDER_WIDTH;
            boolean onLeft = x < RESIZE_BORDER_WIDTH;
            boolean onRight = x > width - RESIZE_BORDER_WIDTH;

            if (onTop && onLeft) { scene.setCursor(Cursor.NW_RESIZE); resizeMode = 5; }
            else if (onTop && onRight) { scene.setCursor(Cursor.NE_RESIZE); resizeMode = 6; }
            else if (onBottom && onLeft) { scene.setCursor(Cursor.SW_RESIZE); resizeMode = 7; }
            else if (onBottom && onRight) { scene.setCursor(Cursor.SE_RESIZE); resizeMode = 8; }
            else if (onTop) { scene.setCursor(Cursor.N_RESIZE); resizeMode = 1; }
            else if (onBottom) { scene.setCursor(Cursor.S_RESIZE); resizeMode = 2; }
            else if (onRight) { scene.setCursor(Cursor.E_RESIZE); resizeMode = 3; }
            else if (onLeft) { scene.setCursor(Cursor.W_RESIZE); resizeMode = 4; }
            else { scene.setCursor(Cursor.DEFAULT); resizeMode = 0; }
        });

        scene.setOnMousePressed(mouseEvent -> {
            if (resizeMode == 0) { // Drag mode
                dragDelta.x = stage.getX() - mouseEvent.getScreenX();
                dragDelta.y = stage.getY() - mouseEvent.getScreenY();
            } else { // Resize mode
                initialX = mouseEvent.getScreenX();
                initialY = mouseEvent.getScreenY();
                initialWidth = stage.getWidth();
                initialHeight = stage.getHeight();
            }
        });

        scene.setOnMouseDragged(mouseEvent -> {
            if (resizeMode != 0) { // Resizing
                double newWidth = initialWidth;
                double newHeight = initialHeight;
                double newX = stage.getX();
                double newY = stage.getY();

                // Horizontal resizing
                if (resizeMode == 3 || resizeMode == 6 || resizeMode == 8) { // East
                    newWidth = initialWidth + (mouseEvent.getScreenX() - initialX);
                }
                if (resizeMode == 4 || resizeMode == 5 || resizeMode == 7) { // West
                    newWidth = initialWidth - (mouseEvent.getScreenX() - initialX);
                    newX = initialX + (mouseEvent.getScreenX() - initialX);
                }

                // Vertical resizing
                if (resizeMode == 2 || resizeMode == 7 || resizeMode == 8) { // South
                    newHeight = initialHeight + (mouseEvent.getScreenY() - initialY);
                }
                if (resizeMode == 1 || resizeMode == 5 || resizeMode == 6) { // North
                    newHeight = initialHeight - (mouseEvent.getScreenY() - initialY);
                    newY = initialY + (mouseEvent.getScreenY() - initialY);
                }

                if (newWidth >= MIN_WIDTH) {
                    stage.setWidth(newWidth);
                    if (resizeMode == 4 || resizeMode == 5 || resizeMode == 7) stage.setX(newX);
                }
                if (newHeight >= MIN_HEIGHT) {
                    stage.setHeight(newHeight);
                    if (resizeMode == 1 || resizeMode == 5 || resizeMode == 6) stage.setY(newY);
                }
            } else { // Dragging
                stage.setX(mouseEvent.getScreenX() + dragDelta.x);
                stage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });
    }

    private void setupGeminiService() {
        String apiKey = ApiKeyManager.loadApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            commandInput.setDisable(true);
            commandInput.setPromptText("API Key required. Restart or enter 'setkey' command.");
            terminalOutput.appendText("Google AI API Key is not set.\n");
            // Show dialog on a separate thread to not block UI rendering
            Platform.runLater(this::showApiKeyDialog);
        } else {
            initializeGeminiService(apiKey);
        }
    }

    private void showApiKeyDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("API Key Required");
        dialog.setHeaderText("Please enter your Google AI API Key.");
        dialog.setContentText("API Key:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(key -> {
            if (!key.isBlank()) {
                ApiKeyManager.saveApiKey(key);
                initializeGeminiService(key);
            }
        });
        // If user cancels, input remains disabled.
    }
    
    private void initializeGeminiService(String apiKey) {
        geminiService = new GeminiService(apiKey);
        commandInput.setDisable(false);
        commandInput.setPromptText("Enter command and press Enter...");
        terminalOutput.appendText("Gemini service is ready.\n");
    }

    private void executeCommand(String command) {
        Set<String> shellCommands = new HashSet<>(Arrays.asList("dir", "ls", "echo", "ping", "whoami"));

        String[] parts = command.split("\\s+", 2);
        String baseCommand = parts[0].toLowerCase();

        if (shellCommands.contains(baseCommand)) {
            executeShellCommand(command);
        } else if ("setkey".equalsIgnoreCase(baseCommand)) {
            Platform.runLater(this::showApiKeyDialog);
        } else {
            // Treat as a prompt for Gemini
            if (geminiService == null) {
                Platform.runLater(() -> {
                    terminalOutput.appendText("Error: Gemini API key is not set. Please use 'setkey' command.\n");
                });
                return;
            }
            callGeminiApi(command);
        }
    }

    private void executeShellCommand(String command) {
        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    // For Windows, ensure UTF-8 output
                    processBuilder.command("cmd.exe", "/c", "chcp 65001 > nul && " + command);
                } else {
                    // For Linux/Mac
                    processBuilder.command("bash", "-c", command);
                }

                Process process = processBuilder.start();
                appendStreamToTerminal(new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)), "");
                appendStreamToTerminal(new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)), "ERROR: ");
                
                int exitCode = process.waitFor();
                Platform.runLater(() -> terminalOutput.appendText("\nProcess finished with exit code: " + exitCode + "\n"));

            } catch (Exception e) {
                Platform.runLater(() -> terminalOutput.appendText("Exception: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    private void appendStreamToTerminal(BufferedReader reader, String prefix) {
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
                    String jsonResponse = response;
                    if (response.startsWith("```json")) {
                        jsonResponse = response.substring(7, response.length() - 3).trim();
                    }

                    JsonObject responseObject = gson.fromJson(jsonResponse, JsonObject.class);
                    if (responseObject.has("action")) {
                        String action = responseObject.get("action").getAsString();
                        if ("add_event".equals(action)) {
                            String title = responseObject.get("title").getAsString();
                            String dateStr = responseObject.get("date").getAsString();
                            String timeStr = responseObject.has("time") ? responseObject.get("time").getAsString() : "";

                            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                            Event newEvent = new Event(title, timeStr);
                            
                            CalendarDataManager.getInstance().addEventForDate(date, newEvent);
                            terminalOutput.appendText("Event added: " + title + " on " + dateStr + "\n");
                            calendarView.redraw();
                        } else {
                            terminalOutput.appendText("Gemini (unhandled action): " + response + "\n");
                        }
                    } else {
                        terminalOutput.appendText("Gemini (JSON but no action): " + response + "\n");
                    }
                } catch (JsonSyntaxException e) {
                    // Not a JSON response, just display it
                    terminalOutput.appendText("Gemini: " + response + "\n");
                } catch (Exception e) {
                    // Other potential errors during JSON processing
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
