package net.talaatharb.workday.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
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
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.facade.FocusModeFacade;
import net.talaatharb.workday.facade.UpdateCheckFacade;
import net.talaatharb.workday.dtos.FocusModeDTO;
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
        
        // Load sample categories for demonstration
        loadSampleCategories();
        
        log.info("Main window controller initialized successfully");
    }
    
    @FXML
    private void handleQuickAdd() {
        String taskInput = quickAddField.getText();
        if (taskInput != null && !taskInput.trim().isEmpty()) {
            log.info("Quick add task: {}", taskInput);
            // TODO: Integrate with TaskFacade.quickAddTask()
            // For now, the QuickAddController handles this logic when integrated
            quickAddField.clear();
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
        // TODO: Load calendar view into contentArea
        setActiveNavButton(calendarButton);
    }
    
    @FXML
    private void handleShowAllTasks() {
        log.info("Show all tasks view");
        // TODO: Load all tasks view into contentArea
        setActiveNavButton(allTasksButton);
    }
    
    @FXML
    private void handleAddCategory() {
        log.info("Add new category");
        // TODO: Open add category dialog
    }
    
    /**
     * Handle edit category action
     */
    private void handleEditCategory(CategoryItem category) {
        log.info("Editing category: {}", category.getName());
        // TODO: Open edit category dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Edit Category");
        alert.setHeaderText("Edit: " + category.getName());
        alert.setContentText("Edit category dialog will be implemented here.");
        alert.showAndWait();
    }
    
    /**
     * Handle add task to category action
     */
    private void handleAddTaskToCategory(CategoryItem category) {
        log.info("Adding task to category: {}", category.getName());
        // TODO: Open quick add task dialog with pre-selected category
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Add Task");
        alert.setHeaderText("Add task to: " + category.getName());
        alert.setContentText("Quick add task dialog will be implemented here.");
        alert.showAndWait();
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
                categoryListView.getItems().remove(category);
                log.info("Category deleted: {}", category.getName());
            }
        });
    }
    
    @FXML
    private void handleOpenSettings() {
        log.info("Opening settings dialog");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/net/talaatharb/workday/ui/SettingsDialog.fxml"));
            VBox dialogRoot = loader.load();
            
            SettingsDialogController controller = loader.getController();
            // TODO: Wire PreferencesFacade when available
            
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
            
            // Get the controller and initialize if needed
            Object controller = loader.getController();
            if (controller instanceof TodayViewController todayController) {
                // TODO: Inject TaskFacade when available
                todayController.loadTasks();
            } else if (controller instanceof UpcomingViewController upcomingController) {
                // TODO: Inject TaskFacade when available
                upcomingController.loadTasks();
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
    
    private void loadSampleCategories() {
        // Sample data for demonstration
        categoryListView.getItems().addAll(
            new CategoryItem("Work", 5, "#3498db"),
            new CategoryItem("Personal", 3, "#2ecc71"),
            new CategoryItem("Shopping", 2, "#e74c3c"),
            new CategoryItem("Health", 1, "#9b59b6")
        );
    }
    
    /**
     * Category item for display in the list
     */
    public static class CategoryItem {
        private final String name;
        private final int taskCount;
        private final String color;
        
        public CategoryItem(String name, int taskCount, String color) {
            this.name = name;
            this.taskCount = taskCount;
            this.color = color;
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
                container.setStyle("-fx-alignment: center-left;");
                
                // Color indicator
                Label colorLabel = new Label("●");
                colorLabel.setStyle("-fx-text-fill: " + item.getColor() + "; -fx-font-size: 16px;");
                
                // Category name
                Label nameLabel = new Label(item.getName());
                nameLabel.setStyle("-fx-font-size: 13px;");
                
                // Task count badge
                Label countLabel = new Label(String.valueOf(item.getTaskCount()));
                countLabel.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                                  "-fx-background-radius: 10; -fx-padding: 2 8 2 8; " +
                                  "-fx-font-size: 11px; -fx-font-weight: bold;");
                
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
