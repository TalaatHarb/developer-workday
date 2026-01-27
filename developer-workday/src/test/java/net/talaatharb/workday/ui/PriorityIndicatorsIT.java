package net.talaatharb.workday.ui;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.utils.PriorityIndicators;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating priority indicators in UI components.
 */
@ExtendWith(ApplicationExtension.class)
class PriorityIndicatorsIT {
    
    private VBox root;
    private ListView<HBox> taskListView;
    
    @Start
    void start(Stage stage) {
        root = new VBox(10);
        root.setStyle("-fx-padding: 20;");
        
        // Create a task list view
        taskListView = new ListView<>();
        
        // Add sample tasks with different priorities
        addTaskToList("Fix critical bug", Priority.URGENT);
        addTaskToList("Review pull request", Priority.HIGH);
        addTaskToList("Update documentation", Priority.MEDIUM);
        addTaskToList("Refactor utility class", Priority.LOW);
        
        root.getChildren().add(taskListView);
        
        Scene scene = new Scene(root, 400, 300);
        stage.setScene(scene);
        stage.show();
    }
    
    private void addTaskToList(String taskTitle, Priority priority) {
        HBox taskItem = new HBox(10);
        taskItem.setStyle("-fx-alignment: center-left; -fx-padding: 5;");
        
        // Add priority indicator
        Circle priorityCircle = PriorityIndicators.createPriorityCircle(priority, 12);
        
        // Add task title
        Label titleLabel = new Label(taskTitle);
        
        // Add priority badge
        Label priorityBadge = PriorityIndicators.createPriorityBadge(priority, 20);
        
        taskItem.getChildren().addAll(priorityCircle, titleLabel, priorityBadge);
        
        // Apply priority border style
        String borderStyle = PriorityIndicators.getPriorityBorderStyle(priority, 4);
        taskItem.setStyle(taskItem.getStyle() + borderStyle);
        
        taskListView.getItems().add(taskItem);
    }
    
    @Test
    void shouldDisplayTasksWithPriorityIndicators() {
        // Verify all tasks are displayed
        assertEquals(4, taskListView.getItems().size());
        
        // Verify each task has priority indicators
        for (HBox taskItem : taskListView.getItems()) {
            // Each task should have at least: circle, title, badge
            assertTrue(taskItem.getChildren().size() >= 3);
            
            // First child should be a Circle (priority indicator)
            assertTrue(taskItem.getChildren().get(0) instanceof Circle);
            
            // Should have border style applied
            assertNotNull(taskItem.getStyle());
            assertTrue(taskItem.getStyle().contains("-fx-border"));
        }
    }
    
    @Test
    void shouldShowDifferentColorsForDifferentPriorities() {
        HBox urgentTask = taskListView.getItems().get(0);
        HBox highTask = taskListView.getItems().get(1);
        HBox mediumTask = taskListView.getItems().get(2);
        HBox lowTask = taskListView.getItems().get(3);
        
        // Verify different styling
        assertNotEquals(urgentTask.getStyle(), highTask.getStyle());
        assertNotEquals(highTask.getStyle(), mediumTask.getStyle());
        assertNotEquals(mediumTask.getStyle(), lowTask.getStyle());
    }
    
    @Test
    void shouldCreateConsistentIndicators() {
        // Test that indicators are created consistently
        Circle circle1 = PriorityIndicators.createPriorityCircle(Priority.URGENT, 16);
        Circle circle2 = PriorityIndicators.createPriorityCircle(Priority.URGENT, 16);
        
        // Both circles should have the same fill color
        assertEquals(circle1.getFill(), circle2.getFill());
        assertEquals(circle1.getRadius(), circle2.getRadius());
    }
    
    @Test
    void shouldCreateIndicatorsForAllPriorities() {
        // Verify that indicators can be created for all priority levels
        for (Priority priority : Priority.values()) {
            HBox indicator = PriorityIndicators.createPriorityIndicator(priority, true);
            assertNotNull(indicator);
            assertFalse(indicator.getChildren().isEmpty());
        }
    }
}
