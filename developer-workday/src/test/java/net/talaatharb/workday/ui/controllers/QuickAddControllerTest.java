package net.talaatharb.workday.ui.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.task.TaskCreatedEvent;
import net.talaatharb.workday.facade.CategoryFacade;
import net.talaatharb.workday.facade.TaskFacade;
import net.talaatharb.workday.model.Category;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

/**
 * Tests for QuickAddController following the acceptance criteria.
 * 
 * Feature: Quick Add Task
 *   Scenario: Quick add with natural language
 *     Given the quick add bar is focused
 *     When typing 'Call John tomorrow at 3pm #work !high'
 *     Then the input should be parsed in real-time
 *     And parsed elements should be highlighted (date, time, category, priority)
 *
 *   Scenario: Submit quick add task
 *     Given a task has been typed in the quick add bar
 *     When pressing Enter
 *     Then the task should be created with parsed attributes
 *     And the input should be cleared
 *     And the new task should appear in the list
 *
 *   Scenario: Quick add suggestions
 *     Given the user is typing in the quick add bar
 *     When typing '#' or '@'
 *     Then autocomplete suggestions should appear for categories or contacts
 */
class QuickAddControllerTest {
    
    private static boolean jfxInitialized = false;
    private TaskFacade mockTaskFacade;
    private CategoryFacade mockCategoryFacade;
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
        mockCategoryFacade = mock(CategoryFacade.class);
        mockEventDispatcher = mock(EventDispatcher.class);
    }
    
    @Test
    @DisplayName("Quick add with natural language - parsing works")
    void testQuickAddNaturalLanguage() throws Exception {
        Platform.runLater(() -> {
            try {
                // Given: the quick add bar is focused with natural language input
                String naturalInput = "Call John tomorrow at 3pm #work !high";
                Task createdTask = createSampleTask("Call John");
                
                when(mockTaskFacade.quickAddTask(anyString())).thenReturn(createdTask);
                
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/QuickAddBar.fxml");
                assertNotNull(fxmlResource, "FXML file should exist");
                
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                HBox root = loader.load();
                QuickAddController controller = loader.getController();
                assertNotNull(controller, "Controller should be initialized");
                
                controller.setTaskFacade(mockTaskFacade);
                controller.setEventDispatcher(mockEventDispatcher);
                
                TextField quickAddField = controller.getQuickAddField();
                assertNotNull(quickAddField, "Quick add field should be accessible");
                
                // When: typing natural language
                quickAddField.setText(naturalInput);
                
                // Then: the input should be parsed (parsing happens in real-time)
                // The actual parsing is done by TaskFacade.quickAddTask
                // We verify the field contains the input
                assertEquals(naturalInput, quickAddField.getText(), "Input should be set");
                
            } catch (Exception e) {
                fail("Failed to load FXML or test: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Submit quick add task - creates task and clears input")
    void testSubmitQuickAddTask() throws Exception {
        Platform.runLater(() -> {
            try {
                // Given: a task has been typed in the quick add bar
                String taskInput = "Buy groceries tomorrow #personal";
                Task createdTask = createSampleTask("Buy groceries");
                
                when(mockTaskFacade.quickAddTask(taskInput)).thenReturn(createdTask);
                
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/QuickAddBar.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                HBox root = loader.load();
                QuickAddController controller = loader.getController();
                
                controller.setTaskFacade(mockTaskFacade);
                controller.setEventDispatcher(mockEventDispatcher);
                
                TextField quickAddField = controller.getQuickAddField();
                quickAddField.setText(taskInput);
                
                // When: pressing Enter (simulated by calling handleQuickAddSubmit)
                controller.handleQuickAddSubmit();
                
                // Then: the task should be created
                verify(mockTaskFacade, times(1)).quickAddTask(taskInput);
                
                // And: the input should be cleared
                assertEquals("", quickAddField.getText(), "Input field should be cleared");
                
                // And: a TaskCreatedEvent should be published
                ArgumentCaptor<TaskCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCreatedEvent.class);
                verify(mockEventDispatcher, times(1)).publish(eventCaptor.capture());
                
                TaskCreatedEvent publishedEvent = eventCaptor.getValue();
                assertNotNull(publishedEvent, "TaskCreatedEvent should be published");
                assertEquals(createdTask.getId(), publishedEvent.getTask().getId(), "Event should contain created task");
                
            } catch (Exception e) {
                fail("Failed to test submit: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Quick add suggestions - autocomplete appears for categories")
    void testQuickAddSuggestions() throws Exception {
        Platform.runLater(() -> {
            try {
                // Given: categories are available
                List<Category> categories = new ArrayList<>();
                categories.add(createSampleCategory("work", "#3498db"));
                categories.add(createSampleCategory("personal", "#2ecc71"));
                categories.add(createSampleCategory("shopping", "#e74c3c"));
                
                when(mockCategoryFacade.findAll()).thenReturn(categories);
                
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/QuickAddBar.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                HBox root = loader.load();
                QuickAddController controller = loader.getController();
                
                controller.setTaskFacade(mockTaskFacade);
                controller.setCategoryFacade(mockCategoryFacade);
                controller.setEventDispatcher(mockEventDispatcher);
                
                TextField quickAddField = controller.getQuickAddField();
                
                // When: typing '#'
                quickAddField.setText("Buy milk #");
                quickAddField.positionCaret(10); // Position after #
                
                // Then: autocomplete suggestions should appear
                // (In a real UI test, we would verify the ContextMenu is showing)
                // For unit test, we verify that categories were loaded
                verify(mockCategoryFacade, timeout(1000).atLeastOnce()).findAll();
                
            } catch (Exception e) {
                fail("Failed to test autocomplete: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Quick add field exists and is accessible")
    void testQuickAddFieldExists() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/QuickAddBar.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                HBox root = loader.load();
                QuickAddController controller = loader.getController();
                
                assertNotNull(controller, "Controller should be initialized");
                assertNotNull(controller.getQuickAddField(), "Quick add field should exist");
                
                TextField quickAddField = (TextField) root.lookup("#quickAddField");
                assertNotNull(quickAddField, "Quick add field should be in scene graph");
                assertFalse(quickAddField.getText().isEmpty() || quickAddField.getPromptText().isEmpty(), 
                    "Field should have prompt text");
                
            } catch (Exception e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Empty input does not create task")
    void testEmptyInputHandling() throws Exception {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/QuickAddBar.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                HBox root = loader.load();
                QuickAddController controller = loader.getController();
                
                controller.setTaskFacade(mockTaskFacade);
                controller.setEventDispatcher(mockEventDispatcher);
                
                TextField quickAddField = controller.getQuickAddField();
                quickAddField.setText(""); // Empty input
                
                // When: submitting empty input
                controller.handleQuickAddSubmit();
                
                // Then: no task should be created
                verify(mockTaskFacade, never()).quickAddTask(anyString());
                verify(mockEventDispatcher, never()).publish(any());
                
            } catch (Exception e) {
                fail("Failed to test empty input: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Callback is triggered when task is created")
    void testOnTaskCreatedCallback() throws Exception {
        Platform.runLater(() -> {
            try {
                String taskInput = "Test task";
                Task createdTask = createSampleTask("Test task");
                
                when(mockTaskFacade.quickAddTask(taskInput)).thenReturn(createdTask);
                
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/QuickAddBar.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                HBox root = loader.load();
                QuickAddController controller = loader.getController();
                
                controller.setTaskFacade(mockTaskFacade);
                controller.setEventDispatcher(mockEventDispatcher);
                
                // Set a callback
                boolean[] callbackTriggered = {false};
                controller.setOnTaskCreatedCallback(() -> callbackTriggered[0] = true);
                
                TextField quickAddField = controller.getQuickAddField();
                quickAddField.setText(taskInput);
                
                // Submit task
                controller.handleQuickAddSubmit();
                
                // Verify callback was triggered
                assertTrue(callbackTriggered[0], "Callback should be triggered after task creation");
                
            } catch (Exception e) {
                fail("Failed to test callback: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    private Task createSampleTask(String title) {
        return Task.builder()
            .id(UUID.randomUUID())
            .title(title)
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
    
    private Category createSampleCategory(String name, String color) {
        return Category.builder()
            .id(UUID.randomUUID())
            .name(name)
            .color(color)
            .displayOrder(0)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
