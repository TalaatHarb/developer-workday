package net.talaatharb.workday.ui.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.dtos.TaskDTO;
import net.talaatharb.workday.dtos.WeeklyStatistics;
import net.talaatharb.workday.facade.WeeklyReviewFacade;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Weekly Review dialog.
 */
@Slf4j
public class WeeklyReviewController implements Initializable {
    
    // Header
    @FXML private Label weekRangeLabel;
    @FXML private TabPane tabPane;
    
    // Review Tab
    @FXML private ListView<String> completedTasksListView;
    @FXML private Label completedCountLabel;
    
    // Statistics Tab
    @FXML private Label totalCompletedLabel;
    @FXML private Label totalPlannedLabel;
    @FXML private Label completionRateLabel;
    @FXML private Label highPriorityLabel;
    @FXML private Label mediumPriorityLabel;
    @FXML private Label lowPriorityLabel;
    @FXML private Label overdueTasksLabel;
    
    // Planning Tab
    @FXML private ListView<String> upcomingTasksListView;
    @FXML private Label upcomingCountLabel;
    
    // Buttons
    @FXML private Button cancelButton;
    @FXML private Button completeButton;
    
    @Setter private WeeklyReviewFacade weeklyReviewFacade;
    
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private WeeklyStatistics statistics;
    private Stage dialogStage;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing WeeklyReviewController");
        
        // Calculate current week range
        LocalDate today = LocalDate.now();
        weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        updateWeekRangeLabel();
        
        log.info("WeeklyReviewController initialized");
    }
    
    /**
     * Load and display the weekly review data.
     */
    public void loadReviewData() {
        if (weeklyReviewFacade == null) {
            log.error("WeeklyReviewFacade not set");
            return;
        }
        
        try {
            log.info("Loading weekly review data for week: {} to {}", weekStart, weekEnd);
            
            // Start the review and get statistics
            statistics = weeklyReviewFacade.startWeeklyReview(weekStart, weekEnd);
            
            // Update statistics tab
            updateStatistics();
            
            // Load completed tasks
            loadCompletedTasks();
            
            // Load upcoming tasks
            loadUpcomingTasks();
            
            log.info("Weekly review data loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load weekly review data", e);
            showError("Failed to load weekly review data: " + e.getMessage());
        }
    }
    
    /**
     * Update the week range label.
     */
    private void updateWeekRangeLabel() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        String rangeText = String.format("Week: %s - %s", 
            weekStart.format(formatter), 
            weekEnd.format(formatter));
        weekRangeLabel.setText(rangeText);
    }
    
    /**
     * Update the statistics tab with data.
     */
    private void updateStatistics() {
        if (statistics == null) {
            return;
        }
        
        totalCompletedLabel.setText(String.valueOf(statistics.getTotalTasksCompleted()));
        totalPlannedLabel.setText(String.valueOf(statistics.getTotalTasksPlanned()));
        completionRateLabel.setText(String.format("%.1f%%", statistics.getCompletionRate()));
        highPriorityLabel.setText(String.valueOf(statistics.getHighPriorityCompleted()));
        mediumPriorityLabel.setText(String.valueOf(statistics.getMediumPriorityCompleted()));
        lowPriorityLabel.setText(String.valueOf(statistics.getLowPriorityCompleted()));
        overdueTasksLabel.setText(String.valueOf(statistics.getOverdueTasks()));
    }
    
    /**
     * Load and display completed tasks.
     */
    private void loadCompletedTasks() {
        List<TaskDTO> completedTasks = weeklyReviewFacade.getCompletedTasksForWeek(weekStart, weekEnd);
        
        completedTasksListView.getItems().clear();
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d");
        
        for (TaskDTO task : completedTasks) {
            String dateStr = task.getCompletedAt() != null ? 
                task.getCompletedAt().format(dateFormatter) : "N/A";
            String displayText = String.format("[%s] %s - %s", 
                dateStr, 
                task.getTitle(),
                task.getPriority());
            completedTasksListView.getItems().add(displayText);
        }
        
        completedCountLabel.setText(completedTasks.size() + " tasks completed");
    }
    
    /**
     * Load and display upcoming tasks.
     */
    private void loadUpcomingTasks() {
        List<TaskDTO> upcomingTasks = weeklyReviewFacade.getUpcomingTasks();
        
        upcomingTasksListView.getItems().clear();
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d");
        
        for (TaskDTO task : upcomingTasks) {
            LocalDate displayDate = task.getScheduledDate() != null ? 
                task.getScheduledDate() : task.getDueDate();
            String dateStr = displayDate != null ? displayDate.format(dateFormatter) : "No date";
            String displayText = String.format("[%s] %s - %s", 
                dateStr, 
                task.getTitle(),
                task.getPriority());
            upcomingTasksListView.getItems().add(displayText);
        }
        
        upcomingCountLabel.setText(upcomingTasks.size() + " upcoming tasks");
    }
    
    /**
     * Handle Cancel button action.
     */
    @FXML
    private void handleCancel() {
        log.info("Weekly review cancelled");
        closeDialog();
    }
    
    /**
     * Handle Complete button action.
     */
    @FXML
    private void handleComplete() {
        log.info("Completing weekly review");
        
        try {
            int reviewedCount = statistics != null ? statistics.getTotalTasksCompleted() : 0;
            weeklyReviewFacade.completeWeeklyReview(weekStart, weekEnd, reviewedCount);
            
            showInfo("Weekly review completed successfully!");
            closeDialog();
        } catch (Exception e) {
            log.error("Failed to complete weekly review", e);
            showError("Failed to complete weekly review: " + e.getMessage());
        }
    }
    
    /**
     * Set the dialog stage.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    /**
     * Close the dialog.
     */
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    /**
     * Show error message.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Weekly Review Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show info message.
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Weekly Review");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
