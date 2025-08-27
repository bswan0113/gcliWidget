package com.example;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class CalendarView extends BorderPane {

    private YearMonth currentYearMonth;
    private final Label monthYearLabel;
    private final GridPane calendarGrid;

    public CalendarView() {
        this.currentYearMonth = YearMonth.now();

        // Header with month/year and navigation buttons
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new javafx.geometry.Insets(10));

        Button prevButton = new Button("<");
        prevButton.setOnAction(e -> changeMonth(-1));

        monthYearLabel = new Label();
        monthYearLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        Button nextButton = new Button(">");
        nextButton.setOnAction(e -> changeMonth(1));

        header.getChildren().addAll(prevButton, monthYearLabel, nextButton);
        this.setTop(header);

        // Calendar grid
        calendarGrid = new GridPane();
        calendarGrid.setAlignment(Pos.CENTER);
        calendarGrid.setHgap(10);
        calendarGrid.setVgap(10);
        calendarGrid.setPadding(new javafx.geometry.Insets(10));
        this.setCenter(calendarGrid);

        drawCalendar();
    }

    private void drawCalendar() {
        calendarGrid.getChildren().clear();

        // Update month-year label
        monthYearLabel.setText(currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        // Add days of the week header
        for (int i = 0; i < 7; i++) {
            DayOfWeek day = DayOfWeek.SUNDAY.plus(i);
            Label dayLabel = new Label(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            dayLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            calendarGrid.add(dayLabel, i, 0);
        }

        // Determine the first day of the month
        LocalDate firstDayOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekOfFirst = firstDayOfMonth.getDayOfWeek().getValue() % 7; // SUN=0, MON=1, ...

        // Add day cells
        int daysInMonth = currentYearMonth.lengthOfMonth();
        for (int i = 0; i < daysInMonth; i++) {
            int day = i + 1;
            int row = (i + dayOfWeekOfFirst) / 7 + 1;
            int col = (i + dayOfWeekOfFirst) % 7;

            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.setFont(Font.font("Arial", 14));
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);

            // Highlight today
            if (currentYearMonth.equals(YearMonth.now()) && day == LocalDate.now().getDayOfMonth()) {
                dayLabel.setStyle("-fx-background-color: #add8e6; -fx-background-radius: 50%;");
            }

            calendarGrid.add(dayLabel, col, row);
        }
    }

    private void changeMonth(int amount) {
        currentYearMonth = currentYearMonth.plusMonths(amount);
        drawCalendar();
    }
}
