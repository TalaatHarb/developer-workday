package net.talaatharb.workday.ui.controllers;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.task.TaskDeletedEvent;
import net.talaatharb.workday.event.task.TaskUpdatedEvent;
import net.talaatharb.workday.facade.CategoryFacade;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.model.Category;
import net.talaatharb.workday.model.Task;

/**
 * Controller for the Inbox view.
 * Displays tasks that haven't been scheduled or categorized.
 */
@Slf4j
public class InboxViewController implements Initializable {
    
    @FXML
    private VBox tasksContainer;
    
    @FXML
    private VBox emptyStateContainer;
    
    @FXML
    private Label inboxCountLabel;
    
    @Setter
    private TaskFacade taskFacade;
    
    @Setter
    private CategoryFacade categoryFacade;
    
    @Setter
    private EventDispatcher eventDispatcher;
    
    private List<Category> categories;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing InboxViewController");
    }
    
    /**
     * Load and display inbox tasks
     */
    public void loadInboxTasks() {
        if (taskFacade == null) {
            log.warn("TaskFacade not set, cannot load inbox tasks");
            return;
        }
        
        log.debug("Loading inbox tasks");
        
        // Load categories if needed
        if (categories == null && categoryFacade != null) {
            try {
                categories = categoryFacade.findAll();
            } catch (Exception e) {
                log.error("Failed to load categories", e);
            }
        }
        
        // Get inbox tasks
        List<Task> inboxTasks = taskFacade.getInboxTasks();
        
        // Update UI on JavaFX thread
        Platform.runLater(() -> {
            displayInboxTasks(inboxTasks);
        });
    }
    
    /**
     * Display inbox tasks
     */
    private void displayInboxTasks(List<Task> tasks) {
        tasksContainer.getChildren().clear();
        
        if (tasks.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            
            // Update count label
            inboxCountLabel.setText(tasks.size() + (tasks.size() == 1 ? " item" : " items"));
            
            // Add task cards
            for (Task task : tasks) {
                tasksContainer.getChildren().add(createTaskCard(task));
            }
        }
    }
    
    /**
     * Create a task card with quick actions
     */
    private VBox createTaskCard(Task task) {
        VBox card = new VBox(10);
        card.getStyleClass().add("inbox-task-card");
        card.setPadding(new Insets(15));
        
        // Task title
        Label titleLabel = new Label(task.getTitle());
        titleLabel.setFont(Font.font("System Bold", 14));
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("task-title");
        
        // Task metadata (if any)
        HBox metadataBox = new HBox(10);
        metadataBox.setAlignment(Pos.CENTER_LEFT);
        
        if (task.getTags() != null && !task.getTags().isEmpty()) {
            for (String tag : task.getTags()) {
                Label tagLabel = new Label("#" + tag);
                tagLabel.getStyleClass().add("task-tag");
                metadataBox.getChildren().add(tagLabel);
            }
        }
        
        // Quick actions
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_LEFT);
        actionsBox.setPadding(new Insets(10, 0, 0, 0));
        
        // Schedule button with date picker
        Button scheduleButton = new Button("📅 Schedule");
        scheduleButton.getStyleClass().add("inbox-action-button");
        scheduleButton.setOnAction(e -> handleScheduleTask(task));
        
        // Categorize button
        Button categorizeButton = new Button("🏷️ Categorize");
        categorizeButton.getStyleClass().add("inbox-action-button");
        categorizeButton.setOnAction(e -> handleCategorizeTask(task));
        
        // Delete button
        Button deleteButton = new Button("🗑️ Delete");
        deleteButton.getStyleClass().addAll("inbox-action-button", "delete-button");
        deleteButton.setOnAction(e -> handleDeleteTask(task));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        actionsBox.getChildren().addAll(scheduleButton, categorizeButton, spacer, deleteButton);
        
        card.getChildren().addAll(titleLabel, metadataBox, actionsBox);
        
        return card;
    }
    
    /**
     * Handle scheduling a task
     */
    private void handleScheduleTask(Task task) {
        log.debug("Scheduling task: {}", task.getId());
        
        // Create date picker dialog
        DatePicker datePicker = new DatePicker(LocalDate.now());
        
        // Create dialog
        VBox dialogContent = new VBox(15);
        dialogContent.setPadding(new Insets(20));
        dialogContent.setAlignment(Pos.CENTER);
        
        Label promptLabel = new Label("Schedule \"" + task.getTitle() + "\" for:");
        promptLabel.setFont(Font.font("System Bold", 14));
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button todayButton = new Button("Today");
        todayButton.setOnAction(e -> scheduleTaskForDate(task, LocalDate.now()));
        
        Button tomorrowButton = new Button("Tomorrow");
        tomorrowButton.setOnAction(e -> scheduleTaskForDate(task, LocalDate.now().plusDays(1)));
        
        Button customButton = new Button("Pick Date");
        customButton.setOnAction(e -> {
            if (datePicker.getValue() != null) {
                scheduleTaskForDate(task, datePicker.getValue());
            }
        });
        
        buttonBox.getChildren().addAll(todayButton, tomorrowButton);
        
        dialogContent.getChildren().addAll(promptLabel, datePicker, buttonBox, customButton);
        
        // For now, just schedule for today as a simple implementation
        // In a full implementation, you'd show a proper dialog
        scheduleTaskForDate(task, LocalDate.now());
    }
    
    /**
     * Schedule task for a specific date
     */
    private void scheduleTaskForDate(Task task, LocalDate date) {
        try {
            task.setScheduledDate(date);
            task.setDueDate(date);
            Task updatedTask = taskFacade.updateTask(task);
            
            if (eventDispatcher != null) {
                eventDispatcher.publish(new TaskUpdatedEvent(task, updatedTask));
            }
            
            log.info("Scheduled task {} for {}", task.getId(), date);
            
            // Reload inbox
            loadInboxTasks();
        } catch (Exception e) {
            log.error("Failed to schedule task", e);
        }
    }
    
    /**
     * Handle categorizing a task
     */
    private void handleCategorizeTask(Task task) {
        log.debug("Categorizing task: {}", task.getId());
        
        if (categories == null || categories.isEmpty()) {
            log.warn("No categories available");
            return;
        }
        
        // Create category picker
        ComboBox<Category> categoryPicker = new ComboBox<>();
        categoryPicker.getItems().addAll(categories);
        categoryPicker.setPromptText("Select category...");
        
        // For simplicity, use the first category
        // In a full implementation, show a proper dialog
        Category firstCategory = categories.get(0);
        categorizeTask(task, firstCategory);
    }
    
    /**
     * Categorize a task
     */
    private void categorizeTask(Task task, Category category) {
        try {
            task.setCategoryId(category.getId());
            Task updatedTask = taskFacade.updateTask(task);
            
            if (eventDispatcher != null) {
                eventDispatcher.publish(new TaskUpdatedEvent(task, updatedTask));
            }
            
            log.info("Categorized task {} to {}", task.getId(), category.getName());
            
            // Reload inbox
            loadInboxTasks();
        } catch (Exception e) {
            log.error("Failed to categorize task", e);
        }
    }
    
    /**
     * Handle deleting a task
     */
    private void handleDeleteTask(Task task) {
        log.debug("Deleting task: {}", task.getId());
        
        try {
            taskFacade.deleteTask(task.getId());
            
            if (eventDispatcher != null) {
                eventDispatcher.publish(new TaskDeletedEvent(task));
            }
            
            log.info("Deleted task: {}", task.getId());
            
            // Reload inbox
            loadInboxTasks();
        } catch (Exception e) {
            log.error("Failed to delete task", e);
        }
    }
    
    /**
     * Show empty state
     */
    private void showEmptyState() {
        tasksContainer.setVisible(false);
        tasksContainer.setManaged(false);
        emptyStateContainer.setVisible(true);
        emptyStateContainer.setManaged(true);
        inboxCountLabel.setText("0 items");
    }
    
    /**
     * Hide empty state
     */
    private void hideEmptyState() {
        emptyStateContainer.setVisible(false);
        emptyStateContainer.setManaged(false);
        tasksContainer.setVisible(true);
        tasksContainer.setManaged(true);
    }
    
    /**
     * Get inbox count for badge
     */
    public int getInboxCount() {
        if (taskFacade == null) {
            return 0;
        }
        return taskFacade.getInboxTasks().size();
    }
}
