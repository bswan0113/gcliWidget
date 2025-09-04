package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class App extends Application {

    private TextArea terminalOutput;
    private GeminiService geminiService;
    private final Gson gson = new Gson();
    private CalendarView calendarView;


    @Override
    public void start(Stage stage) {
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

        setupGeminiService();

        TextField commandInput = new TextField();
        commandInput.setPromptText("Enter command and press Enter...");
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

        terminalPane.getChildren().addAll(terminalOutput, commandInput);
        contentPane.setBottom(terminalPane);

        Button closeButton = new Button("X");
        closeButton.setStyle("-fx-background-color: rgba(255, 0, 0, 0.7); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 5 10 5 10; -fx-background-radius: 0 10 0 10;");
        closeButton.setOnAction(e -> Platform.exit());

        StackPane root = new StackPane();
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        root.getChildren().addAll(contentPane, closeButton);

        Scene scene = new Scene(root, 1024, 768);
        scene.setFill(Color.TRANSPARENT);

        stage.setScene(scene);
        stage.setTitle("gcliwidget - Calendar & Terminal");
        stage.show();
    }

    private void setupGeminiService() {
        String apiKey = ApiKeyManager.loadApiKey();
        if (apiKey == null) {
            Platform.runLater(() -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("API Key Required");
                dialog.setHeaderText("Please enter your Google AI API Key.");
                dialog.setContentText("API Key:");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(key -> {
                    ApiKeyManager.saveApiKey(key);
                    geminiService = new GeminiService(key);
                    terminalOutput.appendText("API Key saved. You can now use the gemini command.\n");
                });
            });
        } else {
            geminiService = new GeminiService(apiKey);
            terminalOutput.appendText("Gemini service is ready.\n");
        }
    }

    private void executeCommand(String command) {
        Set<String> shellCommands = new HashSet<>(Arrays.asList("dir", "ls", "echo", "ping", "whoami"));

        String[] parts = command.split("\\s+", 2);
        String baseCommand = parts[0].toLowerCase();

        if (shellCommands.contains(baseCommand)) {
            // Execute shell command
            new Thread(() -> {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder();
                    processBuilder.command("cmd.exe", "/c", "chcp 65001 > nul && " + command);

                    Process process = processBuilder.start();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String outputLine = line;
                        Platform.runLater(() -> terminalOutput.appendText(outputLine + "\n"));
                    }

                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
                    while ((line = errorReader.readLine()) != null) {
                        final String errorLine = line;
                        Platform.runLater(() -> terminalOutput.appendText("ERROR: " + errorLine + "\n"));
                    }

                    int exitCode = process.waitFor();
                    Platform.runLater(() -> terminalOutput.appendText("\nProcess finished with exit code: " + exitCode + "\n"));

                } catch (Exception e) {
                    Platform.runLater(() -> terminalOutput.appendText("Exception: " + e.getMessage() + "\n"));
                }
            }).start();
        } else {
            // Treat as a prompt for Gemini
            if (geminiService == null) {
                Platform.runLater(() -> {
                    terminalOutput.appendText("Error: Gemini API key is not set. Please restart the application to set the API key.\n");
                    setupGeminiService();
                });
                return;
            }

            String prompt = command;
            Platform.runLater(() -> terminalOutput.appendText("Generating response...\n"));
            geminiService.generateContent(prompt).whenComplete((response, error) -> {
                if (error != null) {
                    Platform.runLater(() -> terminalOutput.appendText("Error: " + error.getMessage() + "\n"));
                } else {
                    try {
                        String jsonResponse = response;
                        if (jsonResponse.startsWith("```json")) {
                            jsonResponse = jsonResponse.substring(7, jsonResponse.length() - 3).trim();
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

                                Platform.runLater(() -> {
                                    terminalOutput.appendText("Event added: " + title + " on " + dateStr + "\n");
                                    calendarView.redraw(); // Redraw the calendar to show the new note
                                });

                            } else {
                                Platform.runLater(() -> terminalOutput.appendText("Gemini: " + response + "\n"));
                            }
                        } else {
                             Platform.runLater(() -> terminalOutput.appendText("Gemini: " + response + "\n"));
                        }
                    } catch (Exception e) {
                        // Not a JSON response, just display it
                        Platform.runLater(() -> terminalOutput.appendText("Gemini: " + response + "\n"));
                    }
                }
            });
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
