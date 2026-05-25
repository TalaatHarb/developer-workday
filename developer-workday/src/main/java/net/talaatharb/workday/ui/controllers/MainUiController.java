package net.talaatharb.workday.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.dtos.CategoryWithTaskCount;
import net.talaatharb.workday.dtos.FocusModeDTO;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.facade.CategoryFacade;
import net.talaatharb.workday.facade.FocusModeFacade;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.facade.UpdateCheckFacade;
import net.talaatharb.workday.model.UpdateInfo;
import net.talaatharb.workday.utils.ContextMenuHelper;

@Slf4j
public class MainUiController implements Initializable {

    @FXML
    private TextField quickAddField;
    
    @FXML
    private Button quickAddButton;
    
    @FXML
    private VBox sidebar;
    
    @FXML
    private Button inboxButton;
    
    @FXML
    private Label inboxBadge;
    
    @FXML
    private Button todayButton;
    
    @FXML
    private Button upcomingButton;
    
    @FXML
    private Button calendarButton;
    
    @FXML
    private Button allTasksButton;
    
    @FXML
    private Button addCategoryButton;
    
    @FXML
    private ListView<CategoryItem> categoryListView;
    
    @FXML
    private Button sidebarToggleButton;
    
    @FXML
    private StackPane contentArea;
    
    @FXML
    private MenuItem focusModeMenuItem;
    
    @FXML
    private Label focusModeStatusLabel;
    
    private boolean sidebarCollapsed = false;
    @Setter
    private TaskFacade taskFacade;
    @Setter
    private CategoryFacade categoryFacade;
    private FocusModeFacade focusModeFacade;
    private UpdateCheckFacade updateCheckFacade;
    private net.talaatharb.workday.facade.WeeklyReviewFacade weeklyReviewFacade;
    private Timer focusModeUpdateTimer;
    
    public void setFocusModeFacade(FocusModeFacade focusModeFacade) {
        this.focusModeFacade = focusModeFacade;
    }
    
    public void setUpdateCheckFacade(UpdateCheckFacade updateCheckFacade) {
        this.updateCheckFacade = updateCheckFacade;
    }
    
    public void setWeeklyReviewFacade(net.talaatharb.workday.facade.WeeklyReviewFacade weeklyReviewFacade) {
        this.weeklyReviewFacade = weeklyReviewFacade;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing UI application Main window controller...");
        
        // Set up category list view with custom cell factory
        categoryListView.setCellFactory(listView -> new CategoryCell(this));
        
        log.info("Main window controller initialized successfully");
    }
    
    /**
     * Load initial data after facades have been injected.
     * Called by JavafxApplication after setting all facades.
     */
    public void loadInitialData() {
        loadCategories();
    }
    
    /**
     * Load categories from the CategoryFacade into the sidebar.
     */
    private void loadCategories() {
        if (categoryFacade == null) {
            log.warn("CategoryFacade not available, sidebar categories will be empty");
            return;
        }
        try {
            List<CategoryWithTaskCount> categories = categoryFacade.getCategoriesWithTaskCount();
            categoryListView.getItems().clear();
            for (CategoryWithTaskCount cat : categories) {
                categoryListView.getItems().add(
                    new CategoryItem(cat.getId(), cat.getName(), (int) cat.getTaskCount(),
                        cat.getColor() != null ? cat.getColor() : "#95a5a6")
                );
            }
            log.info("Loaded {} categories into sidebar", categories.size());
        } catch (Exception e) {
            log.error("Failed to load categories into sidebar", e);
        }
    }
    
    @FXML
    private void handleQuickAdd() {
        String taskInput = quickAddField.getText();
        if (taskInput != null && !taskInput.trim().isEmpty()) {
            log.info("Quick add task: {}", taskInput);
            if (taskFacade != null) {
                try {
                    taskFacade.quickAddTask(taskInput);
                    quickAddField.clear();
                    log.info("Task created via quick add: {}", taskInput);
                } catch (Exception e) {
                    log.error("Failed to create task via quick add", e);
                }
            } else {
                log.warn("TaskFacade not available, cannot create task");
                quickAddField.clear();
            }
        }
    }
    
    @FXML
    private void handleShowToday() {
        log.info("Show today view");
        loadViewIntoContentArea("/net/talaatharb/workday/ui/TodayView.fxml");
        setActiveNavButton(todayButton);
    }
    
    @FXML
    private void handleShowInbox() {
        log.info("Show inbox view");
        loadViewIntoContentArea("/net/talaatharb/workday/ui/InboxView.fxml");
        setActiveNavButton(inboxButton);
    }
    
    @FXML
    private void handleShowUpcoming() {
        log.info("Show upcoming view");
        loadViewIntoContentArea("/net/talaatharb/workday/ui/UpcomingView.fxml");
        setActiveNavButton(upcomingButton);
    }
    
    @FXML
    private void handleShowCalendar() {
        log.info("Show calendar view");
        loadViewIntoContentArea("/net/talaatharb/workday/ui/CalendarView.fxml");
        setActiveNavButton(calendarButton);
    }
    
    @FXML
    private void handleShowAllTasks() {
        log.info("Show all tasks view");
        loadViewIntoContentArea("/net/talaatharb/workday/ui/TaskListView.fxml");
        setActiveNavButton(allTasksButton);
    }
    
    @FXML
    private void handleAddCategory() {
        log.info("Add new category");
        openCategoryManagementDialog();
    }
    
    /**
     * Handle edit category action
     */
    private void handleEditCategory(CategoryItem category) {
        log.info("Editing category: {}", category.getName());
        openCategoryManagementDialog();
    }
    
    /**
     * Handle add task to category action.
     * Prompts the user for a task title and creates it pre-assigned to the
     * given category via {@link TaskFacade#quickAddTask(String)}.
     */
    private void handleAddTaskToCategory(CategoryItem category) {
        log.info("Adding task to category: {}", category.getName());

        if (taskFacade == null) {
            showError("Task creation is not available.");
            return;
        }

        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Add Task");
        dialog.setHeaderText("Add task to: " + category.getName());
        dialog.setContentText("Task title:");

        dialog.showAndWait().ifPresent(title -> {
            String trimmed = title == null ? "" : title.trim();
            if (trimmed.isEmpty()) {
                return;
            }
            try {
                // Encode the category as a tag so quickAddTask pre-assigns it.
                String quickAddInput = trimmed + " #" + category.getName();
                taskFacade.quickAddTask(quickAddInput);
                log.info("Task created for category '{}': {}", category.getName(), trimmed);
                loadCategories();
            } catch (Exception e) {
                log.error("Failed to create task for category {}", category.getName(), e);
                showError("Failed to create task: " + e.getMessage());
            }
        });
    }
    
    /**
     * Handle delete category action
     */
    private void handleDeleteCategory(CategoryItem category) {
        log.info("Deleting category: {}", category.getName());
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Category");
        confirmAlert.setHeaderText("Delete category: " + category.getName() + "?");
        confirmAlert.setContentText(String.format("This will delete the category and affect %d tasks.", category.getTaskCount()));
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                if (categoryFacade != null && category.getId() != null) {
                    try {
                        categoryFacade.deleteCategory(category.getId());
                        loadCategories();
                        log.info("Category deleted: {}", category.getName());
                    } catch (Exception e) {
                        log.error("Failed to delete category", e);
                    }
                } else {
                    categoryListView.getItems().remove(category);
                    log.info("Category removed from display: {}", category.getName());
                }
            }
        });
    }
    
    /**
     * Open the category management dialog.
     */
    private void openCategoryManagementDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/net/talaatharb/workday/ui/CategoryManagementDialog.fxml"));
            VBox dialogRoot = loader.load();
            
            CategoryManagementDialogController controller = loader.getController();
            if (categoryFacade != null) {
                controller.setCategoryFacade(categoryFacade);
            }
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Manage Categories");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            
            Scene dialogScene = new Scene(dialogRoot);
            net.talaatharb.workday.utils.ThemeManager.getInstance().registerScene(dialogScene);
            dialogStage.setScene(dialogScene);
            
            dialogStage.setOnHidden(event -> {
                net.talaatharb.workday.utils.ThemeManager.getInstance().unregisterScene(dialogScene);
                loadCategories();
            });
            
            dialogStage.showAndWait();
        } catch (IOException e) {
            log.error("Failed to open category management dialog", e);
        }
    }
    
    @FXML
    private void handleOpenSettings() {
        log.info("Opening settings dialog");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/net/talaatharb/workday/ui/SettingsDialog.fxml"));
            VBox dialogRoot = loader.load();
            
            SettingsDialogController controller = loader.getController();
            // Wire PreferencesFacade from the ApplicationContext if available
            net.talaatharb.workday.config.ApplicationContext.getInstance()
                .getBeanOptional(net.talaatharb.workday.facade.PreferencesFacade.class)
                .ifPresent(controller::setPreferencesFacade);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Settings");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            
            Scene dialogScene = new Scene(dialogRoot);
            
            // Register scene with theme manager for immediate theme application
            net.talaatharb.workday.utils.ThemeManager.getInstance().registerScene(dialogScene);
            
            dialogStage.setScene(dialogScene);
            
            controller.setOnCloseCallback(() -> {
                net.talaatharb.workday.utils.ThemeManager.getInstance().unregisterScene(dialogScene);
                dialogStage.close();
            });
            controller.loadPreferences();
            
            dialogStage.showAndWait();
        } catch (IOException e) {
            log.error("Failed to open settings dialog", e);
        }
    }
    
    @FXML
    private void handleOpenWeeklyReview() {
        log.info("Opening weekly review dialog");
        
        if (weeklyReviewFacade == null) {
            log.warn("WeeklyReviewFacade not initialized");
            showError("Weekly Review feature is not available.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/net/talaatharb/workday/ui/WeeklyReview.fxml"));
            VBox dialogRoot = loader.load();
            
            net.talaatharb.workday.ui.controllers.WeeklyReviewController controller = loader.getController();
            controller.setWeeklyReviewFacade(weeklyReviewFacade);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Weekly Review");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            
            Scene dialogScene = new Scene(dialogRoot, 800, 600);
            
            // Register scene with theme manager
            net.talaatharb.workday.utils.ThemeManager.getInstance().registerScene(dialogScene);
            
            dialogStage.setScene(dialogScene);
            
            controller.setDialogStage(dialogStage);
            controller.loadReviewData();
            
            dialogStage.setOnHidden(event -> {
                net.talaatharb.workday.utils.ThemeManager.getInstance().unregisterScene(dialogScene);
            });
            
            dialogStage.showAndWait();
        } catch (IOException e) {
            log.error("Failed to open weekly review dialog", e);
            showError("Failed to open weekly review: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleExit() {
        log.info("Exiting application");
        Platform.exit();
    }
    
    @FXML
    private void handleAbout() {
        log.info("Showing about dialog");
        
        String version = updateCheckFacade != null ? updateCheckFacade.getCurrentVersion() : "1.0.0";
        
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.setTitle("About Developer Workday");
        about.setHeaderText("Developer Workday");
        about.setContentText("A task management application for developers.\nVersion " + version);
        about.showAndWait();
    }
    
    @FXML
    private void handleCheckForUpdates() {
        log.info("Manual update check requested");
        
        if (updateCheckFacade == null) {
            log.warn("UpdateCheckFacade not initialized");
            showError("Update check is not available");
            return;
        }
        
        // Show progress
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Checking for Updates");
        progressAlert.setHeaderText("Checking for updates...");
        progressAlert.setContentText("Please wait while we check for the latest version.");
        
        // Check in background thread
        new Thread(() -> {
            UpdateInfo updateInfo = updateCheckFacade.checkForUpdatesManually();
            
            // Show result on JavaFX thread
            Platform.runLater(() -> {
                progressAlert.close();
                showUpdateDialog(updateInfo);
            });
        }).start();
        
        progressAlert.show();
    }
    
    /**
     * Show update notification dialog
     */
    private void showUpdateDialog(UpdateInfo updateInfo) {
        if (updateInfo == null) {
            showError("Failed to check for updates. Please try again later.");
            return;
        }
        
        if (updateInfo.isUpdateAvailable()) {
            Alert updateAlert = new Alert(Alert.AlertType.INFORMATION);
            updateAlert.setTitle("Update Available");
            updateAlert.setHeaderText(String.format("Version %s is available!", updateInfo.getLatestVersion()));
            
            String content = String.format(
                "Current version: %s\nLatest version: %s\n\n%s\n\nWould you like to download the update?",
                updateInfo.getCurrentVersion(),
                updateInfo.getLatestVersion(),
                updateInfo.getReleaseNotes() != null ? updateInfo.getReleaseNotes() : ""
            );
            updateAlert.setContentText(content);
            
            // Add buttons
            updateAlert.getButtonTypes().clear();
            updateAlert.getButtonTypes().addAll(
                javafx.scene.control.ButtonType.YES,
                javafx.scene.control.ButtonType.NO
            );
            
            updateAlert.showAndWait().ifPresent(response -> {
                if (response == javafx.scene.control.ButtonType.YES) {
                    openDownloadLink(updateInfo.getDownloadUrl());
                }
            });
        } else {
            Alert noUpdateAlert = new Alert(Alert.AlertType.INFORMATION);
            noUpdateAlert.setTitle("No Updates Available");
            noUpdateAlert.setHeaderText("You're up to date!");
            noUpdateAlert.setContentText(String.format(
                "You have the latest version (%s) of Developer Workday.",
                updateInfo.getCurrentVersion()
            ));
            noUpdateAlert.showAndWait();
        }
    }
    
    /**
     * Open download link in default browser
     */
    private void openDownloadLink(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            log.error("Failed to open download link", e);
            showError("Failed to open download link: " + url);
        }
    }
    
    /**
     * Show error dialog
     */
    private void showError(String message) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("Error");
        errorAlert.setHeaderText("An error occurred");
        errorAlert.setContentText(message);
        errorAlert.showAndWait();
    }
    
    @FXML
    private void handleToggleFocusMode() {
        if (focusModeFacade == null) {
            log.warn("FocusModeFacade not initialized");
            return;
        }
        
        if (focusModeFacade.isFocusModeEnabled()) {
            focusModeFacade.disableFocusMode();
            updateFocusModeUI(false);
        } else {
            // Show dialog to configure focus mode
            showFocusModeConfigDialog();
        }
    }
    
    private void showFocusModeConfigDialog() {
        Alert configDialog = new Alert(Alert.AlertType.CONFIRMATION);
        configDialog.setTitle("Enable Focus Mode");
        configDialog.setHeaderText("Configure Focus Mode");
        configDialog.setContentText("Focus mode will suppress notifications and simplify the UI.\n\nStart focus mode now?");
        
        configDialog.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                // Enable focus mode with default 25-minute Pomodoro timer
                focusModeFacade.enableFocusMode(25, 25);
                updateFocusModeUI(true);
                startFocusModeUpdateTimer();
            }
        });
    }
    
    private void updateFocusModeUI(boolean enabled) {
        Platform.runLater(() -> {
            if (enabled) {
                focusModeMenuItem.setText("⬤ Disable Focus Mode");
                focusModeStatusLabel.setText("🎯 Focus Mode Active");
                focusModeStatusLabel.setVisible(true);
                focusModeStatusLabel.setManaged(true);
                
                // Simplify UI - collapse sidebar
                if (!sidebarCollapsed) {
                    handleToggleSidebar();
                }
            } else {
                focusModeMenuItem.setText("Toggle Focus Mode");
                focusModeStatusLabel.setVisible(false);
                focusModeStatusLabel.setManaged(false);
                stopFocusModeUpdateTimer();
            }
        });
    }
    
    private void startFocusModeUpdateTimer() {
        stopFocusModeUpdateTimer();
        focusModeUpdateTimer = new Timer("FocusModeUIUpdate", true);
        focusModeUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (focusModeFacade != null && focusModeFacade.isFocusModeEnabled()) {
                    FocusModeDTO state = focusModeFacade.getCurrentState();
                    Integer remaining = state.getRemainingMinutes();
                    
                    Platform.runLater(() -> {
                        if (remaining != null) {
                            focusModeStatusLabel.setText(
                                    String.format("🎯 Focus Mode (%d min remaining)", remaining));
                        } else {
                            focusModeStatusLabel.setText("🎯 Focus Mode Active");
                        }
                    });
                } else {
                    Platform.runLater(() -> updateFocusModeUI(false));
                    stopFocusModeUpdateTimer();
                }
            }
        }, 1000, 60000); // Update every minute
    }
    
    private void stopFocusModeUpdateTimer() {
        if (focusModeUpdateTimer != null) {
            focusModeUpdateTimer.cancel();
            focusModeUpdateTimer = null;
        }
    }
    
    @FXML
    private void handleToggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        
        if (sidebarCollapsed) {
            sidebar.setPrefWidth(50);
            sidebar.setMinWidth(50);
            sidebarToggleButton.setText("▶");
        } else {
            sidebar.setPrefWidth(250);
            sidebar.setMinWidth(220);
            sidebarToggleButton.setText("◀ Collapse");
        }
        
        log.info("Sidebar collapsed: {}", sidebarCollapsed);
    }
    
    private void setActiveNavButton(Button activeButton) {
        // Remove active style from all buttons
        todayButton.getStyleClass().remove("active");
        upcomingButton.getStyleClass().remove("active");
        calendarButton.getStyleClass().remove("active");
        allTasksButton.getStyleClass().remove("active");
        
        // Add active style to clicked button
        if (!activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }
    
    /**
     * Load a view into the content area
     */
    private void loadViewIntoContentArea(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Node view = loader.load();
            
            // Get the controller and inject facades
            Object controller = loader.getController();
            if (controller instanceof TodayViewController todayController) {
                todayController.setTaskFacade(taskFacade);
                todayController.loadTasks();
            } else if (controller instanceof UpcomingViewController upcomingController) {
                upcomingController.setTaskFacade(taskFacade);
                upcomingController.loadTasks();
            } else if (controller instanceof TaskListViewController taskListController) {
                taskListController.setTaskFacade(taskFacade);
                taskListController.setCategoryFacade(categoryFacade);
                taskListController.loadTasksFromFacade();
            } else if (controller instanceof CalendarViewController calendarController) {
                calendarController.setTaskFacade(taskFacade);
                calendarController.setCalendarFacade(
                    net.talaatharb.workday.config.ApplicationContext.getInstance()
                        .getBeanOptional(net.talaatharb.workday.facade.CalendarFacade.class)
                        .orElse(null));
                calendarController.loadCalendarTasks();
            } else if (controller instanceof InboxViewController inboxController) {
                inboxController.setTaskFacade(taskFacade);
                inboxController.setCategoryFacade(categoryFacade);
                inboxController.loadInboxTasks();
            }
            
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            
            log.info("Loaded view: {}", fxmlPath);
        } catch (IOException e) {
            log.error("Failed to load view: {}", fxmlPath, e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load view");
            alert.setContentText("Could not load " + fxmlPath);
            alert.showAndWait();
        }
    }
    
    /**
     * Category item for display in the list
     */
    public static class CategoryItem {
        private final java.util.UUID id;
        private final String name;
        private final int taskCount;
        private final String color;
        
        public CategoryItem(String name, int taskCount, String color) {
            this(null, name, taskCount, color);
        }
        
        public CategoryItem(java.util.UUID id, String name, int taskCount, String color) {
            this.id = id;
            this.name = name;
            this.taskCount = taskCount;
            this.color = color;
        }
        
        public java.util.UUID getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public int getTaskCount() {
            return taskCount;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    /**
     * Custom cell for category list items
     */
    private static class CategoryCell extends ListCell<CategoryItem> {
        private final MainUiController controller;
        
        public CategoryCell(MainUiController controller) {
            super();
            this.controller = controller;
            
            // Add context menu on right-click
            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !isEmpty()) {
                    CategoryItem item = getItem();
                    if (item != null) {
                        showContextMenuForCategory(item, this);
                    }
                }
            });
        }
        
        private void showContextMenuForCategory(CategoryItem category, CategoryCell cell) {
            ContextMenu contextMenu = ContextMenuHelper.createCategoryContextMenu(
                category.getName(),
                () -> controller.handleEditCategory(category),
                () -> controller.handleAddTaskToCategory(category),
                () -> controller.handleDeleteCategory(category)
            );
            
            contextMenu.show(cell, cell.getLayoutX(), cell.getLayoutY());
        }
        
        @Override
        protected void updateItem(CategoryItem item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox container = new HBox(10);
                container.getStyleClass().add("category-cell-row");
                
                // Color indicator (uses category-specific color inline since it's data-driven)
                Label colorLabel = new Label("●");
                colorLabel.getStyleClass().add("category-cell-color");
                colorLabel.setStyle("-fx-text-fill: " + item.getColor() + ";");
                
                // Category name
                Label nameLabel = new Label(item.getName());
                nameLabel.getStyleClass().add("category-cell-name");
                
                // Task count badge
                Label countLabel = new Label(String.valueOf(item.getTaskCount()));
                countLabel.getStyleClass().add("category-count-badge");
                
                container.getChildren().addAll(colorLabel, nameLabel);
                
                // Add spacer and count badge
                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                container.getChildren().addAll(spacer, countLabel);
                
                setGraphic(container);
            }
        }
    }
}
