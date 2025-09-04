package com.example;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.StageStyle;

public class CalendarView extends VBox {

    private YearMonth currentYearMonth;
    private final GridPane calendarGrid;
    private final Label monthTitle;
    private final CalendarDataManager dataManager;

    private static final DataFormat EVENT_DATA_FORMAT = new DataFormat("com.example.event");

    public CalendarView(LocalDate initialDate) {
        this.dataManager = CalendarDataManager.getInstance();
        this.currentYearMonth = YearMonth.from(initialDate);
        this.monthTitle = new Label();

        this.setPadding(new Insets(10));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: transparent;");

        HBox header = createHeader();
        GridPane dayOfWeekHeader = createDayOfWeekHeader();
        
        calendarGrid = new GridPane();
        calendarGrid.setHgap(5);
        calendarGrid.setVgap(5);
        setupGridConstraints(calendarGrid, 6, 7);

        this.getChildren().addAll(header, dayOfWeekHeader, calendarGrid);
        VBox.setVgrow(calendarGrid, Priority.ALWAYS);
        drawCalendar();
    }

    public void redraw() {
        drawCalendar();
    }

    private void changeMonth(int amount) {
        currentYearMonth = currentYearMonth.plusMonths(amount);
        drawCalendar();
    }

    private void drawCalendar() {
        calendarGrid.getChildren().clear();
        monthTitle.setText(currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())));
        LocalDate firstDayOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekOfFirst = firstDayOfMonth.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : firstDayOfMonth.getDayOfWeek().getValue();

        for (int i = 0; i < currentYearMonth.lengthOfMonth(); i++) {
            LocalDate date = firstDayOfMonth.plusDays(i);
            int row = (i + dayOfWeekOfFirst) / 7;
            int col = (i + dayOfWeekOfFirst) % 7;

            if (row < 6) {
                VBox dayCell = createDayCell(date);
                calendarGrid.add(dayCell, col, row);
            }
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cellBox = new VBox(3);
        cellBox.setPadding(new Insets(5));
        cellBox.setAlignment(Pos.TOP_LEFT);
        String baseStyle = "-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 8;";
        cellBox.setStyle(baseStyle);
        if (date.equals(LocalDate.now())) {
            cellBox.setStyle("-fx-background-color: rgba(173, 216, 230, 0.2); -fx-background-radius: 8; -fx-border-color: #add8e6; -fx-border-radius: 8; -fx-border-width: 1;");
        }
        
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        dayLabel.setTextFill(Color.WHITE);

        ScrollPane scrollPane = new ScrollPane();
        VBox eventsContainer = new VBox(2);
        eventsContainer.setStyle("-fx-background-color: transparent;");
        scrollPane.setContent(eventsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        cellBox.getChildren().addAll(dayLabel, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Populate events using the new createEventNode method
        dataManager.getEventsForDate(date).forEach(event -> {
            HBox eventNode = createEventNode(event, date);
            eventsContainer.getChildren().add(eventNode);
        });
        
        setupDragAndDropTarget(cellBox, date);

        return cellBox;
    }
    
private HBox createEventNode(Event event, LocalDate date) {
    HBox eventBox = new HBox(5);
    eventBox.setAlignment(Pos.CENTER_LEFT);
    
    CheckBox checkBox = new CheckBox();
    checkBox.setSelected(event.isCompleted());
    
    Label eventLabel = new Label(event.toString());
    eventLabel.setWrapText(true);
    eventLabel.setTextFill(Color.WHITE);
    eventLabel.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(eventLabel, Priority.ALWAYS);

    eventBox.getChildren().addAll(checkBox, eventLabel);
    
    updateEventNodeStyle(eventBox, event.isCompleted());
    
    checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
        event.setCompleted(isSelected);
        dataManager.updateEvent(date, event);
        updateEventNodeStyle(eventBox, isSelected);
    });
    
    eventBox.setUserData(event);
    
    eventLabel.setOnMouseClicked(mouseEvent -> {
        if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
            inlineEdit(eventBox, event, date);
        }
    });
    
    ContextMenu contextMenu = new ContextMenu();
    MenuItem editItem = new MenuItem("Edit Details");
    editItem.setOnAction(e -> showEditEventDialog(event, date));
    MenuItem deleteItem = new MenuItem("Delete");
    deleteItem.setOnAction(e -> deleteEvent(event, date));
    contextMenu.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);
    
    // HBox에는 setContextMenu가 없으므로 setOnContextMenuRequested 이벤트를 사용합니다.
    eventBox.setOnContextMenuRequested(event_ -> 
        contextMenu.show(eventBox, event_.getScreenX(), event_.getScreenY())
    );
    setupDragAndDropSource(eventBox, event);
    
    return eventBox;
}
    
    private void updateEventNodeStyle(HBox eventBox, boolean isCompleted) {
        Label label = (Label) eventBox.getChildren().get(1); // The label is the second child
        if (isCompleted) {
            eventBox.setStyle("-fx-background-color: rgba(152, 251, 152, 0.5); -fx-padding: 3 5; -fx-background-radius: 4;");
            label.setStyle("-fx-strikethrough: true; -fx-text-fill: #556B2F;"); // Dark green text for readability
        } else {
            eventBox.setStyle("-fx-background-color: rgba(70, 130, 180, 0.6); -fx-padding: 3 5; -fx-background-radius: 4;");
            label.setStyle("-fx-strikethrough: false; -fx-text-fill: white;");
        }
    }
    
    private void inlineEdit(HBox eventBox, Event event, LocalDate date) {
        Label originalLabel = (Label) eventBox.getChildren().get(1);
        TextField editField = new TextField(event.getTitle());
        HBox.setHgrow(editField, Priority.ALWAYS);
        eventBox.getChildren().set(1, editField);
        editField.requestFocus();
        editField.selectAll();

        Runnable saveAction = () -> {
            String newTitle = editField.getText().trim();
            if(!newTitle.isEmpty()) {
                event.setTitle(newTitle);
                dataManager.updateEvent(date, event);
                originalLabel.setText(event.toString());
                eventBox.getChildren().set(1, originalLabel);
            } else {
                eventBox.getChildren().set(1, originalLabel);
            }
        };

        editField.setOnAction(e -> saveAction.run());
        editField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (wasFocused && !isNowFocused) {
                saveAction.run();
            }
        });
    }

    private void showEditEventDialog(Event event, LocalDate date) {
        Dialog<Event> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Edit Event");
        dialog.initOwner(this.getScene().getWindow());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField(event.getTitle());
        TextField timeField = new TextField(event.getTime());
        timeField.setPromptText("HH:MM (optional)");

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Time:"), 0, 1);
        grid.add(timeField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                event.setTitle(titleField.getText());
                event.setTime(timeField.getText());
                return event;
            }
            return null;
        });

        Optional<Event> result = dialog.showAndWait();
        result.ifPresent(updatedEvent -> {
            dataManager.updateEvent(date, updatedEvent);
            redraw();
        });
    }

    private void deleteEvent(Event event, LocalDate date) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(this.getScene().getWindow());
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Event: '" + event.getTitle() + "'");
        alert.setContentText("Are you sure you want to delete this event?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            dataManager.deleteEvent(date, event.getId());
            redraw();
        }
    }

    private void setupDragAndDropSource(HBox eventBox, Event event) {
        eventBox.setOnDragDetected(eventDetected -> {
            Dragboard db = eventBox.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(EVENT_DATA_FORMAT, event.getId());
            db.setContent(content);
            db.setDragView(eventBox.snapshot(null, null));
            eventDetected.consume();
        });
    }
    
    private void setupDragAndDropTarget(VBox dayCell, LocalDate date) {
        dayCell.setOnDragOver(event -> {
            if (event.getGestureSource() != dayCell && event.getDragboard().hasContent(EVENT_DATA_FORMAT)) {
                event.acceptTransferModes(TransferMode.MOVE);
                dayCell.setStyle("-fx-background-color: rgba(70, 130, 180, 0.8); -fx-background-radius: 8;");
            }
            event.consume();
        });

        dayCell.setOnDragExited(event -> {
            String baseStyle = "-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 8;";
             if (date.equals(LocalDate.now())) {
                baseStyle = "-fx-background-color: rgba(173, 216, 230, 0.2); -fx-background-radius: 8; -fx-border-color: #add8e6; -fx-border-radius: 8; -fx-border-width: 1;";
            }
            dayCell.setStyle(baseStyle);
        });
        
        dayCell.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasContent(EVENT_DATA_FORMAT)) {
                String eventId = (String) db.getContent(EVENT_DATA_FORMAT);
                dataManager.moveEvent(eventId, date);
                success = true;
                redraw();
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
    
    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER);
        Button prevMonthButton = new Button("<");
        prevMonthButton.setOnAction(e -> changeMonth(-1));
        Button nextMonthButton = new Button(">");
        nextMonthButton.setOnAction(e -> changeMonth(1));
        
        monthTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        monthTitle.setTextFill(Color.WHITE);
        
        String buttonStyle = "-fx-background-color: rgba(255, 255, 255, 0.2); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;";
        prevMonthButton.setStyle(buttonStyle);
        nextMonthButton.setStyle(buttonStyle);
        
        header.getChildren().addAll(prevMonthButton, monthTitle, nextMonthButton);
        return header;
    }

    private GridPane createDayOfWeekHeader() {
        GridPane dayHeaderGrid = new GridPane();
        dayHeaderGrid.setHgap(5);
        setupGridConstraints(dayHeaderGrid, 1, 7);
        for (int i = 0; i < 7; i++) {
            DayOfWeek day = (i == 0) ? DayOfWeek.SUNDAY : DayOfWeek.SUNDAY.plus(i);
            Label dayLabel = new Label(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            dayLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            dayLabel.setTextFill(Color.WHITE);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            dayHeaderGrid.add(dayLabel, i, 0);
        }
        return dayHeaderGrid;
    }
    
    private void setupGridConstraints(GridPane gridPane, int numRows, int numCols) {
        gridPane.getColumnConstraints().clear();
        gridPane.getRowConstraints().clear();
        for (int i = 0; i < numCols; i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setHgrow(Priority.ALWAYS);
            gridPane.getColumnConstraints().add(colConst);
        }
        for (int i = 0; i < numRows; i++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setVgrow(Priority.ALWAYS);
            gridPane.getRowConstraints().add(rowConst);
        }
    }
}