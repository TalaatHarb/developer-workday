package net.talaatharb.workday.ui.controllers;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.task.TaskUpdatedEvent;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Subtask;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.utils.AnimationHelper;

/**
 * Controller for the task detail/edit panel.
 * Handles displaying and editing task details with auto-save functionality.
 */
@Slf4j
public class TaskDetailPanelController implements Initializable {

    private static final long AUTO_SAVE_DELAY_MS = 1000; // 1 second delay
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Label unsavedIndicator;
    
    @FXML
    private Button closeButton;
    
    @FXML
    private TextField titleField;
    
    @FXML
    private ChoiceBox<Priority> priorityChoice;
    
    @FXML
    private ChoiceBox<TaskStatus> statusChoice;
    
    @FXML
    private DatePicker dueDatePicker;
    
    @FXML
    private TextField dueTimeField;
    
    @FXML
    private TextArea descriptionArea;
    
    @FXML
    private TextField tagsField;
    
    @FXML
    private TextField reminderField;
    
    @FXML
    private Button deleteButton;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private ListView<Subtask> subtaskListView;
    
    @FXML
    private TextField subtaskInputField;
    
    @FXML
    private Button addSubtaskButton;
    
    @FXML
    private Label subtaskProgressLabel;
    
    @Setter
    private TaskFacade taskFacade;
    
    @Setter
    private EventDispatcher eventDispatcher;
    
    private Task currentTask;
    private Task originalTask;
    private boolean hasUnsavedChanges = false;
    private java.util.Timer autoSaveTimer;
    private Runnable onCloseCallback;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing TaskDetailPanelController");
        
        // Setup choice boxes
        priorityChoice.setItems(FXCollections.observableArrayList(Priority.values()));
        statusChoice.setItems(FXCollections.observableArrayList(TaskStatus.values()));
        
        // Setup subtask list view with custom cell factory
        setupSubtaskListView();
        
        // Setup change listeners for auto-save
        setupAutoSaveListeners();
        
        log.info("TaskDetailPanelController initialized successfully");
    }
    
    /**
     * Setup subtask list view with custom cell factory
     */
    private void setupSubtaskListView() {
        subtaskListView.setCellFactory(listView -> new javafx.scene.control.ListCell<Subtask>() {
            @Override
            protected void updateItem(Subtask subtask, boolean empty) {
                super.updateItem(subtask, empty);
                
                if (empty || subtask == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    CheckBox checkBox = new CheckBox(subtask.getTitle());
                    checkBox.setSelected(subtask.isCompleted());
                    
                    // Handle checkbox changes with animation
                    checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                        subtask.setCompleted(isSelected);
                        if (isSelected) {
                            subtask.setCompletedAt(java.time.LocalDateTime.now());
                            // Play check animation
                            AnimationHelper.checkboxCheckAnimation(checkBox).play();
                        } else {
                            subtask.setCompletedAt(null);
                        }
                        updateSubtaskProgress();
                        scheduleAutoSave();
                    });
                    
                    Button deleteBtn = new Button("×");
                    deleteBtn.setOnAction(e -> {
                        if (currentTask != null && currentTask.getSubtasks() != null) {
                            currentTask.getSubtasks().remove(subtask);
                            refreshSubtaskList();
                            updateSubtaskProgress();
                            scheduleAutoSave();
                        }
                    });
                    
                    HBox hbox = new HBox(10, checkBox, deleteBtn);
                    hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(hbox);
                }
            }
        });
    }
    
    /**
     * Refresh subtask list view
     */
    private void refreshSubtaskList() {
        if (currentTask != null && currentTask.getSubtasks() != null) {
            ObservableList<Subtask> observableSubtasks = FXCollections.observableArrayList(currentTask.getSubtasks());
            subtaskListView.setItems(observableSubtasks);
        } else {
            subtaskListView.setItems(FXCollections.observableArrayList());
        }
    }
    
    /**
     * Update subtask progress indicator
     */
    private void updateSubtaskProgress() {
        if (currentTask == null || currentTask.getSubtasks() == null || currentTask.getSubtasks().isEmpty()) {
            subtaskProgressLabel.setText("No subtasks");
            return;
        }
        
        long total = currentTask.getSubtasks().size();
        long completed = currentTask.getSubtasks().stream().filter(Subtask::isCompleted).count();
        int percentage = (int) ((completed * 100.0) / total);
        
        subtaskProgressLabel.setText(String.format("%d/%d (%d%%)", completed, total, percentage));
        
        // Check if all subtasks are completed
        if (completed == total && total > 0) {
            log.debug("All subtasks completed - prompting to complete parent task");
            // Could show a dialog or notification here
        }
    }
    
    /**
     * Handle add subtask button click
     */
    @FXML
    private void handleAddSubtask() {
        String title = subtaskInputField.getText();
        if (title == null || title.trim().isEmpty()) {
            return;
        }
        
        if (currentTask == null) {
            log.warn("Cannot add subtask: no current task");
            return;
        }
        
        // Initialize subtasks list if needed
        if (currentTask.getSubtasks() == null) {
            currentTask.setSubtasks(new ArrayList<>());
        }
        
        // Create new subtask
        Subtask subtask = Subtask.builder()
            .id(UUID.randomUUID())
            .title(title.trim())
            .completed(false)
            .sortOrder(currentTask.getSubtasks().size())
            .createdAt(java.time.LocalDateTime.now())
            .build();
        
        currentTask.getSubtasks().add(subtask);
        
        // Clear input field
        subtaskInputField.clear();
        
        // Refresh list
        refreshSubtaskList();
        updateSubtaskProgress();
        
        // Animate the newly added subtask
        if (subtaskListView.getItems().size() > 0) {
            javafx.application.Platform.runLater(() -> {
                int lastIndex = subtaskListView.getItems().size() - 1;
                javafx.scene.Node cell = subtaskListView.lookup(".list-cell:nth-child(" + (lastIndex + 1) + ")");
                if (cell != null) {
                    AnimationHelper.addItemAnimation(cell).play();
                }
            });
        }
        
        // Mark as changed
        scheduleAutoSave();
        
        log.debug("Added subtask: {}", title);
    }
    
    /**
     * Load task into the detail panel
     */
    public void loadTask(Task task) {
        if (task == null) {
            log.warn("Attempted to load null task");
            return;
        }
        
        log.debug("Loading task: {}", task.getTitle());
        
        this.currentTask = task;
        this.originalTask = Task.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .priority(task.getPriority())
            .status(task.getStatus())
            .dueDate(task.getDueDate())
            .dueTime(task.getDueTime())
            .tags(task.getTags() != null ? List.copyOf(task.getTags()) : null)
            .reminderMinutesBefore(task.getReminderMinutesBefore())
            .scheduledDate(task.getScheduledDate())
            .categoryId(task.getCategoryId())
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .build();
        
        // Populate fields
        titleField.setText(task.getTitle() != null ? task.getTitle() : "");
        priorityChoice.setValue(task.getPriority() != null ? task.getPriority() : Priority.MEDIUM);
        statusChoice.setValue(task.getStatus() != null ? task.getStatus() : TaskStatus.TODO);
        dueDatePicker.setValue(task.getDueDate());
        dueTimeField.setText(task.getDueTime() != null ? task.getDueTime().format(TIME_FORMATTER) : "");
        descriptionArea.setText(task.getDescription() != null ? task.getDescription() : "");
        tagsField.setText(task.getTags() != null ? String.join(", ", task.getTags()) : "");
        reminderField.setText(task.getReminderMinutesBefore() != null ? task.getReminderMinutesBefore().toString() : "");
        
        // Load subtasks
        refreshSubtaskList();
        updateSubtaskProgress();
        
        hasUnsavedChanges = false;
        updateUnsavedIndicator();
    }
    
    /**
     * Setup auto-save listeners on all fields
     */
    private void setupAutoSaveListeners() {
        // Text field listeners
        titleField.textProperty().addListener((obs, old, newVal) -> scheduleAutoSave());
        dueTimeField.textProperty().addListener((obs, old, newVal) -> scheduleAutoSave());
        descriptionArea.textProperty().addListener((obs, old, newVal) -> scheduleAutoSave());
        tagsField.textProperty().addListener((obs, old, newVal) -> scheduleAutoSave());
        reminderField.textProperty().addListener((obs, old, newVal) -> scheduleAutoSave());
        
        // Choice box listeners
        priorityChoice.valueProperty().addListener((obs, old, newVal) -> scheduleAutoSave());
        statusChoice.valueProperty().addListener((obs, old, newVal) -> scheduleAutoSave());
        
        // Date picker listener
        dueDatePicker.valueProperty().addListener((obs, old, newVal) -> scheduleAutoSave());
    }
    
    /**
     * Schedule auto-save after a delay
     */
    private void scheduleAutoSave() {
        // Cancel any existing timer
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
        }
        
        // Mark as unsaved
        hasUnsavedChanges = true;
        updateUnsavedIndicator();
        
        // Schedule new save
        autoSaveTimer = new java.util.Timer();
        autoSaveTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> performAutoSave());
            }
        }, AUTO_SAVE_DELAY_MS);
    }
    
    /**
     * Perform auto-save operation
     */
    private void performAutoSave() {
        if (currentTask == null || !hasUnsavedChanges) {
            return;
        }
        
        try {
            // Update current task with field values
            updateTaskFromFields();
            
            // Save via facade
            if (taskFacade != null) {
                Task updatedTask = taskFacade.updateTask(currentTask);
                currentTask = updatedTask;
                
                // Publish event
                if (eventDispatcher != null) {
                    eventDispatcher.publish(new TaskUpdatedEvent(originalTask, updatedTask));
                }
                
                // Update original task snapshot
                originalTask = Task.builder()
                    .id(updatedTask.getId())
                    .title(updatedTask.getTitle())
                    .description(updatedTask.getDescription())
                    .priority(updatedTask.getPriority())
                    .status(updatedTask.getStatus())
                    .dueDate(updatedTask.getDueDate())
                    .dueTime(updatedTask.getDueTime())
                    .tags(updatedTask.getTags() != null ? List.copyOf(updatedTask.getTags()) : null)
                    .reminderMinutesBefore(updatedTask.getReminderMinutesBefore())
                    .scheduledDate(updatedTask.getScheduledDate())
                    .categoryId(updatedTask.getCategoryId())
                    .createdAt(updatedTask.getCreatedAt())
                    .updatedAt(updatedTask.getUpdatedAt())
                    .build();
                
                log.debug("Auto-saved task: {}", updatedTask.getTitle());
            }
            
            hasUnsavedChanges = false;
            updateUnsavedIndicator();
            
        } catch (Exception e) {
            log.error("Failed to auto-save task", e);
        }
    }
    
    /**
     * Update task object from form fields
     */
    private void updateTaskFromFields() {
        currentTask = Task.builder()
            .id(currentTask.getId())
            .title(titleField.getText())
            .description(descriptionArea.getText())
            .priority(priorityChoice.getValue())
            .status(statusChoice.getValue())
            .dueDate(dueDatePicker.getValue())
            .dueTime(parseDueTime(dueTimeField.getText()))
            .tags(parseTags(tagsField.getText()))
            .reminderMinutesBefore(parseReminder(reminderField.getText()))
            .subtasks(currentTask.getSubtasks())
            .scheduledDate(currentTask.getScheduledDate())
            .categoryId(currentTask.getCategoryId())
            .createdAt(currentTask.getCreatedAt())
            .updatedAt(LocalDate.now().atStartOfDay())
            .build();
    }
    
    /**
     * Parse due time from string
     */
    private LocalTime parseDueTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalTime.parse(timeStr.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse time: {}", timeStr);
            return null;
        }
    }
    
    /**
     * Parse tags from comma-separated string
     */
    private List<String> parseTags(String tagsStr) {
        if (tagsStr == null || tagsStr.trim().isEmpty()) {
            return List.of();
        }
        
        return Arrays.stream(tagsStr.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
    
    /**
     * Parse reminder minutes from string
     */
    private Integer parseReminder(String reminderStr) {
        if (reminderStr == null || reminderStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Integer.parseInt(reminderStr.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse reminder: {}", reminderStr);
            return null;
        }
    }
    
    /**
     * Update unsaved indicator visibility
     */
    private void updateUnsavedIndicator() {
        unsavedIndicator.setVisible(hasUnsavedChanges);
    }
    
    /**
     * Handle save button click
     */
    @FXML
    private void handleSave() {
        log.debug("Save button clicked");
        
        // Cancel any pending auto-save
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
        }
        
        // Perform immediate save
        if (hasUnsavedChanges) {
            performAutoSave();
        }
    }
    
    /**
     * Handle close button click
     */
    @FXML
    private void handleClose() {
        log.debug("Close button clicked");
        
        // Save any unsaved changes before closing
        if (hasUnsavedChanges) {
            handleSave();
        }
        
        // Cancel any pending auto-save
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
        }
        
        // Trigger close callback
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
    
    /**
     * Handle delete button click
     */
    @FXML
    private void handleDelete() {
        log.debug("Delete button clicked");
        
        if (currentTask != null && taskFacade != null) {
            // TODO: Add confirmation dialog
            taskFacade.deleteTask(currentTask.getId());
            
            // Close panel after delete
            handleClose();
        }
    }
    
    /**
     * Set callback to execute when panel is closed
     */
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }
    
    /**
     * Animate panel sliding in from right
     */
    public void slideIn(Node panelNode) {
        if (panelNode == null) {
            return;
        }
        
        // Start position: off-screen to the right
        panelNode.setTranslateX(panelNode.getBoundsInParent().getWidth());
        panelNode.setOpacity(0);
        
        // Slide-in animation
        TranslateTransition slideTransition = new TranslateTransition(Duration.millis(300), panelNode);
        slideTransition.setToX(0);
        
        // Fade-in animation
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(300), panelNode);
        fadeTransition.setToValue(1.0);
        
        // Play both animations
        slideTransition.play();
        fadeTransition.play();
        
        log.debug("Sliding in task detail panel");
    }
    
    /**
     * Animate panel sliding out to the right
     */
    public void slideOut(Node panelNode, Runnable onComplete) {
        if (panelNode == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        
        // Slide-out animation
        TranslateTransition slideTransition = new TranslateTransition(Duration.millis(300), panelNode);
        slideTransition.setToX(panelNode.getBoundsInParent().getWidth());
        
        // Fade-out animation
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(300), panelNode);
        fadeTransition.setToValue(0);
        
        // Execute callback when complete
        slideTransition.setOnFinished(e -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
        
        // Play both animations
        slideTransition.play();
        fadeTransition.play();
        
        log.debug("Sliding out task detail panel");
    }
    
    /**
     * Check if panel has unsaved changes
     */
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
    
    /**
     * Get current task
     */
    public Task getCurrentTask() {
        return currentTask;
    }
}
