package net.talaatharb.workday.ui.controllers;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.task.TaskScheduledEvent;
import net.talaatharb.workday.facade.CalendarFacade;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.model.Task;

/**
 * Controller for calendar view with month, week, and day views.
 * Supports drag-and-drop task scheduling.
 */
@Slf4j
public class CalendarViewController implements Initializable {
    
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final String[] DAY_NAMES = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    
    @FXML
    private Label monthYearLabel;
    
    @FXML
    private Button previousButton;
    
    @FXML
    private Button todayButton;
    
    @FXML
    private Button nextButton;
    
    @FXML
    private ChoiceBox<ViewMode> viewModeChoice;
    
    @FXML
    private VBox monthViewContainer;
    
    @FXML
    private GridPane dayHeadersGrid;
    
    @FXML
    private GridPane calendarGrid;
    
    @FXML
    private VBox weekViewContainer;
    
    @FXML
    private GridPane weekGrid;
    
    @FXML
    private VBox dayViewContainer;
    
    @FXML
    private GridPane dayGrid;
    
    @FXML
    private Label taskCountLabel;
    
    @Setter
    private TaskFacade taskFacade;
    
    @Setter
    private CalendarFacade calendarFacade;
    
    @Setter
    private EventDispatcher eventDispatcher;
    
    private ViewMode currentViewMode = ViewMode.MONTH;
    private LocalDate currentDate = LocalDate.now();
    private YearMonth currentYearMonth = YearMonth.now();
    private List<Task> currentTasks = new ArrayList<>();
    private Task draggedTask;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing CalendarViewController");
        
        // Setup view mode choice box
        viewModeChoice.setItems(FXCollections.observableArrayList(ViewMode.values()));
        viewModeChoice.setValue(ViewMode.MONTH);
        viewModeChoice.setOnAction(e -> handleViewModeChange());
        
        // Initialize with month view
        refreshCalendarView();
        
        log.info("CalendarViewController initialized successfully");
    }
    
    /**
     * Handle view mode change
     */
    private void handleViewModeChange() {
        currentViewMode = viewModeChoice.getValue();
        log.debug("View mode changed to: {}", currentViewMode);
        
        // Show/hide appropriate containers
        monthViewContainer.setVisible(currentViewMode == ViewMode.MONTH);
        monthViewContainer.setManaged(currentViewMode == ViewMode.MONTH);
        
        weekViewContainer.setVisible(currentViewMode == ViewMode.WEEK);
        weekViewContainer.setManaged(currentViewMode == ViewMode.WEEK);
        
        dayViewContainer.setVisible(currentViewMode == ViewMode.DAY);
        dayViewContainer.setManaged(currentViewMode == ViewMode.DAY);
        
        refreshCalendarView();
    }
    
    /**
     * Handle previous button click
     */
    @FXML
    private void handlePrevious() {
        log.debug("Previous button clicked");
        
        switch (currentViewMode) {
            case MONTH:
                currentYearMonth = currentYearMonth.minusMonths(1);
                currentDate = currentYearMonth.atDay(1);
                break;
            case WEEK:
                currentDate = currentDate.minusWeeks(1);
                break;
            case DAY:
                currentDate = currentDate.minusDays(1);
                break;
        }
        
        refreshCalendarView();
    }
    
    /**
     * Handle today button click
     */
    @FXML
    private void handleToday() {
        log.debug("Today button clicked");
        
        currentDate = LocalDate.now();
        currentYearMonth = YearMonth.from(currentDate);
        
        refreshCalendarView();
    }
    
    /**
     * Handle next button click
     */
    @FXML
    private void handleNext() {
        log.debug("Next button clicked");
        
        switch (currentViewMode) {
            case MONTH:
                currentYearMonth = currentYearMonth.plusMonths(1);
                currentDate = currentYearMonth.atDay(1);
                break;
            case WEEK:
                currentDate = currentDate.plusWeeks(1);
                break;
            case DAY:
                currentDate = currentDate.plusDays(1);
                break;
        }
        
        refreshCalendarView();
    }
    
    /**
     * Refresh calendar view based on current mode
     */
    private void refreshCalendarView() {
        // Update header label
        updateHeaderLabel();
        
        // Load tasks for current period
        loadTasksForCurrentPeriod();
        
        // Render appropriate view
        switch (currentViewMode) {
            case MONTH:
                renderMonthView();
                break;
            case WEEK:
                renderWeekView();
                break;
            case DAY:
                renderDayView();
                break;
        }
        
        // Update task count
        updateTaskCount();
    }

    /**
     * Reload calendar tasks after dependencies are injected.
     */
    public void loadCalendarTasks() {
        refreshCalendarView();
    }
    
    /**
     * Update header label based on view mode
     */
    private void updateHeaderLabel() {
        switch (currentViewMode) {
            case MONTH:
                monthYearLabel.setText(currentYearMonth.format(MONTH_YEAR_FORMATTER));
                break;
            case WEEK:
                LocalDate weekStart = currentDate.minusDays(currentDate.getDayOfWeek().getValue() % 7);
                LocalDate weekEnd = weekStart.plusDays(6);
                monthYearLabel.setText(String.format("%s - %s", 
                    weekStart.format(DateTimeFormatter.ofPattern("MMM d")),
                    weekEnd.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))));
                break;
            case DAY:
                monthYearLabel.setText(currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
                break;
        }
    }
    
    /**
     * Load tasks for current period
     */
    private void loadTasksForCurrentPeriod() {
        if (calendarFacade == null) {
            currentTasks = new ArrayList<>();
            return;
        }
        
        try {
            switch (currentViewMode) {
                case MONTH:
                    LocalDate monthStart = currentYearMonth.atDay(1);
                    LocalDate monthEnd = currentYearMonth.atEndOfMonth();
                    currentTasks = calendarFacade.getTasksForPeriod(monthStart, monthEnd);
                    break;
                case WEEK:
                    LocalDate weekStart = currentDate.minusDays(currentDate.getDayOfWeek().getValue() % 7);
                    LocalDate weekEnd = weekStart.plusDays(6);
                    currentTasks = calendarFacade.getTasksForPeriod(weekStart, weekEnd);
                    break;
                case DAY:
                    currentTasks = calendarFacade.getTasksForDay(currentDate);
                    break;
            }
            
            log.debug("Loaded {} tasks for current period", currentTasks.size());
        } catch (Exception e) {
            log.error("Failed to load tasks", e);
            currentTasks = new ArrayList<>();
        }
    }
    
    /**
     * Render month view
     */
    private void renderMonthView() {
        // Clear existing grid
        dayHeadersGrid.getChildren().clear();
        calendarGrid.getChildren().clear();
        
        // Add day headers
        for (int i = 0; i < 7; i++) {
            Label dayHeader = new Label(DAY_NAMES[i]);
            dayHeader.setStyle("-fx-font-weight: bold; -fx-alignment: center; -fx-padding: 5;");
            dayHeader.setMaxWidth(Double.MAX_VALUE);
            dayHeader.setAlignment(Pos.CENTER);
            dayHeadersGrid.add(dayHeader, i, 0);
        }
        
        // Calculate calendar grid
        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; // Sunday = 0
        int daysInMonth = currentYearMonth.lengthOfMonth();
        
        // Add day cells
        int row = 0;
        int col = firstDayOfWeek;
        
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            Pane dayCell = createMonthDayCell(date);
            calendarGrid.add(dayCell, col, row);
            
            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }
    
    /**
     * Create a day cell for month view
     */
    private Pane createMonthDayCell(LocalDate date) {
        VBox cell = new VBox(5);
        cell.getStyleClass().add("calendar-day-cell");
        
        // Highlight today
        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("calendar-day-cell-today");
        }
        
        // Day number
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.getStyleClass().add("calendar-day-number");
        cell.getChildren().add(dayLabel);
        
        // Task indicators
        long taskCount = currentTasks.stream()
            .filter(t -> date.equals(t.getScheduledDate()) || date.equals(t.getDueDate()))
            .count();
        
        if (taskCount > 0) {
            Label taskIndicator = new Label(taskCount + " task" + (taskCount > 1 ? "s" : ""));
            taskIndicator.getStyleClass().add("calendar-task-indicator");
            cell.getChildren().add(taskIndicator);
        }
        
        // Click handler to show tasks for day
        final LocalDate cellDate = date;
        cell.setOnMouseClicked(e -> handleDayClick(cellDate));
        
        // Drag-and-drop support
        setupDayDragAndDrop(cell, date);
        
        return cell;
    }
    
    /**
     * Setup drag-and-drop for a day cell
     */
    private void setupDayDragAndDrop(Pane cell, LocalDate date) {
        // Accept drops
        cell.setOnDragOver(event -> {
            if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        
        // Handle drop
        cell.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasString() && draggedTask != null) {
                // Update task scheduled date
                handleTaskRescheduled(draggedTask, date);
                success = true;
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }
    
    /**
     * Handle day cell click - show a dialog listing tasks scheduled/due on that day.
     */
    void handleDayClick(LocalDate date) {
        log.debug("Day clicked: {}", date);

        List<Task> tasksForDay;
        if (calendarFacade != null) {
            try {
                tasksForDay = calendarFacade.getTasksForDay(date);
            } catch (Exception e) {
                log.error("Failed to fetch tasks for day {}", date, e);
                tasksForDay = filterTasksForDay(date);
            }
        } else {
            tasksForDay = filterTasksForDay(date);
        }

        javafx.scene.control.Alert dialog = createDayTasksDialog(date, tasksForDay);
        dialog.showAndWait();
    }

    /**
     * Visible-for-testing factory for the day-tasks dialog.
     */
    javafx.scene.control.Alert createDayTasksDialog(LocalDate date, List<Task> tasksForDay) {
        javafx.scene.control.Alert dialog = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION);
        dialog.setTitle("Tasks");
        dialog.setHeaderText("Tasks on " + date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

        if (tasksForDay.isEmpty()) {
            dialog.setContentText("No tasks scheduled.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Task t : tasksForDay) {
                sb.append("• ").append(t.getTitle() == null ? "(untitled)" : t.getTitle());
                if (t.getDueTime() != null) {
                    sb.append("  @ ").append(t.getDueTime());
                }
                sb.append('\n');
            }
            dialog.setContentText(sb.toString());
        }
        return dialog;
    }

    private List<Task> filterTasksForDay(LocalDate date) {
        List<Task> result = new ArrayList<>();
        for (Task t : currentTasks) {
            if (date.equals(t.getScheduledDate()) || date.equals(t.getDueDate())) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Render week view with day columns and task chips.
     */
    private void renderWeekView() {
        weekGrid.getChildren().clear();
        weekGrid.getColumnConstraints().clear();

        LocalDate weekStart = currentDate.minusDays(currentDate.getDayOfWeek().getValue() % 7);

        // Day headers
        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = weekStart.plusDays(i);
            VBox header = new VBox(2);
            header.setAlignment(Pos.CENTER);
            Label nameLabel = new Label(dayDate.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            nameLabel.getStyleClass().add("calendar-day-number");
            Label dateLabel = new Label(String.valueOf(dayDate.getDayOfMonth()));
            dateLabel.getStyleClass().add("calendar-day-number");
            header.getChildren().addAll(nameLabel, dateLabel);
            weekGrid.add(header, i, 0);
        }

        // Day columns with tasks
        for (int i = 0; i < 7; i++) {
            final LocalDate dayDate = weekStart.plusDays(i);
            VBox column = new VBox(4);
            column.getStyleClass().add("calendar-day-cell");
            if (dayDate.equals(LocalDate.now())) {
                column.getStyleClass().add("calendar-day-cell-today");
            }

            List<Task> dayTasks = filterTasksForDay(dayDate);
            for (Task t : dayTasks) {
                Label chip = new Label(t.getTitle() == null ? "(untitled)" : t.getTitle());
                chip.getStyleClass().add("calendar-task-chip");
                chip.setMaxWidth(Double.MAX_VALUE);
                column.getChildren().add(chip);
            }

            column.setOnMouseClicked(e -> handleDayClick(dayDate));
            setupDayDragAndDrop(column, dayDate);
            weekGrid.add(column, i, 1);
        }

        log.debug("Week view rendered ({} - {})", weekStart, weekStart.plusDays(6));
    }

    /**
     * Render day view with hourly time slots.
     */
    private void renderDayView() {
        dayGrid.getChildren().clear();

        List<Task> tasksToday = filterTasksForDay(currentDate);

        // 24 hourly rows
        for (int hour = 0; hour < 24; hour++) {
            Label timeLabel = new Label(String.format("%02d:00", hour));
            timeLabel.getStyleClass().add("calendar-time-label");
            dayGrid.add(timeLabel, 0, hour);

            VBox slot = new VBox(2);
            slot.getStyleClass().add("calendar-time-slot");

            final int currentHour = hour;
            tasksToday.stream()
                .filter(t -> t.getDueTime() != null && t.getDueTime().getHour() == currentHour)
                .forEach(t -> {
                    Label chip = new Label((t.getDueTime() != null ? t.getDueTime() + "  " : "")
                        + (t.getTitle() == null ? "(untitled)" : t.getTitle()));
                    chip.getStyleClass().add("calendar-task-chip");
                    slot.getChildren().add(chip);
                });

            dayGrid.add(slot, 1, hour);
        }

        // Untimed tasks shown at the bottom
        List<Task> untimed = new ArrayList<>();
        for (Task t : tasksToday) {
            if (t.getDueTime() == null) {
                untimed.add(t);
            }
        }
        if (!untimed.isEmpty()) {
            Label allDayLabel = new Label("All-day");
            allDayLabel.getStyleClass().add("calendar-time-label");
            dayGrid.add(allDayLabel, 0, 24);

            VBox allDayBox = new VBox(2);
            allDayBox.getStyleClass().add("calendar-time-slot");
            for (Task t : untimed) {
                Label chip = new Label(t.getTitle() == null ? "(untitled)" : t.getTitle());
                chip.getStyleClass().add("calendar-task-chip");
                allDayBox.getChildren().add(chip);
            }
            dayGrid.add(allDayBox, 1, 24);
        }

        log.debug("Day view rendered for {} ({} tasks)", currentDate, tasksToday.size());
    }
    
    /**
     * Handle task rescheduled via drag and drop
     */
    private void handleTaskRescheduled(Task task, LocalDate newDate) {
        log.info("Rescheduling task {} to {}", task.getTitle(), newDate);
        
        if (taskFacade == null) {
            log.warn("TaskFacade not set, cannot reschedule task");
            return;
        }
        
        try {
            // Update task
            Task updatedTask = Task.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .status(task.getStatus())
                .dueDate(newDate)
                .dueTime(task.getDueTime())
                .scheduledDate(newDate)
                .tags(task.getTags())
                .categoryId(task.getCategoryId())
                .reminderMinutesBefore(task.getReminderMinutesBefore())
                .createdAt(task.getCreatedAt())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
            
            taskFacade.updateTask(updatedTask);
            
            // Publish event
            if (eventDispatcher != null) {
                eventDispatcher.publish(new TaskScheduledEvent(
                    updatedTask.getId(),
                    newDate
                ));
            }
            
            // Refresh view
            refreshCalendarView();
            
            log.info("Task rescheduled successfully");
        } catch (Exception e) {
            log.error("Failed to reschedule task", e);
        }
    }
    
    /**
     * Update task count label
     */
    private void updateTaskCount() {
        int count = currentTasks.size();
        taskCountLabel.setText(count + (count == 1 ? " task scheduled" : " tasks scheduled"));
    }
    
    /**
     * View mode enum
     */
    public enum ViewMode {
        MONTH("Month"),
        WEEK("Week"),
        DAY("Day");
        
        private final String displayName;
        
        ViewMode(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
}
