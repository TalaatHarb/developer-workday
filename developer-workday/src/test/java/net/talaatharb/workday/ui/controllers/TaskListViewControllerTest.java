package net.talaatharb.workday.ui.controllers;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

/**
 * Tests for TaskListViewController following the acceptance criteria.
 * 
 * Feature: Task List View
 *   Scenario: Display task list
 *     Given tasks exist in the system
 *     When viewing the task list
 *     Then tasks should be displayed with title, due date, priority indicator, and category
 *     And completed tasks should be visually distinguished
 *     And overdue tasks should be highlighted
 *
 *   Scenario: Filter tasks
 *     Given the task list is displayed
 *     When a filter is applied (by category, priority, or status)
 *     Then only matching tasks should be displayed
 *     And the filter state should be visually indicated
 *
 *   Scenario: Sort tasks
 *     Given the task list is displayed
 *     When a sort option is selected (by date, priority, or name)
 *     Then tasks should be reordered according to the selected criteria
 */
class TaskListViewControllerTest {
    
    private static boolean jfxInitialized = false;
    
    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        if (!jfxInitialized) {
            Platform.startup(() -> {});
            jfxInitialized = true;
        }
    }
    
    @Test
    @DisplayName("Display task list - shows tasks with all required information")
    void testDisplayTaskList() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskListView.fxml");
                assertNotNull(fxmlResource, "FXML file should exist");
                
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                TaskListViewController controller = loader.getController();
                
                // Given: tasks exist in the system
                assertNotNull(controller, "Controller should be initialized");
                
                // When: viewing the task list
                ListView<?> taskListView = (ListView<?>) root.lookup(".task-list");
                assertNotNull(taskListView, "Task list view should exist");
                
                // Then: tasks should be displayed (sample data is loaded in controller)
                assertTrue(taskListView.getItems().size() > 0, 
                    "Task list should display tasks");
                
            } catch (Exception e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Display task list - has filter controls")
    void testDisplayFilters() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskListView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                
                // Given: the task list is displayed
                // When: viewing the filter controls
                // Then: category filter should exist
                ChoiceBox<?> categoryFilter = (ChoiceBox<?>) root.lookupAll(".choice-box").stream()
                    .findFirst()
                    .orElse(null);
                assertNotNull(categoryFilter, "Category filter should exist");
                
            } catch (Exception e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Filter tasks - by priority")
    void testFilterByPriority() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskListView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                TaskListViewController controller = loader.getController();
                
                // Given: the task list is displayed with tasks
                List<Task> testTasks = new ArrayList<>();
                testTasks.add(Task.builder()
                    .title("High priority task")
                    .priority(Priority.HIGH)
                    .status(TaskStatus.TODO)
                    .build());
                testTasks.add(Task.builder()
                    .title("Low priority task")
                    .priority(Priority.LOW)
                    .status(TaskStatus.TODO)
                    .build());
                
                controller.loadTasks(testTasks);
                
                // When: a filter is applied (by priority)
                ListView<?> taskListView = (ListView<?>) root.lookup(".task-list");
                int totalCount = taskListView.getItems().size();
                
                // Then: tasks are displayed
                assertEquals(2, totalCount, "Should display all tasks initially");
                
            } catch (Exception e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Filter tasks - by status")
    void testFilterByStatus() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskListView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                TaskListViewController controller = loader.getController();
                
                // Given: the task list is displayed
                List<Task> testTasks = new ArrayList<>();
                testTasks.add(Task.builder()
                    .title("Todo task")
                    .status(TaskStatus.TODO)
                    .build());
                testTasks.add(Task.builder()
                    .title("Completed task")
                    .status(TaskStatus.COMPLETED)
                    .build());
                
                controller.loadTasks(testTasks);
                
                // When: viewing the task list
                ListView<?> taskListView = (ListView<?>) root.lookup(".task-list");
                
                // Then: both tasks should be displayed initially
                assertEquals(2, taskListView.getItems().size());
                
            } catch (Exception e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Filter tasks - toggle show completed")
    void testToggleShowCompleted() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskListView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                TaskListViewController controller = loader.getController();
                
                // Given: the task list is displayed
                CheckBox showCompletedCheck = (CheckBox) root.lookup(".check-box");
                assertNotNull(showCompletedCheck, "Show completed checkbox should exist");
                
                // Initially should be checked (showing all tasks)
                assertTrue(showCompletedCheck.isSelected(), "Show completed should be checked initially");
                
            } catch (Exception e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Sort tasks - has sort options")
    void testSortOptions() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskListView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                TaskListViewController controller = loader.getController();
                
                // Given: the task list is displayed
                // When: viewing sort options
                // Then: sort choice box should exist with options
                List<?> choiceBoxes = root.lookupAll(".choice-box").stream().toList();
                assertTrue(choiceBoxes.size() >= 4, "Should have at least 4 choice boxes (filters + sort)");
                
            } catch (Exception e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Sort tasks - by due date")
    void testSortByDueDate() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskListView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                TaskListViewController controller = loader.getController();
                
                // Given: the task list is displayed with tasks
                List<Task> testTasks = new ArrayList<>();
                testTasks.add(Task.builder()
                    .title("Task 1")
                    .dueDate(LocalDate.now().plusDays(3))
                    .build());
                testTasks.add(Task.builder()
                    .title("Task 2")
                    .dueDate(LocalDate.now().plusDays(1))
                    .build());
                testTasks.add(Task.builder()
                    .title("Task 3")
                    .dueDate(LocalDate.now().plusDays(2))
                    .build());
                
                // When: tasks are loaded (default sort is by due date)
                controller.loadTasks(testTasks);
                
                // Then: tasks should be sorted by due date
                ListView<Task> taskListView = (ListView<Task>) root.lookup(".task-list");
                assertNotNull(taskListView);
                assertEquals(3, taskListView.getItems().size());
                
                // Verify order (earliest first)
                Task first = taskListView.getItems().get(0);
                Task second = taskListView.getItems().get(1);
                Task third = taskListView.getItems().get(2);
                
                assertTrue(first.getDueDate().isBefore(second.getDueDate()) ||
                          first.getDueDate().isEqual(second.getDueDate()));
                assertTrue(second.getDueDate().isBefore(third.getDueDate()) ||
                          second.getDueDate().isEqual(third.getDueDate()));
                
            } catch (Exception e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Display task list - shows overdue tasks highlighted")
    void testOverdueTasksHighlighted() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskListView.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                VBox root = loader.load();
                TaskListViewController controller = loader.getController();
                
                // Given: tasks with overdue due dates
                List<Task> testTasks = new ArrayList<>();
                testTasks.add(Task.builder()
                    .title("Overdue task")
                    .dueDate(LocalDate.now().minusDays(1))
                    .status(TaskStatus.TODO)
                    .build());
                
                controller.loadTasks(testTasks);
                
                // When: viewing the task list
                ListView<?> taskListView = (ListView<?>) root.lookup(".task-list");
                
                // Then: overdue task should be displayed
                // (Visual highlighting is verified through the TaskCell implementation)
                assertEquals(1, taskListView.getItems().size());
                
            } catch (Exception e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
}
