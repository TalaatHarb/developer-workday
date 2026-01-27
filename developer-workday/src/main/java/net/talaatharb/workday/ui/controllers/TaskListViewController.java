package net.talaatharb.workday.ui.controllers;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.utils.AnimationHelper;
import net.talaatharb.workday.utils.ContextMenuHelper;

/**
 * Controller for the task list view.
 * Handles displaying, filtering, sorting, searching, and keyboard navigation for tasks.
 */
@Slf4j
public class TaskListViewController implements Initializable {

    @FXML
    private Label viewTitleLabel;
    
    @FXML
    private Button newTaskButton;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Button clearSearchButton;
    
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
    private String currentSearchKeyword = "";
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing Task List View Controller");
        
        // Setup search field listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            currentSearchKeyword = newValue;
            applyFilters();
        });
        
        // Setup keyboard shortcuts for search field
        searchField.setOnKeyPressed(this::handleSearchFieldKeyPress);
        
        // Setup keyboard navigation for task list
        taskListView.setOnKeyPressed(this::handleTaskListKeyPress);
        
        // Enable multiple selection for task list
        taskListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
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
    private void handleClearSearch() {
        log.info("Clearing search");
        searchField.clear();
        currentSearchKeyword = "";
        applyFilters();
    }
    
    /**
     * Handle keyboard navigation in task list
     */
    private void handleTaskListKeyPress(KeyEvent event) {
        Task selectedTask = taskListView.getSelectionModel().getSelectedItem();
        
        if (selectedTask == null) {
            return;
        }
        
        switch (event.getCode()) {
            case ENTER:
                log.info("Opening task details for: {}", selectedTask.getTitle());
                handleOpenTaskDetails(selectedTask);
                event.consume();
                break;
                
            case DELETE:
                log.info("Delete key pressed for task: {}", selectedTask.getTitle());
                handleDeleteTask(selectedTask);
                event.consume();
                break;
                
            case SPACE:
                log.info("Space key pressed for task: {}", selectedTask.getTitle());
                handleToggleTaskComplete(selectedTask);
                event.consume();
                break;
                
            default:
                // Let other keys pass through for default list navigation
                break;
        }
    }
    
    /**
     * Handle keyboard shortcuts in search field
     */
    private void handleSearchFieldKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            log.info("Escape pressed in search field");
            handleClearSearch();
            taskListView.requestFocus();
            event.consume();
        }
    }
    
    /**
     * Open task details dialog
     */
    private void handleOpenTaskDetails(Task task) {
        // TODO: Implement task details dialog
        log.info("Would open details for task: {}", task.getTitle());
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Task Details");
        alert.setHeaderText(task.getTitle());
        alert.setContentText("Task ID: " + task.getId() + "\n" +
                           "Status: " + task.getStatus() + "\n" +
                           "Priority: " + task.getPriority() + "\n" +
                           "Description: " + (task.getDescription() != null ? task.getDescription() : "No description"));
        alert.showAndWait();
    }
    
    /**
     * Delete task with confirmation
     */
    private void handleDeleteTask(Task task) {
        log.info("Attempting to delete task: {}", task.getTitle());
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Task");
        confirmAlert.setHeaderText("Delete task: " + task.getTitle() + "?");
        confirmAlert.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            allTasks.remove(task);
            applyFilters();
            log.info("Task deleted: {}", task.getTitle());
        }
    }
    
    /**
     * Toggle task completion status
     */
    private void handleToggleTaskComplete(Task task) {
        log.info("Toggling completion for task: {}", task.getTitle());
        
        if (task.getStatus() == TaskStatus.COMPLETED) {
            task.setStatus(TaskStatus.TODO);
            log.info("Task marked as TODO: {}", task.getTitle());
        } else {
            task.setStatus(TaskStatus.COMPLETED);
            log.info("Task marked as COMPLETED: {}", task.getTitle());
        }
        
        applyFilters();
    }
    
    /**
     * Handle task edit action
     */
    private void handleEditTask(Task task) {
        log.info("Editing task: {}", task.getTitle());
        handleOpenTaskDetails(task);
    }
    
    /**
     * Handle task schedule action
     */
    private void handleScheduleTask(Task task) {
        log.info("Scheduling task: {}", task.getTitle());
        // TODO: Open date/time picker dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Schedule Task");
        alert.setHeaderText("Schedule: " + task.getTitle());
        alert.setContentText("Date/time picker dialog will be implemented here.");
        alert.showAndWait();
    }
    
    /**
     * Handle move task to category action
     */
    private void handleMoveToCategory(Task task) {
        log.info("Moving task to category: {}", task.getTitle());
        // TODO: Show category selection dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Move to Category");
        alert.setHeaderText("Move: " + task.getTitle());
        alert.setContentText("Category selection dialog will be implemented here.");
        alert.showAndWait();
    }
    
    /**
     * Handle duplicate task action
     */
    private void handleDuplicateTask(Task task) {
        log.info("Duplicating task: {}", task.getTitle());
        
        Task duplicatedTask = Task.builder()
            .title(task.getTitle() + " (Copy)")
            .description(task.getDescription())
            .status(TaskStatus.TODO)
            .priority(task.getPriority())
            .dueDate(task.getDueDate())
            .dueTime(task.getDueTime())
            .tags(task.getTags() != null ? new ArrayList<>(task.getTags()) : null)
            .build();
        
        allTasks.add(duplicatedTask);
        applyFilters();
        
        log.info("Task duplicated: {}", duplicatedTask.getTitle());
    }
    
    /**
     * Handle bulk complete all tasks
     */
    private void handleCompleteAllTasks(List<Task> tasks) {
        log.info("Completing {} tasks", tasks.size());
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Complete All Tasks");
        confirmAlert.setHeaderText(String.format("Complete %d tasks?", tasks.size()));
        confirmAlert.setContentText("This will mark all selected tasks as completed.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            for (Task task : tasks) {
                task.setStatus(TaskStatus.COMPLETED);
            }
            applyFilters();
            log.info("{} tasks marked as completed", tasks.size());
        }
    }
    
    /**
     * Handle bulk move all tasks to category
     */
    private void handleMoveAllTasks(List<Task> tasks) {
        log.info("Moving {} tasks to category", tasks.size());
        // TODO: Show category selection dialog for bulk move
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Move All Tasks");
        alert.setHeaderText(String.format("Move %d tasks", tasks.size()));
        alert.setContentText("Category selection dialog will be implemented here.");
        alert.showAndWait();
    }
    
    /**
     * Handle bulk delete all tasks
     */
    private void handleDeleteAllTasks(List<Task> tasks) {
        log.info("Attempting to delete {} tasks", tasks.size());
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete All Tasks");
        confirmAlert.setHeaderText(String.format("Delete %d tasks?", tasks.size()));
        confirmAlert.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            allTasks.removeAll(tasks);
            applyFilters();
            log.info("{} tasks deleted", tasks.size());
        }
    }
    
    /**
     * Focus the search field (for Ctrl+F shortcut)
     */
    public void focusSearch() {
        searchField.requestFocus();
    }
    
    /**
     * Focus the task list
     */
    public void focusTaskList() {
        taskListView.requestFocus();
        if (taskListView.getItems().isEmpty()) {
            return;
        }
        if (taskListView.getSelectionModel().getSelectedIndex() < 0) {
            taskListView.getSelectionModel().selectFirst();
        }
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
        log.debug("Applying filters with search keyword: {}", currentSearchKeyword);
        
        filteredTasks = allTasks.stream()
            .filter(task -> filterBySearch(task))
            .filter(task -> filterByCategory(task))
            .filter(task -> filterByPriority(task))
            .filter(task -> filterByStatus(task))
            .filter(task -> filterByCompleted(task))
            .collect(Collectors.toList());
        
        applySorting();
    }
    
    private boolean filterBySearch(Task task) {
        if (currentSearchKeyword == null || currentSearchKeyword.trim().isEmpty()) {
            return true;
        }
        
        String lowerKeyword = currentSearchKeyword.toLowerCase().trim();
        
        // Search in title
        if (task.getTitle() != null && task.getTitle().toLowerCase().contains(lowerKeyword)) {
            return true;
        }
        
        // Search in description
        if (task.getDescription() != null && task.getDescription().toLowerCase().contains(lowerKeyword)) {
            return true;
        }
        
        // Search in tags
        if (task.getTags() != null) {
            for (String tag : task.getTags()) {
                if (tag != null && tag.toLowerCase().contains(lowerKeyword)) {
                    return true;
                }
            }
        }
        
        return false;
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
            .description("Memory leak causing server crashes")
            .status(TaskStatus.IN_PROGRESS)
            .priority(Priority.URGENT)
            .dueDate(java.time.LocalDate.now().minusDays(1))
            .tags(List.of("bug", "production", "urgent"))
            .build();
        
        // Sample task 2: Today medium priority
        Task task2 = Task.builder()
            .title("Review pull requests")
            .description("Check code quality and test coverage")
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .dueDate(java.time.LocalDate.now())
            .tags(List.of("code-review", "team"))
            .build();
        
        // Sample task 3: Completed task
        Task task3 = Task.builder()
            .title("Write unit tests")
            .description("Add tests for new search functionality")
            .status(TaskStatus.COMPLETED)
            .priority(Priority.HIGH)
            .dueDate(java.time.LocalDate.now().minusDays(2))
            .tags(List.of("testing", "development"))
            .build();
        
        // Sample task 4: Future low priority
        Task task4 = Task.builder()
            .title("Update documentation")
            .description("Add API documentation for new endpoints")
            .status(TaskStatus.TODO)
            .priority(Priority.LOW)
            .dueDate(java.time.LocalDate.now().plusDays(3))
            .tags(List.of("documentation", "api"))
            .build();
        
        // Sample task 5: No due date
        Task task5 = Task.builder()
            .title("Brainstorm new features")
            .description("Research competitor features and user feedback")
            .status(TaskStatus.TODO)
            .priority(Priority.LOW)
            .tags(List.of("planning", "research"))
            .build();
        
        allTasks.add(task1);
        allTasks.add(task2);
        allTasks.add(task3);
        allTasks.add(task4);
        allTasks.add(task5);
    }
    
    /**
     * Show context menu for task based on selection state
     */
    private void showContextMenuForTask(Task task, TaskCell cell) {
        List<Task> selectedTasks = new ArrayList<>(taskListView.getSelectionModel().getSelectedItems());
        
        ContextMenu contextMenu;
        if (selectedTasks.size() > 1) {
            // Multiple selection - show bulk actions menu
            contextMenu = ContextMenuHelper.createBulkTaskContextMenu(
                selectedTasks,
                () -> handleCompleteAllTasks(selectedTasks),
                () -> handleMoveAllTasks(selectedTasks),
                () -> handleDeleteAllTasks(selectedTasks)
            );
        } else {
            // Single selection - show standard task menu
            contextMenu = ContextMenuHelper.createTaskContextMenu(
                task,
                () -> handleEditTask(task),
                () -> handleToggleTaskComplete(task),
                () -> handleScheduleTask(task),
                () -> handleMoveToCategory(task),
                () -> handleDuplicateTask(task),
                () -> handleDeleteTask(task)
            );
        }
        
        contextMenu.show(cell, cell.getLayoutX(), cell.getLayoutY());
    }
    
    /**
     * Custom cell for task list items
     */
    private class TaskCell extends ListCell<Task> {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");
        private boolean firstRender = true;
        
        public TaskCell() {
            super();
            
            // Add context menu on right-click
            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !isEmpty()) {
                    Task task = getItem();
                    if (task != null) {
                        showContextMenuForTask(task, this);
                    }
                }
            });
        }
        
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
                
                // Priority indicator
                Label priorityLabel = new Label(getPrioritySymbol(task.getPriority()));
                priorityLabel.setStyle("-fx-font-size: 16px;");
                
                // Task title with highlighting
                Region titleRegion;
                if (currentSearchKeyword != null && !currentSearchKeyword.trim().isEmpty()) {
                    titleRegion = createHighlightedText(task.getTitle(), currentSearchKeyword);
                } else {
                    Label titleLabel = new Label(task.getTitle());
                    titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                    
                    // Apply completed style
                    if (task.getStatus() == TaskStatus.COMPLETED) {
                        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " +
                                          "-fx-text-fill: #95a5a6; -fx-strikethrough: true;");
                    }
                    titleRegion = titleLabel;
                }
                
                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                
                topRow.getChildren().addAll(priorityLabel, titleRegion, spacer);
                
                // Bottom row: due date, status, tags
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
                
                // Tags with highlighting
                if (task.getTags() != null && !task.getTags().isEmpty()) {
                    for (String tag : task.getTags()) {
                        if (currentSearchKeyword != null && !currentSearchKeyword.trim().isEmpty() 
                            && tag.toLowerCase().contains(currentSearchKeyword.toLowerCase())) {
                            TextFlow tagFlow = createHighlightedTextFlow("#" + tag, currentSearchKeyword);
                            tagFlow.setStyle("-fx-font-size: 10px; -fx-padding: 2 6 2 6; " +
                                           "-fx-background-color: #ecf0f1; -fx-background-radius: 3;");
                            bottomRow.getChildren().add(tagFlow);
                        } else {
                            Label tagLabel = new Label("#" + tag);
                            tagLabel.setStyle("-fx-font-size: 10px; -fx-padding: 2 6 2 6; " +
                                            "-fx-background-color: #ecf0f1; -fx-background-radius: 3;");
                            bottomRow.getChildren().add(tagLabel);
                        }
                    }
                }
                
                container.getChildren().addAll(topRow, bottomRow);
                setGraphic(container);
                
                // Apply animations for new items
                if (firstRender) {
                    firstRender = false;
                    AnimationHelper.addItemAnimation(container).play();
                }
                
                // Highlight overdue tasks
                if (task.getDueDate() != null && task.getDueDate().isBefore(java.time.LocalDate.now()) 
                    && task.getStatus() != TaskStatus.COMPLETED) {
                    setStyle("-fx-background-color: #ffe6e6;");
                } else if (task.getStatus() == TaskStatus.COMPLETED) {
                    setStyle("-fx-background-color: #f0f0f0;");
                    // Apply completion animation style
                    container.setOpacity(0.6);
                } else {
                    setStyle("");
                    container.setOpacity(1.0);
                }
            }
        }
        
        private Region createHighlightedText(String text, String keyword) {
            if (text == null || keyword == null || keyword.trim().isEmpty()) {
                Label label = new Label(text);
                label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                return label;
            }
            
            return createHighlightedTextFlow(text, keyword);
        }
        
        private TextFlow createHighlightedTextFlow(String text, String keyword) {
            TextFlow textFlow = new TextFlow();
            String lowerText = text.toLowerCase();
            String lowerKeyword = keyword.toLowerCase().trim();
            
            int lastIndex = 0;
            int index = lowerText.indexOf(lowerKeyword);
            
            while (index >= 0) {
                // Add text before match
                if (index > lastIndex) {
                    Text normalText = new Text(text.substring(lastIndex, index));
                    normalText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                    textFlow.getChildren().add(normalText);
                }
                
                // Add highlighted match
                Text highlightedText = new Text(text.substring(index, index + lowerKeyword.length()));
                highlightedText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " +
                                       "-fx-fill: #e74c3c; -fx-background-color: #fff3cd;");
                textFlow.getChildren().add(highlightedText);
                
                lastIndex = index + lowerKeyword.length();
                index = lowerText.indexOf(lowerKeyword, lastIndex);
            }
            
            // Add remaining text
            if (lastIndex < text.length()) {
                Text normalText = new Text(text.substring(lastIndex));
                normalText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                textFlow.getChildren().add(normalText);
            }
            
            return textFlow;
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
