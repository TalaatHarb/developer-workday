package net.talaatharb.workday.ui.controllers;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

/**
 * Controller for the Today view showing overdue and today's tasks with smart grouping.
 */
@Slf4j
public class TodayViewController implements Initializable {
    
    @FXML
    private Button quickAddButton;
    
    @FXML
    private VBox contentContainer;
    
    @FXML
    private VBox emptyStateContainer;
    
    @FXML
    private VBox overdueSection;
    
    @FXML
    private Label overdueSectionToggle;
    
    @FXML
    private Label overdueCount;
    
    @FXML
    private VBox overdueTasksContainer;
    
    @FXML
    private VBox todaySection;
    
    @FXML
    private Label todaySectionToggle;
    
    @FXML
    private Label todayCount;
    
    @FXML
    private VBox todayTasksContainer;
    
    @FXML
    private Button emptyStateQuickAddButton;
    
    @Setter
    private TaskFacade taskFacade;
    
    private boolean overdueSectionCollapsed = false;
    private boolean todaySectionCollapsed = false;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    
    // Time block definitions
    private static final LocalTime MORNING_START = LocalTime.of(0, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(12, 0);
    private static final LocalTime EVENING_START = LocalTime.of(17, 0);
    private static final LocalTime DAY_END = LocalTime.of(23, 59, 59);
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing Today View Controller");
        
        // Initially show empty state
        showEmptyState();
        
        log.info("Today View Controller initialized successfully");
    }
    
    /**
     * Load tasks for today view
     */
    public void loadTasks() {
        if (taskFacade == null) {
            log.warn("TaskFacade not set, cannot load tasks for today");
            showEmptyState();
            return;
        }
        
        List<Task> tasksForToday = taskFacade.getTasksForToday();
        displayTasks(tasksForToday);
    }
    
    /**
     * Display tasks in the view with smart grouping
     */
    private void displayTasks(List<Task> allTasks) {
        log.debug("Displaying {} tasks for today", allTasks.size());
        
        // Filter out completed and cancelled tasks
        List<Task> activeTasks = allTasks.stream()
            .filter(task -> task.getStatus() != TaskStatus.COMPLETED 
                         && task.getStatus() != TaskStatus.CANCELLED)
            .collect(Collectors.toList());
        
        // Separate overdue and today's tasks
        LocalDate today = LocalDate.now();
        List<Task> overdueTasks = activeTasks.stream()
            .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(today))
            .collect(Collectors.toList());
        
        List<Task> todaysTasks = activeTasks.stream()
            .filter(task -> (task.getScheduledDate() != null && task.getScheduledDate().isEqual(today))
                         || (task.getDueDate() != null && task.getDueDate().isEqual(today) 
                             && (task.getScheduledDate() == null || !task.getScheduledDate().isBefore(today))))
            .collect(Collectors.toList());
        
        // Show/hide sections based on content
        if (overdueTasks.isEmpty() && todaysTasks.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            displayOverdueSection(overdueTasks);
            displayTodaySection(todaysTasks);
        }
    }
    
    /**
     * Display overdue tasks section
     */
    private void displayOverdueSection(List<Task> overdueTasks) {
        overdueTasksContainer.getChildren().clear();
        
        if (overdueTasks.isEmpty()) {
            overdueSection.setVisible(false);
            overdueSection.setManaged(false);
            return;
        }
        
        overdueSection.setVisible(true);
        overdueSection.setManaged(true);
        overdueCount.setText(String.valueOf(overdueTasks.size()));
        
        for (Task task : overdueTasks) {
            VBox taskCard = createTaskCard(task);
            overdueTasksContainer.getChildren().add(taskCard);
        }
    }
    
    /**
     * Display today's tasks section with time block grouping
     */
    private void displayTodaySection(List<Task> todaysTasks) {
        todayTasksContainer.getChildren().clear();
        
        if (todaysTasks.isEmpty()) {
            todaySection.setVisible(false);
            todaySection.setManaged(false);
            return;
        }
        
        todaySection.setVisible(true);
        todaySection.setManaged(true);
        todayCount.setText(String.valueOf(todaysTasks.size()));
        
        // Group tasks by time blocks
        Map<TimeBlock, List<Task>> groupedTasks = groupTasksByTimeBlock(todaysTasks);
        
        // Display each time block
        for (TimeBlock timeBlock : TimeBlock.values()) {
            List<Task> tasks = groupedTasks.get(timeBlock);
            if (tasks != null && !tasks.isEmpty()) {
                VBox timeBlockContainer = createTimeBlockContainer(timeBlock, tasks);
                todayTasksContainer.getChildren().add(timeBlockContainer);
            }
        }
    }
    
    /**
     * Group tasks by time blocks (Morning, Afternoon, Evening)
     */
    private Map<TimeBlock, List<Task>> groupTasksByTimeBlock(List<Task> tasks) {
        Map<TimeBlock, List<Task>> grouped = new HashMap<>();
        
        for (Task task : tasks) {
            TimeBlock block = determineTimeBlock(task);
            grouped.computeIfAbsent(block, k -> new ArrayList<>()).add(task);
        }
        
        return grouped;
    }
    
    /**
     * Determine which time block a task belongs to
     */
    private TimeBlock determineTimeBlock(Task task) {
        LocalTime time = task.getDueTime();
        if (time == null) {
            // Tasks without specific time go to morning by default
            return TimeBlock.MORNING;
        }
        
        if (time.isBefore(AFTERNOON_START)) {
            return TimeBlock.MORNING;
        } else if (time.isBefore(EVENING_START)) {
            return TimeBlock.AFTERNOON;
        } else {
            return TimeBlock.EVENING;
        }
    }
    
    /**
     * Create a time block container with header and tasks
     */
    private VBox createTimeBlockContainer(TimeBlock timeBlock, List<Task> tasks) {
        VBox container = new VBox(10);
        container.getStyleClass().add("time-block-container");
        
        // Time block header
        Label headerLabel = new Label(timeBlock.getLabel());
        headerLabel.getStyleClass().add("time-block-header");
        headerLabel.setFont(Font.font("System Bold", 14));
        headerLabel.setStyle("-fx-text-fill: #7f8c8d;");
        
        container.getChildren().add(headerLabel);
        
        // Add tasks
        for (Task task : tasks) {
            VBox taskCard = createTaskCard(task);
            container.getChildren().add(taskCard);
        }
        
        return container;
    }
    
    /**
     * Create a task card UI element
     */
    private VBox createTaskCard(Task task) {
        VBox card = new VBox(5);
        card.getStyleClass().add("task-card");
        
        // Add overdue style if applicable
        if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
            card.getStyleClass().add("task-card-overdue");
        }
        
        // Top row: priority and title
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        Label priorityLabel = new Label(getPrioritySymbol(task.getPriority()));
        priorityLabel.setFont(Font.font(16));
        
        Label titleLabel = new Label(task.getTitle());
        titleLabel.setFont(Font.font("System Bold", 14));
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);
        
        topRow.getChildren().addAll(priorityLabel, titleLabel);
        
        // Bottom row: time, status, tags
        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        
        if (task.getDueTime() != null) {
            Label timeLabel = new Label("🕐 " + task.getDueTime().format(TIME_FORMATTER));
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
            bottomRow.getChildren().add(timeLabel);
        }
        
        Label statusLabel = new Label(task.getStatus().toString());
        statusLabel.getStyleClass().add(getStatusStyleClass(task.getStatus()));
        bottomRow.getChildren().add(statusLabel);
        
        if (task.getTags() != null && !task.getTags().isEmpty()) {
            for (String tag : task.getTags()) {
                Label tagLabel = new Label("#" + tag);
                tagLabel.getStyleClass().add("tag-label");
                bottomRow.getChildren().add(tagLabel);
            }
        }
        
        card.getChildren().addAll(topRow, bottomRow);
        
        // Add click handler for task details
        card.setOnMouseClicked(event -> handleTaskClick(task));
        card.setCursor(javafx.scene.Cursor.HAND);
        
        return card;
    }
    
    /**
     * Get priority symbol emoji
     */
    private String getPrioritySymbol(Priority priority) {
        if (priority == null) return "⚪";
        return switch (priority) {
            case URGENT -> "🔴";
            case HIGH -> "🟠";
            case MEDIUM -> "🟡";
            case LOW -> "🟢";
        };
    }
    
    /**
     * Get status CSS style class
     */
    private String getStatusStyleClass(TaskStatus status) {
        return switch (status) {
            case TODO -> "status-badge-todo";
            case IN_PROGRESS -> "status-badge-in-progress";
            case COMPLETED -> "status-badge-completed";
            case CANCELLED -> "status-badge-cancelled";
        };
    }
    
    /**
     * Handle task card click
     */
    private void handleTaskClick(Task task) {
        log.info("Task clicked: {}", task.getTitle());
        // TODO: Open task detail dialog
    }
    
    /**
     * Show empty state
     */
    private void showEmptyState() {
        emptyStateContainer.setVisible(true);
        emptyStateContainer.setManaged(true);
        overdueSection.setVisible(false);
        overdueSection.setManaged(false);
        todaySection.setVisible(false);
        todaySection.setManaged(false);
    }
    
    /**
     * Hide empty state
     */
    private void hideEmptyState() {
        emptyStateContainer.setVisible(false);
        emptyStateContainer.setManaged(false);
    }
    
    @FXML
    private void handleQuickAdd() {
        log.info("Quick add button clicked");
        // TODO: Integrate with quick add functionality
    }
    
    @FXML
    private void handleToggleOverdueSection() {
        overdueSectionCollapsed = !overdueSectionCollapsed;
        overdueTasksContainer.setVisible(!overdueSectionCollapsed);
        overdueTasksContainer.setManaged(!overdueSectionCollapsed);
        overdueSectionToggle.setText(overdueSectionCollapsed ? "▶" : "▼");
        log.debug("Overdue section collapsed: {}", overdueSectionCollapsed);
    }
    
    @FXML
    private void handleToggleTodaySection() {
        todaySectionCollapsed = !todaySectionCollapsed;
        todayTasksContainer.setVisible(!todaySectionCollapsed);
        todayTasksContainer.setManaged(!todaySectionCollapsed);
        todaySectionToggle.setText(todaySectionCollapsed ? "▶" : "▼");
        log.debug("Today section collapsed: {}", todaySectionCollapsed);
    }
    
    /**
     * Time block enumeration
     */
    private enum TimeBlock {
        MORNING("🌅 Morning"),
        AFTERNOON("☀️ Afternoon"),
        EVENING("🌙 Evening");
        
        private final String label;
        
        TimeBlock(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
    }
}
