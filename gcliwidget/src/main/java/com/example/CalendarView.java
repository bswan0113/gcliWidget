package com.example;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class CalendarView extends BorderPane {

    private YearMonth currentYearMonth;
    private final Label monthYearLabel;
    private final GridPane calendarGrid;
    private VBox currentlyOpenCell = null;
    private final CalendarDataManager dataManager;

    public CalendarView() {
        this.dataManager = CalendarDataManager.getInstance();
        this.currentYearMonth = YearMonth.now();
        this.setStyle("-fx-background-color: transparent;");

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new javafx.geometry.Insets(20));

        Button prevButton = new Button("<");
        prevButton.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        prevButton.setOnAction(e -> changeMonth(-1));

        monthYearLabel = new Label();
        monthYearLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        monthYearLabel.setTextFill(Color.WHITE);

        Button nextButton = new Button(">");
        nextButton.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        nextButton.setOnAction(e -> changeMonth(1));

        header.getChildren().addAll(prevButton, monthYearLabel, nextButton);
        this.setTop(header);

        calendarGrid = new GridPane();
        calendarGrid.setHgap(5);
        calendarGrid.setVgap(5);
        calendarGrid.setPadding(new javafx.geometry.Insets(10));
        setupCalendarGridConstraints();
        this.setCenter(calendarGrid);

        drawCalendar();
    }

    private void setupCalendarGridConstraints() {
        for (int i = 0; i < 7; i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(colConst);
        }
        RowConstraints rowConstHeader = new RowConstraints();
        rowConstHeader.setVgrow(Priority.NEVER);
        calendarGrid.getRowConstraints().add(rowConstHeader);
        for (int i = 0; i < 6; i++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setVgrow(Priority.ALWAYS);
            calendarGrid.getRowConstraints().add(rowConst);
        }
    }

    private void drawCalendar() {
        calendarGrid.getChildren().clear();
        currentlyOpenCell = null;

        monthYearLabel.setText(currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        for (int i = 0; i < 7; i++) {
            DayOfWeek day = DayOfWeek.SUNDAY.plus(i);
            Label dayLabel = new Label(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            dayLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            dayLabel.setTextFill(Color.WHITE);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            calendarGrid.add(dayLabel, i, 0);
        }

        LocalDate firstDayOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekOfFirst = firstDayOfMonth.getDayOfWeek().getValue() % 7;

        int daysInMonth = currentYearMonth.lengthOfMonth();
        for (int i = 0; i < daysInMonth; i++) {
            LocalDate date = currentYearMonth.atDay(i + 1);
            int row = (i + dayOfWeekOfFirst) / 7 + 1;
            int col = (i + dayOfWeekOfFirst) % 7;

            VBox dayCell = createDayCell(date);
            calendarGrid.add(dayCell, col, row);
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cellBox = new VBox(5);
        cellBox.setAlignment(Pos.TOP_LEFT);
        cellBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 8; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 8;");
        cellBox.setPadding(new javafx.geometry.Insets(5));

        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        dayLabel.setTextFill(Color.WHITE);

        TextArea notesArea = new TextArea();
        notesArea.setEditable(false);
        notesArea.setWrapText(true);
        notesArea.setVisible(false);
        notesArea.setManaged(false);
        notesArea.setStyle("-fx-control-inner-background: rgba(0,0,0,0.4); -fx-text-fill: white; -fx-font-family: 'Segoe UI';");

        List<Event> events = dataManager.getEventsForDate(date);
        if (!events.isEmpty()) {
            StringBuilder notesText = new StringBuilder();
            for (Event event : events) {
                notesText.append(event.toString()).append("\n");
            }
            notesArea.setText(notesText.toString());
        } else {
            notesArea.setPromptText("No events");
        }


        cellBox.getChildren().addAll(dayLabel, notesArea);

        if (date.equals(LocalDate.now())) {
            cellBox.setStyle("-fx-background-color: rgba(173, 216, 230, 0.2); -fx-background-radius: 8; -fx-border-color: #add8e6; -fx-border-radius: 8;");
        }

        cellBox.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (currentlyOpenCell != null && currentlyOpenCell != cellBox) {
                TextArea previouslyOpenNotes = (TextArea) currentlyOpenCell.getChildren().get(1);
                previouslyOpenNotes.setManaged(false);
                previouslyOpenNotes.setVisible(false);
            }

            if (notesArea.isManaged()) {
                notesArea.setManaged(false);
                notesArea.setVisible(false);
                currentlyOpenCell = null;
            } else {
                notesArea.setManaged(true);
                notesArea.setVisible(true);
                currentlyOpenCell = cellBox;
            }
            event.consume();
        });
        cellBox.setUserData(date);

        return cellBox;
    }

    private void changeMonth(int amount) {
        currentYearMonth = currentYearMonth.plusMonths(amount);
        drawCalendar();
    }

    public void redraw() {
        drawCalendar();
    }
}