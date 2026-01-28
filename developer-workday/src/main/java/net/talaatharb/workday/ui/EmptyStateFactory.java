package net.talaatharb.workday.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory class for creating empty state views.
 * Provides consistent, user-friendly empty states throughout the application.
 */
@Slf4j
public class EmptyStateFactory {
    
    /**
     * Create a generic empty state view
     */
    public static VBox createEmptyState(String title, String message, String icon, String actionText, Runnable action) {
        VBox emptyState = new VBox(20);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setStyle("-fx-padding: 40;");
        
        // Icon label (emoji or symbol)
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 48px;");
        
        // Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        
        // Message
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);
        messageLabel.setTextAlignment(TextAlignment.CENTER);
        
        emptyState.getChildren().addAll(iconLabel, titleLabel, messageLabel);
        
        // Action button (optional)
        if (actionText != null && action != null) {
            Button actionButton = new Button(actionText);
            actionButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                                "-fx-padding: 10 20 10 20; -fx-font-size: 14px;");
            actionButton.setOnAction(e -> action.run());
            emptyState.getChildren().add(actionButton);
        }
        
        return emptyState;
    }
    
    /**
     * Create empty state for task list
     */
    public static VBox createEmptyTaskList(Runnable onAddTask) {
        return createEmptyState(
            "No Tasks Yet",
            "Get started by creating your first task.\nStay organized and productive!",
            "📝",
            "Create Task",
            onAddTask
        );
    }
    
    /**
     * Create empty state for category
     */
    public static VBox createEmptyCategoryState(String categoryName, Runnable onAddTask) {
        return createEmptyState(
            "No Tasks in " + categoryName,
            "This category is empty.\nAdd a task to get started!",
            "📂",
            "Add Task",
            onAddTask
        );
    }
    
    /**
     * Create empty state for search results
     */
    public static VBox createEmptySearchResults(String searchTerm) {
        return createEmptyState(
            "No Results Found",
            "No tasks match your search for \"" + searchTerm + "\".\nTry different keywords or check your filters.",
            "🔍",
            null,
            null
        );
    }
    
    /**
     * Create empty state for today's tasks
     */
    public static VBox createEmptyTodayView(Runnable onAddTask) {
        return createEmptyState(
            "Nothing Scheduled for Today",
            "You're all caught up!\nSchedule a task or take a well-deserved break.",
            "✨",
            "Schedule Task",
            onAddTask
        );
    }
    
    /**
     * Create empty state for upcoming tasks
     */
    public static VBox createEmptyUpcomingView(Runnable onAddTask) {
        return createEmptyState(
            "No Upcoming Tasks",
            "Plan ahead by scheduling tasks for the future.",
            "📅",
            "Schedule Task",
            onAddTask
        );
    }
    
    /**
     * Create empty state for completed tasks
     */
    public static VBox createEmptyCompletedView() {
        return createEmptyState(
            "No Completed Tasks",
            "Complete tasks to see them here.\nTrack your accomplishments!",
            "✅",
            null,
            null
        );
    }
    
    /**
     * Create empty state for categories
     */
    public static VBox createEmptyCategoryList(Runnable onAddCategory) {
        return createEmptyState(
            "No Categories",
            "Organize your tasks by creating categories.\nGet started with your first category!",
            "🏷️",
            "Create Category",
            onAddCategory
        );
    }
}
