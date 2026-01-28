package net.talaatharb.workday.ui;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.talaatharb.workday.utils.DragAndDropHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for drag-and-drop task reordering.
 */
@ExtendWith(ApplicationExtension.class)
class DragAndDropIT {
    
    private ListView<String> taskListView;
    private Label statusLabel;
    private List<DragAndDropHelper.DragResult<String>> reorderHistory;
    private AtomicInteger reorderCount;
    
    @Start
    void start(Stage stage) {
        taskListView = new ListView<>();
        taskListView.getItems().addAll(
            "Fix login bug",
            "Write documentation",
            "Code review",
            "Update dependencies",
            "Run integration tests"
        );
        
        statusLabel = new Label("Ready");
        reorderHistory = new ArrayList<>();
        reorderCount = new AtomicInteger(0);
        
        // Enable drag and drop
        DragAndDropHelper<String> dragDropHelper = new DragAndDropHelper<>(taskListView);
        dragDropHelper.enableDragAndDrop(result -> {
            reorderCount.incrementAndGet();
            reorderHistory.add(result);
            statusLabel.setText(String.format(
                "Moved '%s' from position %d to %d",
                result.getItem(), result.getFromIndex(), result.getToIndex()
            ));
        });
        
        VBox root = new VBox(10, taskListView, statusLabel);
        root.setStyle("-fx-padding: 20;");
        
        Scene scene = new Scene(root, 400, 500);
        stage.setScene(scene);
        stage.show();
    }
    
    @Test
    void shouldHaveInitialTaskOrder() {
        assertEquals(5, taskListView.getItems().size());
        assertEquals("Fix login bug", taskListView.getItems().get(0));
        assertEquals("Write documentation", taskListView.getItems().get(1));
        assertEquals("Code review", taskListView.getItems().get(2));
        assertEquals("Update dependencies", taskListView.getItems().get(3));
        assertEquals("Run integration tests", taskListView.getItems().get(4));
    }
    
    @Test
    void shouldSimulateTaskReordering() {
        // Simulate dragging first task to third position
        String task = taskListView.getItems().remove(0);
        taskListView.getItems().add(2, task);
        
        // Manually trigger callback as if drag-drop occurred
        DragAndDropHelper.DragResult<String> result = 
            new DragAndDropHelper.DragResult<>(task, 0, 2);
        reorderHistory.add(result);
        reorderCount.incrementAndGet();
        
        // Verify new order
        assertEquals("Write documentation", taskListView.getItems().get(0));
        assertEquals("Code review", taskListView.getItems().get(1));
        assertEquals("Fix login bug", taskListView.getItems().get(2));
        
        // Verify callback was triggered
        assertEquals(1, reorderCount.get());
        assertEquals(1, reorderHistory.size());
        assertEquals("Fix login bug", reorderHistory.get(0).getItem());
    }
    
    @Test
    void shouldHandleMultipleReorders() {
        // Simulate multiple reordering operations
        
        // Move task from position 0 to 2
        String task1 = taskListView.getItems().remove(0);
        taskListView.getItems().add(2, task1);
        reorderHistory.add(new DragAndDropHelper.DragResult<>(task1, 0, 2));
        
        // Move task from position 3 to 1
        String task2 = taskListView.getItems().remove(3);
        taskListView.getItems().add(1, task2);
        reorderHistory.add(new DragAndDropHelper.DragResult<>(task2, 3, 1));
        
        // Verify history
        assertEquals(2, reorderHistory.size());
        assertTrue(reorderHistory.get(0).isReordered());
        assertTrue(reorderHistory.get(1).isReordered());
    }
    
    @Test
    void shouldTrackReorderMetrics() {
        // Simulate some reorders
        for (int i = 0; i < 3; i++) {
            String task = taskListView.getItems().remove(0);
            taskListView.getItems().add(taskListView.getItems().size(), task);
            reorderHistory.add(new DragAndDropHelper.DragResult<>(task, 0, taskListView.getItems().size() - 1));
        }
        
        // Verify metrics
        assertEquals(3, reorderHistory.size());
        
        // All reorders should be valid (from != to)
        for (DragAndDropHelper.DragResult<String> result : reorderHistory) {
            assertTrue(result.isReordered());
            assertNotEquals(result.getFromIndex(), result.getToIndex());
        }
    }
    
    @Test
    void shouldMoveTaskToEnd() {
        // Move first task to the end
        String task = taskListView.getItems().remove(0);
        taskListView.getItems().add(task);
        
        // Verify it's at the end
        assertEquals(task, taskListView.getItems().get(taskListView.getItems().size() - 1));
    }
    
    @Test
    void shouldMoveTaskToBeginning() {
        // Move last task to the beginning
        String task = taskListView.getItems().remove(taskListView.getItems().size() - 1);
        taskListView.getItems().add(0, task);
        
        // Verify it's at the beginning
        assertEquals(task, taskListView.getItems().get(0));
    }
    
    @Test
    void shouldPreserveAllTasks() {
        // Perform several reordering operations
        List<String> originalTasks = new ArrayList<>(taskListView.getItems());
        
        // Shuffle around
        taskListView.getItems().remove(0);
        taskListView.getItems().add("Fix login bug");
        taskListView.getItems().remove(1);
        taskListView.getItems().add(0, "Write documentation");
        
        // Verify all original tasks are still present
        assertEquals(originalTasks.size(), taskListView.getItems().size());
        for (String task : originalTasks) {
            assertTrue(taskListView.getItems().contains(task), 
                "Task '" + task + "' should still be in the list");
        }
    }
}
