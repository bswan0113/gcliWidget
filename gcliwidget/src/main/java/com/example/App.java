package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * JavaFX App
 */
public class App extends Application {

    private TextArea terminalOutput;

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);

        BorderPane contentPane = new BorderPane();
        contentPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4); -fx-background-radius: 10;");

        CalendarView calendarView = new CalendarView();
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

        // Close button
        Button closeButton = new Button("X");
        closeButton.setStyle("-fx-background-color: rgba(255, 0, 0, 0.7); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 5 10 5 10; -fx-background-radius: 0 10 0 10;");
        closeButton.setOnAction(e -> Platform.exit());

        // Root StackPane to hold content and close button
        StackPane root = new StackPane();
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        root.getChildren().addAll(contentPane, closeButton);

        Scene scene = new Scene(root, 1024, 768);
        scene.setFill(Color.TRANSPARENT);

        stage.setScene(scene);
        stage.setTitle("gcliwidget - Calendar & Terminal");
        stage.show();
    }

    private void executeCommand(String command) {
            Set<String> allowedCommands = new HashSet<>(Arrays.asList("dir", "ls", "echo", "ping", "whoami"));

            String[] parts = command.split("\\s+", 2);
            String baseCommand = parts[0].toLowerCase(); // 명령어는 소문자로 변환하여 비교합니다.
            if (!allowedCommands.contains(baseCommand)) {
                Platform.runLater(() -> terminalOutput.appendText("Error: Command '" + baseCommand + "' is not allowed.\n"));
                return; // 허용되지 않은 명령어일 경우 즉시 종료합니다.
            }
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
    }

    public static void main(String[] args) {
        launch();
    }
}
