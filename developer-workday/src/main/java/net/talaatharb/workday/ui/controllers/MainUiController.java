package net.talaatharb.workday.ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainUiController implements Initializable {

    @FXML
    private TextField quickAddField;
    
    @FXML
    private Button quickAddButton;
    
    @FXML
    private VBox sidebar;
    
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
    
    private boolean sidebarCollapsed = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing UI application Main window controller...");
        
        // Set up category list view with custom cell factory
        categoryListView.setCellFactory(listView -> new CategoryCell());
        
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
        // TODO: Load today view into contentArea
        setActiveNavButton(todayButton);
    }
    
    @FXML
    private void handleShowUpcoming() {
        log.info("Show upcoming view");
        // TODO: Load upcoming view into contentArea
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
            dialogStage.setScene(new Scene(dialogRoot));
            
            controller.setOnCloseCallback(() -> dialogStage.close());
            controller.loadPreferences();
            
            dialogStage.showAndWait();
        } catch (IOException e) {
            log.error("Failed to open settings dialog", e);
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
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.setTitle("About Developer Workday");
        about.setHeaderText("Developer Workday");
        about.setContentText("A task management application for developers.\nVersion 1.0.0");
        about.showAndWait();
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
