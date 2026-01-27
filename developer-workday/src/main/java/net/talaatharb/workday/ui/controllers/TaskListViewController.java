package net.talaatharb.workday.ui.controllers;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

/**
 * Controller for the task list view.
 * Handles displaying, filtering, and sorting tasks.
 */
@Slf4j
public class TaskListViewController implements Initializable {

    @FXML
    private Label viewTitleLabel;
    
    @FXML
    private Button newTaskButton;
    
    @FXML
    private ChoiceBox<String> categoryFilterChoice;
    
    @FXML
    private ChoiceBox<String> priorityFilterChoice;
    
    @FXML
    private ChoiceBox<String> statusFilterChoice;
    
    @FXML
    private ChoiceBox<String> sortChoice;
    
    @FXML
    private Button clearFiltersButton;
    
    @FXML
    private ListView<Task> taskListView;
    
    @FXML
    private Label taskCountLabel;
    
    @FXML
    private CheckBox showCompletedCheck;
    
    private List<Task> allTasks = new ArrayList<>();
    private List<Task> filteredTasks = new ArrayList<>();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing Task List View Controller");
        
        // Setup category filter
        categoryFilterChoice.setItems(FXCollections.observableArrayList(
            "All Categories", "Work", "Personal", "Shopping", "Health"
        ));
        categoryFilterChoice.setValue("All Categories");
        categoryFilterChoice.setOnAction(e -> applyFilters());
        
        // Setup priority filter
        priorityFilterChoice.setItems(FXCollections.observableArrayList(
            "All Priorities", "Urgent", "High", "Medium", "Low"
        ));
        priorityFilterChoice.setValue("All Priorities");
        priorityFilterChoice.setOnAction(e -> applyFilters());
        
        // Setup status filter
        statusFilterChoice.setItems(FXCollections.observableArrayList(
            "All Statuses", "TODO", "In Progress", "Completed", "Cancelled"
        ));
        statusFilterChoice.setValue("All Statuses");
        statusFilterChoice.setOnAction(e -> applyFilters());
        
        // Setup sort options
        sortChoice.setItems(FXCollections.observableArrayList(
            "Due Date", "Priority", "Name", "Created Date"
        ));
        sortChoice.setValue("Due Date");
        sortChoice.setOnAction(e -> applySorting());
        
        // Setup task list custom cell factory
        taskListView.setCellFactory(listView -> new TaskCell());
        
        // Load sample tasks for demonstration
        loadSampleTasks();
        applyFilters();
        
        log.info("Task List View Controller initialized successfully");
    }
    
    @FXML
    private void handleNewTask() {
        log.info("New task button clicked");
        // TODO: Open new task dialog
    }
    
    @FXML
    private void handleClearFilters() {
        log.info("Clearing filters");
        categoryFilterChoice.setValue("All Categories");
        priorityFilterChoice.setValue("All Priorities");
        statusFilterChoice.setValue("All Statuses");
        showCompletedCheck.setSelected(true);
        applyFilters();
    }
    
    @FXML
    private void handleToggleShowCompleted() {
        log.info("Toggle show completed: {}", showCompletedCheck.isSelected());
        applyFilters();
    }
    
    /**
     * Apply filters to the task list
     */
    private void applyFilters() {
        log.debug("Applying filters");
        
        filteredTasks = allTasks.stream()
            .filter(task -> filterByCategory(task))
            .filter(task -> filterByPriority(task))
            .filter(task -> filterByStatus(task))
            .filter(task -> filterByCompleted(task))
            .collect(Collectors.toList());
        
        applySorting();
    }
    
    private boolean filterByCategory(Task task) {
        String selected = categoryFilterChoice.getValue();
        if (selected == null || "All Categories".equals(selected)) {
            return true;
        }
        // TODO: Implement actual category filtering with CategoryFacade
        return true;
    }
    
    private boolean filterByPriority(Task task) {
        String selected = priorityFilterChoice.getValue();
        if (selected == null || "All Priorities".equals(selected)) {
            return true;
        }
        Priority priority = Priority.valueOf(selected.toUpperCase().replace(" ", "_"));
        return task.getPriority() == priority;
    }
    
    private boolean filterByStatus(Task task) {
        String selected = statusFilterChoice.getValue();
        if (selected == null || "All Statuses".equals(selected)) {
            return true;
        }
        String statusValue = selected.toUpperCase().replace(" ", "_");
        TaskStatus status = TaskStatus.valueOf(statusValue);
        return task.getStatus() == status;
    }
    
    private boolean filterByCompleted(Task task) {
        if (showCompletedCheck.isSelected()) {
            return true;
        }
        return task.getStatus() != TaskStatus.COMPLETED;
    }
    
    /**
     * Apply sorting to the filtered task list
     */
    private void applySorting() {
        log.debug("Applying sorting");
        
        String sortBy = sortChoice.getValue();
        Comparator<Task> comparator;
        
        switch (sortBy) {
            case "Priority":
                comparator = Comparator.comparing(Task::getPriority, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "Name":
                comparator = Comparator.comparing(Task::getTitle, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "Created Date":
                comparator = Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "Due Date":
            default:
                comparator = Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
        }
        
        filteredTasks.sort(comparator);
        refreshTaskList();
    }
    
    /**
     * Refresh the task list view
     */
    private void refreshTaskList() {
        taskListView.setItems(FXCollections.observableArrayList(filteredTasks));
        updateTaskCount();
    }
    
    /**
     * Update the task count label
     */
    private void updateTaskCount() {
        int count = filteredTasks.size();
        taskCountLabel.setText(count + (count == 1 ? " task" : " tasks"));
    }
    
    /**
     * Load tasks into the view
     */
    public void loadTasks(List<Task> tasks) {
        this.allTasks = new ArrayList<>(tasks);
        applyFilters();
    }
    
    /**
     * Load sample tasks for demonstration
     */
    private void loadSampleTasks() {
        allTasks = new ArrayList<>();
        
        // Sample task 1: Overdue high priority
        Task task1 = Task.builder()
            .title("Fix critical bug in production")
            .status(TaskStatus.IN_PROGRESS)
            .priority(Priority.URGENT)
            .dueDate(java.time.LocalDate.now().minusDays(1))
            .build();
        
        // Sample task 2: Today medium priority
        Task task2 = Task.builder()
            .title("Review pull requests")
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .dueDate(java.time.LocalDate.now())
            .build();
        
        // Sample task 3: Completed task
        Task task3 = Task.builder()
            .title("Write unit tests")
            .status(TaskStatus.COMPLETED)
            .priority(Priority.HIGH)
            .dueDate(java.time.LocalDate.now().minusDays(2))
            .build();
        
        // Sample task 4: Future low priority
        Task task4 = Task.builder()
            .title("Update documentation")
            .status(TaskStatus.TODO)
            .priority(Priority.LOW)
            .dueDate(java.time.LocalDate.now().plusDays(3))
            .build();
        
        // Sample task 5: No due date
        Task task5 = Task.builder()
            .title("Brainstorm new features")
            .status(TaskStatus.TODO)
            .priority(Priority.LOW)
            .build();
        
        allTasks.add(task1);
        allTasks.add(task2);
        allTasks.add(task3);
        allTasks.add(task4);
        allTasks.add(task5);
    }
    
    /**
     * Custom cell for task list items
     */
    private static class TaskCell extends ListCell<Task> {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");
        
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            
            if (empty || task == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                VBox container = new VBox(5);
                container.setStyle("-fx-padding: 5;");
                
                // Top row: title and priority
                HBox topRow = new HBox(10);
                topRow.setAlignment(Pos.CENTER_LEFT);
                
                // Task title
                Label titleLabel = new Label(task.getTitle());
                titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                
                // Apply completed style
                if (task.getStatus() == TaskStatus.COMPLETED) {
                    titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " +
                                      "-fx-text-fill: #95a5a6; -fx-strikethrough: true;");
                }
                
                // Priority indicator
                Label priorityLabel = new Label(getPrioritySymbol(task.getPriority()));
                priorityLabel.setStyle("-fx-font-size: 16px;");
                
                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                
                topRow.getChildren().addAll(priorityLabel, titleLabel, spacer);
                
                // Bottom row: due date, status, category
                HBox bottomRow = new HBox(15);
                bottomRow.setAlignment(Pos.CENTER_LEFT);
                
                // Due date
                if (task.getDueDate() != null) {
                    Label dueDateLabel = new Label("📅 " + task.getDueDate().format(DATE_FORMATTER));
                    dueDateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
                    
                    // Highlight overdue tasks
                    if (task.getDueDate().isBefore(java.time.LocalDate.now()) 
                        && task.getStatus() != TaskStatus.COMPLETED) {
                        dueDateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                    
                    bottomRow.getChildren().add(dueDateLabel);
                }
                
                // Status badge
                Label statusLabel = new Label(task.getStatus().toString());
                statusLabel.setStyle(getStatusStyle(task.getStatus()));
                bottomRow.getChildren().add(statusLabel);
                
                container.getChildren().addAll(topRow, bottomRow);
                setGraphic(container);
                
                // Highlight overdue tasks
                if (task.getDueDate() != null && task.getDueDate().isBefore(java.time.LocalDate.now()) 
                    && task.getStatus() != TaskStatus.COMPLETED) {
                    setStyle("-fx-background-color: #ffe6e6;");
                } else if (task.getStatus() == TaskStatus.COMPLETED) {
                    setStyle("-fx-background-color: #f0f0f0;");
                } else {
                    setStyle("");
                }
            }
        }
        
        private String getPrioritySymbol(Priority priority) {
            if (priority == null) return "⚪";
            return switch (priority) {
                case URGENT -> "🔴";
                case HIGH -> "🟠";
                case MEDIUM -> "🟡";
                case LOW -> "🟢";
            };
        }
        
        private String getStatusStyle(TaskStatus status) {
            String baseStyle = "-fx-font-size: 10px; -fx-padding: 3 8 3 8; " +
                             "-fx-background-radius: 3; -fx-font-weight: bold;";
            return switch (status) {
                case TODO -> baseStyle + "-fx-background-color: #3498db; -fx-text-fill: white;";
                case IN_PROGRESS -> baseStyle + "-fx-background-color: #f39c12; -fx-text-fill: white;";
                case COMPLETED -> baseStyle + "-fx-background-color: #2ecc71; -fx-text-fill: white;";
                case CANCELLED -> baseStyle + "-fx-background-color: #95a5a6; -fx-text-fill: white;";
            };
        }
    }
}
