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
        cell.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0.5; -fx-padding: 5; " +
                     "-fx-min-width: 80; -fx-min-height: 80;");
        
        // Highlight today
        if (date.equals(LocalDate.now())) {
            cell.setStyle(cell.getStyle() + "-fx-background-color: #e3f2fd;");
        }
        
        // Day number
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setStyle("-fx-font-weight: bold;");
        cell.getChildren().add(dayLabel);
        
        // Task indicators
        long taskCount = currentTasks.stream()
            .filter(t -> date.equals(t.getScheduledDate()) || date.equals(t.getDueDate()))
            .count();
        
        if (taskCount > 0) {
            Label taskIndicator = new Label(taskCount + " task" + (taskCount > 1 ? "s" : ""));
            taskIndicator.setStyle("-fx-font-size: 10; -fx-text-fill: #1976d2;");
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
     * Handle day cell click
     */
    private void handleDayClick(LocalDate date) {
        log.debug("Day clicked: {}", date);
        // TODO: Show task list for the selected day in a side panel or dialog
    }
    
    /**
     * Render week view
     */
    private void renderWeekView() {
        // Clear existing grid
        weekGrid.getChildren().clear();
        
        // TODO: Implement week view with time slots
        Label placeholder = new Label("Week view coming soon");
        placeholder.setStyle("-fx-font-size: 16; -fx-text-fill: #666;");
        weekGrid.add(placeholder, 0, 0);
        
        log.debug("Week view rendered (placeholder)");
    }
    
    /**
     * Render day view
     */
    private void renderDayView() {
        // Clear existing grid
        dayGrid.getChildren().clear();
        
        // TODO: Implement day view with hourly time slots
        Label placeholder = new Label("Day view coming soon");
        placeholder.setStyle("-fx-font-size: 16; -fx-text-fill: #666;");
        dayGrid.add(placeholder, 0, 0);
        
        log.debug("Day view rendered (placeholder)");
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
