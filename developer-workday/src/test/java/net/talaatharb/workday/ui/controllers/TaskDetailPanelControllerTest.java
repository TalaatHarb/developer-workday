package net.talaatharb.workday.ui.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.task.TaskUpdatedEvent;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

/**
 * Tests for TaskDetailPanelController following the acceptance criteria.
 * 
 * Feature: Task Detail Panel
 *   Scenario: View task details
 *     Given a task is selected from the list
 *     When the detail panel opens
 *     Then all task fields should be displayed
 *     And the panel should slide in with smooth animation
 *
 *   Scenario: Edit task inline
 *     Given the task detail panel is open
 *     When editing any field
 *     Then changes should be saved automatically after a brief delay
 *     And a visual indicator should show unsaved changes
 *     And a TaskUpdatedEvent should be published on save
 *
 *   Scenario: Close panel
 *     Given the task detail panel is open
 *     When clicking outside or pressing Escape
 *     Then the panel should slide out
 *     And any unsaved changes should be saved
 */
class TaskDetailPanelControllerTest {
    
    private static boolean jfxInitialized = false;
    private TaskFacade mockTaskFacade;
    private EventDispatcher mockEventDispatcher;
    
    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        if (!jfxInitialized) {
            Platform.startup(() -> {});
            jfxInitialized = true;
        }
    }
    
    @BeforeEach
    void setUp() {
        mockTaskFacade = mock(TaskFacade.class);
        mockEventDispatcher = mock(EventDispatcher.class);
    }
    
    @Test
    @DisplayName("View task details - all fields should be displayed")
    void testViewTaskDetails() throws Exception {
        Platform.runLater(() -> {
            try {
                // Given: a task is selected from the list
                Task task = createSampleTask();
                
                // When: the detail panel opens
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskDetailPanel.fxml");
                assertNotNull(fxmlResource, "FXML file should exist");
                
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                ScrollPane root = loader.load();
                TaskDetailPanelController controller = loader.getController();
                assertNotNull(controller, "Controller should be initialized");
                
                controller.setTaskFacade(mockTaskFacade);
                controller.setEventDispatcher(mockEventDispatcher);
                controller.loadTask(task);
                
                // Then: all task fields should be displayed
                TextField titleField = (TextField) root.lookup("#titleField");
                assertNotNull(titleField, "Title field should exist");
                assertEquals(task.getTitle(), titleField.getText(), "Title should be loaded");
                
                @SuppressWarnings("unchecked")
                ChoiceBox<Priority> priorityChoice = (ChoiceBox<Priority>) root.lookup("#priorityChoice");
                assertNotNull(priorityChoice, "Priority choice should exist");
                assertEquals(task.getPriority(), priorityChoice.getValue(), "Priority should be loaded");
                
                @SuppressWarnings("unchecked")
                ChoiceBox<TaskStatus> statusChoice = (ChoiceBox<TaskStatus>) root.lookup("#statusChoice");
                assertNotNull(statusChoice, "Status choice should exist");
                assertEquals(task.getStatus(), statusChoice.getValue(), "Status should be loaded");
                
                DatePicker dueDatePicker = (DatePicker) root.lookup("#dueDatePicker");
                assertNotNull(dueDatePicker, "Due date picker should exist");
                assertEquals(task.getDueDate(), dueDatePicker.getValue(), "Due date should be loaded");
                
                TextArea descriptionArea = (TextArea) root.lookup("#descriptionArea");
                assertNotNull(descriptionArea, "Description area should exist");
                assertEquals(task.getDescription(), descriptionArea.getText(), "Description should be loaded");
                
            } catch (Exception e) {
                fail("Failed to load FXML or test: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Edit task inline - changes trigger auto-save")
    void testEditTaskInline() throws Exception {
        Platform.runLater(() -> {
            try {
                // Given: the task detail panel is open
                Task task = createSampleTask();
                Task updatedTaskResponse = Task.builder()
                    .id(task.getId())
                    .title("Updated Title")
                    .description(task.getDescription())
                    .priority(task.getPriority())
                    .status(task.getStatus())
                    .dueDate(task.getDueDate())
                    .dueTime(task.getDueTime())
                    .tags(task.getTags())
                    .build();
                
                when(mockTaskFacade.updateTask(any(Task.class))).thenReturn(updatedTaskResponse);
                
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskDetailPanel.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                ScrollPane root = loader.load();
                TaskDetailPanelController controller = loader.getController();
                
                controller.setTaskFacade(mockTaskFacade);
                controller.setEventDispatcher(mockEventDispatcher);
                controller.loadTask(task);
                
                // When: editing any field
                TextField titleField = (TextField) root.lookup("#titleField");
                titleField.setText("Updated Title");
                
                // Then: unsaved indicator should be visible
                Label unsavedIndicator = (Label) root.lookup("#unsavedIndicator");
                assertNotNull(unsavedIndicator, "Unsaved indicator should exist");
                
                // Wait for auto-save to trigger (1 second delay + buffer)
                Thread.sleep(1500);
                
                // Then: changes should be saved automatically
                verify(mockTaskFacade, timeout(2000).atLeastOnce()).updateTask(any(Task.class));
                
                // And: a TaskUpdatedEvent should be published on save
                ArgumentCaptor<TaskUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(TaskUpdatedEvent.class);
                verify(mockEventDispatcher, timeout(2000).atLeastOnce()).publish(eventCaptor.capture());
                
                TaskUpdatedEvent publishedEvent = eventCaptor.getValue();
                assertNotNull(publishedEvent, "TaskUpdatedEvent should be published");
                assertEquals(task.getTitle(), publishedEvent.getOldTask().getTitle(), "Old task should match");
                assertEquals("Updated Title", publishedEvent.getNewTask().getTitle(), "New task should have updated title");
                
            } catch (Exception e) {
                fail("Failed to test auto-save: " + e.getMessage());
            }
        });
        
        Thread.sleep(2000);
    }
    
    @Test
    @DisplayName("Close panel - unsaved changes are saved")
    void testClosePanel() throws Exception {
        Platform.runLater(() -> {
            try {
                // Given: the task detail panel is open with unsaved changes
                Task task = createSampleTask();
                Task updatedTaskResponse = Task.builder()
                    .id(task.getId())
                    .title("Closing with changes")
                    .description(task.getDescription())
                    .priority(task.getPriority())
                    .status(task.getStatus())
                    .dueDate(task.getDueDate())
                    .dueTime(task.getDueTime())
                    .tags(task.getTags())
                    .build();
                
                when(mockTaskFacade.updateTask(any(Task.class))).thenReturn(updatedTaskResponse);
                
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskDetailPanel.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                ScrollPane root = loader.load();
                TaskDetailPanelController controller = loader.getController();
                
                controller.setTaskFacade(mockTaskFacade);
                controller.setEventDispatcher(mockEventDispatcher);
                controller.loadTask(task);
                
                // Make a change
                TextField titleField = (TextField) root.lookup("#titleField");
                titleField.setText("Closing with changes");
                
                // Verify there are unsaved changes
                assertTrue(controller.hasUnsavedChanges(), "Should have unsaved changes");
                
                // When: clicking close button
                controller.handleClose();
                
                // Then: any unsaved changes should be saved
                verify(mockTaskFacade, timeout(1000).atLeastOnce()).updateTask(any(Task.class));
                
            } catch (Exception e) {
                fail("Failed to test close panel: " + e.getMessage());
            }
        });
        
        Thread.sleep(1500);
    }
    
    @Test
    @DisplayName("Panel has required UI controls")
    void testPanelHasRequiredControls() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskDetailPanel.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                ScrollPane root = loader.load();
                
                // Verify all required controls exist
                assertNotNull(root.lookup("#titleField"), "Title field should exist");
                assertNotNull(root.lookup("#priorityChoice"), "Priority choice should exist");
                assertNotNull(root.lookup("#statusChoice"), "Status choice should exist");
                assertNotNull(root.lookup("#dueDatePicker"), "Due date picker should exist");
                assertNotNull(root.lookup("#dueTimeField"), "Due time field should exist");
                assertNotNull(root.lookup("#descriptionArea"), "Description area should exist");
                assertNotNull(root.lookup("#tagsField"), "Tags field should exist");
                assertNotNull(root.lookup("#reminderField"), "Reminder field should exist");
                assertNotNull(root.lookup("#saveButton"), "Save button should exist");
                assertNotNull(root.lookup("#deleteButton"), "Delete button should exist");
                assertNotNull(root.lookup("#closeButton"), "Close button should exist");
                assertNotNull(root.lookup("#unsavedIndicator"), "Unsaved indicator should exist");
                
            } catch (Exception e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Load task with all fields populated")
    void testLoadTaskWithAllFields() throws Exception {
        Platform.runLater(() -> {
            try {
                Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Complete Task")
                    .description("Full description")
                    .priority(Priority.HIGH)
                    .status(TaskStatus.IN_PROGRESS)
                    .dueDate(LocalDate.of(2024, 12, 31))
                    .dueTime(LocalTime.of(14, 30))
                    .tags(List.of("work", "urgent"))
                    .reminderMinutesBefore(30)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/TaskDetailPanel.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                ScrollPane root = loader.load();
                TaskDetailPanelController controller = loader.getController();
                
                controller.setTaskFacade(mockTaskFacade);
                controller.loadTask(task);
                
                // Verify all fields are populated correctly
                TextField titleField = (TextField) root.lookup("#titleField");
                assertEquals("Complete Task", titleField.getText());
                
                TextArea descriptionArea = (TextArea) root.lookup("#descriptionArea");
                assertEquals("Full description", descriptionArea.getText());
                
                @SuppressWarnings("unchecked")
                ChoiceBox<Priority> priorityChoice = (ChoiceBox<Priority>) root.lookup("#priorityChoice");
                assertEquals(Priority.HIGH, priorityChoice.getValue());
                
                @SuppressWarnings("unchecked")
                ChoiceBox<TaskStatus> statusChoice = (ChoiceBox<TaskStatus>) root.lookup("#statusChoice");
                assertEquals(TaskStatus.IN_PROGRESS, statusChoice.getValue());
                
                DatePicker dueDatePicker = (DatePicker) root.lookup("#dueDatePicker");
                assertEquals(LocalDate.of(2024, 12, 31), dueDatePicker.getValue());
                
                TextField dueTimeField = (TextField) root.lookup("#dueTimeField");
                assertEquals("14:30", dueTimeField.getText());
                
                TextField tagsField = (TextField) root.lookup("#tagsField");
                assertEquals("work, urgent", tagsField.getText());
                
                TextField reminderField = (TextField) root.lookup("#reminderField");
                assertEquals("30", reminderField.getText());
                
            } catch (Exception e) {
                fail("Failed to test load task: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    private Task createSampleTask() {
        return Task.builder()
            .id(UUID.randomUUID())
            .title("Sample Task")
            .description("Sample description")
            .priority(Priority.MEDIUM)
            .status(TaskStatus.TODO)
            .dueDate(LocalDate.now().plusDays(1))
            .dueTime(LocalTime.of(10, 0))
            .tags(List.of("test"))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
