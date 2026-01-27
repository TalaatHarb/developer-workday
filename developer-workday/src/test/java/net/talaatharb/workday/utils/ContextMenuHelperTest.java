package net.talaatharb.workday.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

/**
 * Unit tests for ContextMenuHelper
 */
@ExtendWith(ApplicationExtension.class)
class ContextMenuHelperTest {
    
    private Task sampleTask;
    private List<Task> sampleTasks;
    
    @BeforeEach
    void setUp() {
        sampleTask = Task.builder()
            .title("Test Task")
            .description("Test Description")
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .dueDate(LocalDate.now())
            .build();
        
        Task task2 = Task.builder()
            .title("Task 2")
            .status(TaskStatus.TODO)
            .priority(Priority.HIGH)
            .build();
        
        sampleTasks = Arrays.asList(sampleTask, task2);
    }
    
    @Test
    @DisplayName("Should create task context menu with all standard actions")
    void shouldCreateTaskContextMenuWithAllActions() {
        // When
        ContextMenu menu = ContextMenuHelper.createTaskContextMenu(
            sampleTask,
            () -> {},
            () -> {},
            () -> {},
            () -> {},
            () -> {},
            () -> {}
        );
        
        // Then
        assertNotNull(menu);
        assertEquals(7, menu.getItems().size()); // 6 menu items + 1 separator
        
        // Verify menu item labels
        List<String> expectedLabels = Arrays.asList("Edit", "Complete", "Schedule", "Move to Category", "Duplicate", "Delete");
        for (int i = 0; i < 5; i++) {
            MenuItem item = menu.getItems().get(i);
            assertInstanceOf(MenuItem.class, item);
            assertTrue(expectedLabels.contains(item.getText()), "Expected menu item: " + item.getText());
        }
        
        // Verify delete item has red styling
        MenuItem deleteItem = (MenuItem) menu.getItems().get(6);
        assertEquals("Delete", deleteItem.getText());
        assertTrue(deleteItem.getStyle().contains("#e74c3c"), "Delete item should have red text");
    }
    
    @Test
    @DisplayName("Should trigger edit callback when Edit is selected")
    void shouldTriggerEditCallback() {
        // Given
        AtomicBoolean editCalled = new AtomicBoolean(false);
        ContextMenu menu = ContextMenuHelper.createTaskContextMenu(
            sampleTask,
            () -> editCalled.set(true),
            () -> {},
            () -> {},
            () -> {},
            () -> {},
            () -> {}
        );
        
        // When
        MenuItem editItem = menu.getItems().get(0);
        editItem.fire();
        
        // Then
        assertTrue(editCalled.get(), "Edit callback should be triggered");
    }
    
    @Test
    @DisplayName("Should trigger complete callback when Complete is selected")
    void shouldTriggerCompleteCallback() {
        // Given
        AtomicBoolean completeCalled = new AtomicBoolean(false);
        ContextMenu menu = ContextMenuHelper.createTaskContextMenu(
            sampleTask,
            () -> {},
            () -> completeCalled.set(true),
            () -> {},
            () -> {},
            () -> {},
            () -> {}
        );
        
        // When
        MenuItem completeItem = menu.getItems().get(1);
        completeItem.fire();
        
        // Then
        assertTrue(completeCalled.get(), "Complete callback should be triggered");
    }
    
    @Test
    @DisplayName("Should trigger delete callback when Delete is selected")
    void shouldTriggerDeleteCallback() {
        // Given
        AtomicBoolean deleteCalled = new AtomicBoolean(false);
        ContextMenu menu = ContextMenuHelper.createTaskContextMenu(
            sampleTask,
            () -> {},
            () -> {},
            () -> {},
            () -> {},
            () -> {},
            () -> deleteCalled.set(true)
        );
        
        // When
        MenuItem deleteItem = (MenuItem) menu.getItems().get(6);
        deleteItem.fire();
        
        // Then
        assertTrue(deleteCalled.get(), "Delete callback should be triggered");
    }
    
    @Test
    @DisplayName("Should create bulk task context menu with count in labels")
    void shouldCreateBulkTaskContextMenu() {
        // When
        ContextMenu menu = ContextMenuHelper.createBulkTaskContextMenu(
            sampleTasks,
            () -> {},
            () -> {},
            () -> {}
        );
        
        // Then
        assertNotNull(menu);
        assertEquals(4, menu.getItems().size()); // 3 menu items + 1 separator
        
        // Verify labels include count
        MenuItem completeAllItem = menu.getItems().get(0);
        assertEquals("Complete All (2)", completeAllItem.getText());
        
        MenuItem moveAllItem = menu.getItems().get(1);
        assertEquals("Move All (2)", moveAllItem.getText());
        
        MenuItem deleteAllItem = (MenuItem) menu.getItems().get(3);
        assertEquals("Delete All (2)", deleteAllItem.getText());
        assertTrue(deleteAllItem.getStyle().contains("#e74c3c"), "Delete All item should have red text");
    }
    
    @Test
    @DisplayName("Should trigger bulk complete callback")
    void shouldTriggerBulkCompleteCallback() {
        // Given
        AtomicBoolean completeAllCalled = new AtomicBoolean(false);
        ContextMenu menu = ContextMenuHelper.createBulkTaskContextMenu(
            sampleTasks,
            () -> completeAllCalled.set(true),
            () -> {},
            () -> {}
        );
        
        // When
        MenuItem completeAllItem = menu.getItems().get(0);
        completeAllItem.fire();
        
        // Then
        assertTrue(completeAllCalled.get(), "Complete All callback should be triggered");
    }
    
    @Test
    @DisplayName("Should trigger bulk delete callback")
    void shouldTriggerBulkDeleteCallback() {
        // Given
        AtomicBoolean deleteAllCalled = new AtomicBoolean(false);
        ContextMenu menu = ContextMenuHelper.createBulkTaskContextMenu(
            sampleTasks,
            () -> {},
            () -> {},
            () -> deleteAllCalled.set(true)
        );
        
        // When
        MenuItem deleteAllItem = (MenuItem) menu.getItems().get(3);
        deleteAllItem.fire();
        
        // Then
        assertTrue(deleteAllCalled.get(), "Delete All callback should be triggered");
    }
    
    @Test
    @DisplayName("Should create category context menu with all standard actions")
    void shouldCreateCategoryContextMenu() {
        // When
        ContextMenu menu = ContextMenuHelper.createCategoryContextMenu(
            "Work",
            () -> {},
            () -> {},
            () -> {}
        );
        
        // Then
        assertNotNull(menu);
        assertEquals(4, menu.getItems().size()); // 3 menu items + 1 separator
        
        // Verify menu item labels
        MenuItem editItem = menu.getItems().get(0);
        assertEquals("Edit", editItem.getText());
        
        MenuItem addTaskItem = menu.getItems().get(1);
        assertEquals("Add Task", addTaskItem.getText());
        
        MenuItem deleteItem = (MenuItem) menu.getItems().get(3);
        assertEquals("Delete", deleteItem.getText());
        assertTrue(deleteItem.getStyle().contains("#e74c3c"), "Delete item should have red text");
    }
    
    @Test
    @DisplayName("Should trigger category edit callback")
    void shouldTriggerCategoryEditCallback() {
        // Given
        AtomicBoolean editCalled = new AtomicBoolean(false);
        ContextMenu menu = ContextMenuHelper.createCategoryContextMenu(
            "Work",
            () -> editCalled.set(true),
            () -> {},
            () -> {}
        );
        
        // When
        MenuItem editItem = menu.getItems().get(0);
        editItem.fire();
        
        // Then
        assertTrue(editCalled.get(), "Edit callback should be triggered");
    }
    
    @Test
    @DisplayName("Should trigger add task to category callback")
    void shouldTriggerAddTaskCallback() {
        // Given
        AtomicBoolean addTaskCalled = new AtomicBoolean(false);
        ContextMenu menu = ContextMenuHelper.createCategoryContextMenu(
            "Work",
            () -> {},
            () -> addTaskCalled.set(true),
            () -> {}
        );
        
        // When
        MenuItem addTaskItem = menu.getItems().get(1);
        addTaskItem.fire();
        
        // Then
        assertTrue(addTaskCalled.get(), "Add Task callback should be triggered");
    }
    
    @Test
    @DisplayName("Should trigger category delete callback")
    void shouldTriggerCategoryDeleteCallback() {
        // Given
        AtomicBoolean deleteCalled = new AtomicBoolean(false);
        ContextMenu menu = ContextMenuHelper.createCategoryContextMenu(
            "Work",
            () -> {},
            () -> {},
            () -> deleteCalled.set(true)
        );
        
        // When
        MenuItem deleteItem = (MenuItem) menu.getItems().get(3);
        deleteItem.fire();
        
        // Then
        assertTrue(deleteCalled.get(), "Delete callback should be triggered");
    }
    
    @Test
    @DisplayName("Should handle null callbacks gracefully")
    void shouldHandleNullCallbacks() {
        // When - create menu with null callbacks
        ContextMenu taskMenu = ContextMenuHelper.createTaskContextMenu(
            sampleTask,
            null, null, null, null, null, null
        );
        
        ContextMenu bulkMenu = ContextMenuHelper.createBulkTaskContextMenu(
            sampleTasks,
            null, null, null
        );
        
        ContextMenu categoryMenu = ContextMenuHelper.createCategoryContextMenu(
            "Work",
            null, null, null
        );
        
        // Then - menus should be created and items should be clickable without error
        assertNotNull(taskMenu);
        assertNotNull(bulkMenu);
        assertNotNull(categoryMenu);
        
        // Fire events should not throw exceptions
        assertDoesNotThrow(() -> taskMenu.getItems().get(0).fire());
        assertDoesNotThrow(() -> bulkMenu.getItems().get(0).fire());
        assertDoesNotThrow(() -> categoryMenu.getItems().get(0).fire());
    }
}
