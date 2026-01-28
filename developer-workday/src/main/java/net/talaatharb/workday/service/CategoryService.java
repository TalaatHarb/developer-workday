package net.talaatharb.workday.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.category.*;
import net.talaatharb.workday.model.Category;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.repository.CategoryRepository;
import net.talaatharb.workday.repository.TaskRepository;

/**
 * Service class for category business logic operations.
 * Handles category creation, updates, deletion, reordering, and hierarchy management.
 */
@Slf4j
@RequiredArgsConstructor
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;
    private final EventDispatcher eventDispatcher;
    
    /**
     * Create a new category
     */
    public Category createCategory(Category category) {
        log.debug("Creating new category: {}", category.getName());
        
        // Save via repository
        Category savedCategory = categoryRepository.save(category);
        
        // Publish event
        eventDispatcher.publish(new CategoryCreatedEvent(savedCategory));
        
        log.info("Created category: {} (ID: {})", savedCategory.getName(), savedCategory.getId());
        return savedCategory;
    }
    
    /**
     * Update an existing category
     */
    public Category updateCategory(Category updatedCategory) {
        log.debug("Updating category: {}", updatedCategory.getId());
        
        // Get old category for event
        Optional<Category> oldCategoryOpt = categoryRepository.findById(updatedCategory.getId());
        if (oldCategoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Category not found: " + updatedCategory.getId());
        }
        
        Category oldCategory = oldCategoryOpt.get();
        
        // Save updated category
        Category savedCategory = categoryRepository.save(updatedCategory);
        
        // Publish event with before and after states
        eventDispatcher.publish(new CategoryUpdatedEvent(oldCategory, savedCategory));
        
        log.info("Updated category: {}", savedCategory.getId());
        return savedCategory;
    }
    
    /**
     * Delete a category
     * For the test implementation, we'll use a simple approach with task handling strategy
     */
    public void deleteCategory(UUID categoryId, TaskHandlingStrategy strategy) {
        log.debug("Deleting category: {} with strategy: {}", categoryId, strategy);
        
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Category not found: " + categoryId);
        }
        
        Category category = categoryOpt.get();
        
        // Find all tasks in this category
        List<Task> affectedTasks = taskRepository.findByCategoryId(categoryId);
        List<UUID> affectedTaskIds = affectedTasks.stream()
            .map(Task::getId)
            .collect(Collectors.toList());
        
        // Handle tasks according to strategy
        handleTasksBeforeCategoryDeletion(affectedTasks, strategy);
        
        // Delete the category
        categoryRepository.deleteById(categoryId);
        
        // Publish event with affected task information
        eventDispatcher.publish(new CategoryDeletedEvent(category, affectedTaskIds));
        
        log.info("Deleted category: {} affecting {} tasks", categoryId, affectedTaskIds.size());
    }
    
    /**
     * Handle tasks when their category is deleted
     */
    private void handleTasksBeforeCategoryDeletion(List<Task> tasks, TaskHandlingStrategy strategy) {
        switch (strategy) {
            case MOVE_TO_DEFAULT:
                // Find the default category
                UUID defaultCategoryId = getDefaultCategoryId();
                for (Task task : tasks) {
                    task.setCategoryId(defaultCategoryId);
                    taskRepository.save(task);
                }
                log.debug("Moved {} tasks to default category", tasks.size());
                break;
                
            case DELETE_TASKS:
                for (Task task : tasks) {
                    taskRepository.deleteById(task.getId());
                }
                log.debug("Deleted {} tasks", tasks.size());
                break;
                
            case LEAVE_ORPHANED:
                // Leave tasks with null category
                for (Task task : tasks) {
                    task.setCategoryId(null);
                    taskRepository.save(task);
                }
                log.debug("Left {} tasks orphaned", tasks.size());
                break;
        }
    }
    
    /**
     * Get the default category ID, or create one if it doesn't exist
     */
    private UUID getDefaultCategoryId() {
        List<Category> defaultCategories = categoryRepository.findDefaultCategories();
        
        if (!defaultCategories.isEmpty()) {
            return defaultCategories.get(0).getId();
        }
        
        // Create a default category if none exists
        Category defaultCategory = Category.builder()
            .name("Uncategorized")
            .description("Default category for uncategorized tasks")
            .isDefault(true)
            .build();
        
        Category savedCategory = categoryRepository.save(defaultCategory);
        log.info("Created default category: {}", savedCategory.getId());
        return savedCategory.getId();
    }
    
    /**
     * Reorder categories
     */
    public void reorderCategories(List<UUID> categoryIds) {
        log.debug("Reordering {} categories", categoryIds.size());
        
        // Update sort order for each category
        for (int i = 0; i < categoryIds.size(); i++) {
            UUID categoryId = categoryIds.get(i);
            Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
            
            if (categoryOpt.isPresent()) {
                Category category = categoryOpt.get();
                category.setSortOrder(i);
                categoryRepository.save(category);
            }
        }
        
        // Publish event
        eventDispatcher.publish(new CategoryReorderedEvent(categoryIds));
        
        log.info("Reordered {} categories", categoryIds.size());
    }
    
    /**
     * Find category by ID
     */
    public Optional<Category> findById(UUID categoryId) {
        return categoryRepository.findById(categoryId);
    }
    
    /**
     * Find all categories
     */
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }
    
    /**
     * Find all categories ordered by sort order
     */
    public List<Category> findAllOrdered() {
        return categoryRepository.findAllOrdered();
    }
    
    /**
     * Find root categories (categories without parent)
     */
    public List<Category> findRootCategories() {
        return categoryRepository.findRootCategories();
    }
    
    /**
     * Find subcategories of a parent category
     */
    public List<Category> findByParentId(UUID parentId) {
        return categoryRepository.findByParentId(parentId);
    }
    
    /**
     * Find default categories
     */
    public List<Category> findDefaultCategories() {
        return categoryRepository.findDefaultCategories();
    }
    
    /**
     * Strategy for handling tasks when deleting their category
     */
    public enum TaskHandlingStrategy {
        MOVE_TO_DEFAULT,    // Move tasks to default category
        DELETE_TASKS,       // Delete all tasks in the category
        LEAVE_ORPHANED      // Leave tasks without a category
    }
}
