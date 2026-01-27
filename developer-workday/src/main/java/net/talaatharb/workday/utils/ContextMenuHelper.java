package net.talaatharb.workday.utils;

import java.util.List;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import net.talaatharb.workday.model.Task;

/**
 * Utility class for creating context menus for tasks and categories.
 * Provides standardized context menu creation with consistent styling and behavior.
 */
public class ContextMenuHelper {
    
    private ContextMenuHelper() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Create a context menu for a single task with standard actions.
     * 
     * @param task The task to create the menu for
     * @param onEdit Callback when Edit is selected
     * @param onComplete Callback when Complete is selected
     * @param onSchedule Callback when Schedule is selected
     * @param onMoveToCategory Callback when Move to Category is selected
     * @param onDuplicate Callback when Duplicate is selected
     * @param onDelete Callback when Delete is selected
     * @return A configured ContextMenu
     */
    public static ContextMenu createTaskContextMenu(
            Task task,
            Runnable onEdit,
            Runnable onComplete,
            Runnable onSchedule,
            Runnable onMoveToCategory,
            Runnable onDuplicate,
            Runnable onDelete) {
        
        ContextMenu menu = new ContextMenu();
        
        // Edit action
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> {
            if (onEdit != null) onEdit.run();
        });
        
        // Complete action
        MenuItem completeItem = new MenuItem("Complete");
        completeItem.setOnAction(e -> {
            if (onComplete != null) onComplete.run();
        });
        
        // Schedule action
        MenuItem scheduleItem = new MenuItem("Schedule");
        scheduleItem.setOnAction(e -> {
            if (onSchedule != null) onSchedule.run();
        });
        
        // Move to Category action
        MenuItem moveToCategoryItem = new MenuItem("Move to Category");
        moveToCategoryItem.setOnAction(e -> {
            if (onMoveToCategory != null) onMoveToCategory.run();
        });
        
        // Duplicate action
        MenuItem duplicateItem = new MenuItem("Duplicate");
        duplicateItem.setOnAction(e -> {
            if (onDuplicate != null) onDuplicate.run();
        });
        
        // Separator
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
        // Delete action
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setStyle("-fx-text-fill: #e74c3c;"); // Red text for destructive action
        deleteItem.setOnAction(e -> {
            if (onDelete != null) onDelete.run();
        });
        
        menu.getItems().addAll(
            editItem,
            completeItem,
            scheduleItem,
            moveToCategoryItem,
            duplicateItem,
            separator,
            deleteItem
        );
        
        return menu;
    }
    
    /**
     * Create a context menu for bulk task actions (multiple selected tasks).
     * 
     * @param tasks The list of selected tasks
     * @param onCompleteAll Callback when Complete All is selected
     * @param onMoveAll Callback when Move All is selected
     * @param onDeleteAll Callback when Delete All is selected
     * @return A configured ContextMenu for bulk operations
     */
    public static ContextMenu createBulkTaskContextMenu(
            List<Task> tasks,
            Runnable onCompleteAll,
            Runnable onMoveAll,
            Runnable onDeleteAll) {
        
        ContextMenu menu = new ContextMenu();
        
        // Complete All action
        MenuItem completeAllItem = new MenuItem(String.format("Complete All (%d)", tasks.size()));
        completeAllItem.setOnAction(e -> {
            if (onCompleteAll != null) onCompleteAll.run();
        });
        
        // Move All action
        MenuItem moveAllItem = new MenuItem(String.format("Move All (%d)", tasks.size()));
        moveAllItem.setOnAction(e -> {
            if (onMoveAll != null) onMoveAll.run();
        });
        
        // Separator
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
        // Delete All action
        MenuItem deleteAllItem = new MenuItem(String.format("Delete All (%d)", tasks.size()));
        deleteAllItem.setStyle("-fx-text-fill: #e74c3c;"); // Red text for destructive action
        deleteAllItem.setOnAction(e -> {
            if (onDeleteAll != null) onDeleteAll.run();
        });
        
        menu.getItems().addAll(
            completeAllItem,
            moveAllItem,
            separator,
            deleteAllItem
        );
        
        return menu;
    }
    
    /**
     * Create a context menu for a category with standard actions.
     * 
     * @param categoryName The name of the category
     * @param onEdit Callback when Edit is selected
     * @param onAddTask Callback when Add Task is selected
     * @param onDelete Callback when Delete is selected
     * @return A configured ContextMenu
     */
    public static ContextMenu createCategoryContextMenu(
            String categoryName,
            Runnable onEdit,
            Runnable onAddTask,
            Runnable onDelete) {
        
        ContextMenu menu = new ContextMenu();
        
        // Edit action
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> {
            if (onEdit != null) onEdit.run();
        });
        
        // Add Task action
        MenuItem addTaskItem = new MenuItem("Add Task");
        addTaskItem.setOnAction(e -> {
            if (onAddTask != null) onAddTask.run();
        });
        
        // Separator
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
        // Delete action
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setStyle("-fx-text-fill: #e74c3c;"); // Red text for destructive action
        deleteItem.setOnAction(e -> {
            if (onDelete != null) onDelete.run();
        });
        
        menu.getItems().addAll(
            editItem,
            addTaskItem,
            separator,
            deleteItem
        );
        
        return menu;
    }
}
