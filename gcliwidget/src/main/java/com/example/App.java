package com.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * JavaFX App
 */
public class App extends Application {

    private TextArea terminalOutput;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        // Replace TextArea with the new CalendarView
        CalendarView calendarView = new CalendarView();
        root.setCenter(calendarView);

        VBox terminalPane = new VBox(5);
        terminalPane.setPadding(new Insets(10));

        terminalOutput = new TextArea();
        terminalOutput.setEditable(false);
        terminalOutput.setWrapText(true);
        terminalOutput.setPrefHeight(200);
        terminalOutput.setStyle("-fx-font-family: 'monospaced';");
        terminalOutput.setText("Welcome to gcliwidget!\n");

        TextField commandInput = new TextField();
        commandInput.setPromptText("Enter command and press Enter...");

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
        root.setBottom(terminalPane);

        Scene scene = new Scene(root, 800, 700);
        stage.setScene(scene);
        stage.setTitle("gcliwidget - Calendar & Terminal");
        stage.show();
    }

    private void executeCommand(String command) {
        // Run the command in a background thread to keep the UI responsive
        new Thread(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                // For Windows, execute commands via cmd.exe
                processBuilder.command("cmd.exe", "/c", command);

                Process process = processBuilder.start();

                // Read output from the command
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String outputLine = line;
                    // Update the UI on the JavaFX Application Thread
                    Platform.runLater(() -> terminalOutput.appendText(outputLine + "\n"));
                }

                // Read errors from the command
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
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
