package net.talaatharb.workday.ui.controllers;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

/**
 * Controller for the Upcoming view showing future tasks grouped by date.
 */
@Slf4j
public class UpcomingViewController implements Initializable {
    
    @FXML
    private Button quickAddButton;
    
    @FXML
    private VBox contentContainer;
    
    @FXML
    private VBox emptyStateContainer;
    
    @FXML
    private VBox dateSectionsContainer;
    
    @FXML
    private Button emptyStateQuickAddButton;
    
    @Setter
    private TaskFacade taskFacade;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private static final int DEFAULT_DAYS_TO_SHOW = 14; // Show next 2 weeks by default
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing Upcoming View Controller");
        
        // Initially show empty state
        showEmptyState();
        
        log.info("Upcoming View Controller initialized successfully");
    }
    
    /**
     * Load tasks for upcoming view
     */
    public void loadTasks() {
        loadTasks(DEFAULT_DAYS_TO_SHOW);
    }
    
    /**
     * Load tasks for upcoming view with specified number of days
     */
    public void loadTasks(int daysAhead) {
        if (taskFacade == null) {
            log.warn("TaskFacade not set, cannot load upcoming tasks");
            showEmptyState();
            return;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead);
        
        // Get all tasks scheduled or due in the upcoming period
        List<Task> upcomingTasks = getAllUpcomingTasks(today, endDate);
        
        displayTasks(upcomingTasks);
    }
    
    /**
     * Get all upcoming tasks (excluding today)
     */
    private List<Task> getAllUpcomingTasks(LocalDate startDate, LocalDate endDate) {
        if (taskFacade == null) {
            return new ArrayList<>();
        }
        return taskFacade.getUpcomingTasks(startDate, endDate);
    }
    
    /**
     * Display tasks grouped by date
     */
    private void displayTasks(List<Task> allTasks) {
        log.debug("Displaying {} upcoming tasks", allTasks.size());
        
        // Filter out completed and cancelled tasks
        List<Task> activeTasks = allTasks.stream()
            .filter(task -> task.getStatus() != TaskStatus.COMPLETED 
                         && task.getStatus() != TaskStatus.CANCELLED)
            .collect(Collectors.toList());
        
        if (activeTasks.isEmpty()) {
            showEmptyState();
            return;
        }
        
        hideEmptyState();
        
        // Group tasks by date
        Map<LocalDate, List<Task>> tasksByDate = groupTasksByDate(activeTasks);
        
        // Display each date section
        dateSectionsContainer.getChildren().clear();
        for (Map.Entry<LocalDate, List<Task>> entry : tasksByDate.entrySet()) {
            VBox dateSection = createDateSection(entry.getKey(), entry.getValue());
            dateSectionsContainer.getChildren().add(dateSection);
        }
    }
    
    /**
     * Group tasks by their date (scheduled or due date)
     */
    private Map<LocalDate, List<Task>> groupTasksByDate(List<Task> tasks) {
        Map<LocalDate, List<Task>> grouped = new LinkedHashMap<>();
        
        for (Task task : tasks) {
            LocalDate date = getTaskDate(task);
            if (date != null) {
                grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(task);
            }
        }
        
        // Sort dates
        return grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }
    
    /**
     * Get the relevant date for a task (scheduled date or due date)
     */
    private LocalDate getTaskDate(Task task) {
        if (task.getScheduledDate() != null) {
            return task.getScheduledDate();
        }
        return task.getDueDate();
    }
    
    /**
     * Create a date section with header and tasks
     */
    private VBox createDateSection(LocalDate date, List<Task> tasks) {
        VBox section = new VBox(10);
        section.getStyleClass().add("date-section");
        
        // Date header with relative label
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getStyleClass().add("date-section-header");
        
        Label dateLabel = new Label(date.format(DATE_FORMATTER));
        dateLabel.setFont(Font.font("System Bold", 16));
        
        Label relativeLabel = new Label(getRelativeDateLabel(date));
        relativeLabel.setFont(Font.font(12));
        relativeLabel.getStyleClass().add("tag-label");
        
        Label countLabel = new Label(tasks.size() + (tasks.size() == 1 ? " task" : " tasks"));
        countLabel.setFont(Font.font(12));
        countLabel.setStyle("-fx-text-fill: #6c757d;");
        
        headerBox.getChildren().addAll(dateLabel, relativeLabel, countLabel);
        
        // Tasks container
        VBox tasksContainer = new VBox(8);
        for (Task task : tasks) {
            VBox taskCard = createTaskCard(task);
            tasksContainer.getChildren().add(taskCard);
        }
        
        section.getChildren().addAll(headerBox, tasksContainer);
        return section;
    }
    
    /**
     * Get relative date label (Tomorrow, This Week, Next Week, etc.)
     */
    private String getRelativeDateLabel(LocalDate date) {
        LocalDate today = LocalDate.now();
        long daysUntil = ChronoUnit.DAYS.between(today, date);
        
        if (daysUntil == 1) {
            return "Tomorrow";
        } else if (daysUntil >= 2 && daysUntil <= 7) {
            return "This Week";
        } else if (daysUntil >= 8 && daysUntil <= 14) {
            return "Next Week";
        } else if (daysUntil >= 15 && daysUntil <= 30) {
            return "This Month";
        } else {
            return "Later";
        }
    }
    
    /**
     * Create a task card UI element
     */
    private VBox createTaskCard(Task task) {
        VBox card = new VBox(5);
        card.getStyleClass().add("task-card");
        
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
        dateSectionsContainer.setVisible(false);
        dateSectionsContainer.setManaged(false);
    }
    
    /**
     * Hide empty state
     */
    private void hideEmptyState() {
        emptyStateContainer.setVisible(false);
        emptyStateContainer.setManaged(false);
        dateSectionsContainer.setVisible(true);
        dateSectionsContainer.setManaged(true);
    }
    
    @FXML
    private void handleQuickAdd() {
        log.info("Quick add button clicked");
        // TODO: Integrate with quick add functionality
    }
}
