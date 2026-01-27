package net.talaatharb.workday.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.EventLogger;
import net.talaatharb.workday.event.category.*;
import net.talaatharb.workday.model.Category;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.CategoryRepository;
import net.talaatharb.workday.repository.TaskRepository;
import net.talaatharb.workday.service.CategoryService.TaskHandlingStrategy;

/**
 * Tests for CategoryService following the acceptance criteria.
 */
class CategoryServiceTest {
    
    private DB database;
    private CategoryRepository categoryRepository;
    private TaskRepository taskRepository;
    private EventDispatcher eventDispatcher;
    private EventLogger eventLogger;
    private CategoryService categoryService;
    private File dbFile;
    
    @BeforeEach
    void setUp() {
        dbFile = new File("test-categoryservice-" + UUID.randomUUID() + ".db");
        database = DBMaker.fileDB(dbFile)
            .transactionEnable()
            .make();
        categoryRepository = new CategoryRepository(database);
        taskRepository = new TaskRepository(database);
        eventLogger = new EventLogger();
        eventDispatcher = new EventDispatcher(eventLogger);
        categoryService = new CategoryService(categoryRepository, taskRepository, eventDispatcher);
    }
    
    @AfterEach
    void tearDown() {
        if (database != null && !database.isClosed()) {
            database.close();
        }
        if (dbFile != null && dbFile.exists()) {
            dbFile.delete();
        }
    }
    
    @Test
    @DisplayName("Create a new category - saves via repository and publishes event")
    void testCreateCategory() throws InterruptedException {
        // Given: valid category creation data
        CountDownLatch latch = new CountDownLatch(1);
        final CategoryCreatedEvent[] capturedEvent = new CategoryCreatedEvent[1];
        
        eventDispatcher.subscribe(CategoryCreatedEvent.class, event -> {
            capturedEvent[0] = event;
            latch.countDown();
        });
        
        Category category = Category.builder()
            .name("Work")
            .description("Work-related tasks")
            .color("#FF5733")
            .icon("briefcase")
            .isDefault(false)
            .build();
        
        // When: createCategory is called
        Category createdCategory = categoryService.createCategory(category);
        
        // Then: the category should be saved via repository
        assertNotNull(createdCategory.getId(), "Category should have an ID");
        assertNotNull(createdCategory.getCreatedAt(), "Category should have createdAt timestamp");
        assertTrue(categoryRepository.existsById(createdCategory.getId()), 
            "Category should be in repository");
        
        // And: a CategoryCreatedEvent should be published
        assertTrue(latch.await(1, TimeUnit.SECONDS), "CategoryCreatedEvent should be published");
        assertNotNull(capturedEvent[0], "Event should be captured");
        assertEquals(createdCategory.getId(), capturedEvent[0].getCategory().getId());
        assertEquals("Work", capturedEvent[0].getCategory().getName());
    }
    
    @Test
    @DisplayName("Delete category with tasks - moves to default category")
    void testDeleteCategoryWithTasks_MoveToDefault() throws InterruptedException {
        // Given: a category containing tasks
        Category category = categoryRepository.save(Category.builder()
            .name("Old Category")
            .build());
        
        Task task1 = taskRepository.save(Task.builder()
            .title("Task 1")
            .status(TaskStatus.TODO)
            .categoryId(category.getId())
            .build());
        
        Task task2 = taskRepository.save(Task.builder()
            .title("Task 2")
            .status(TaskStatus.TODO)
            .categoryId(category.getId())
            .build());
        
        CountDownLatch latch = new CountDownLatch(1);
        final CategoryDeletedEvent[] capturedEvent = new CategoryDeletedEvent[1];
        
        eventDispatcher.subscribe(CategoryDeletedEvent.class, event -> {
            capturedEvent[0] = event;
            latch.countDown();
        });
        
        // When: deleteCategory is called with MOVE_TO_DEFAULT strategy
        categoryService.deleteCategory(category.getId(), TaskHandlingStrategy.MOVE_TO_DEFAULT);
        
        // Then: tasks should be handled according to user choice (moved to default)
        Task updatedTask1 = taskRepository.findById(task1.getId()).orElse(null);
        Task updatedTask2 = taskRepository.findById(task2.getId()).orElse(null);
        
        assertNotNull(updatedTask1);
        assertNotNull(updatedTask2);
        assertNotEquals(category.getId(), updatedTask1.getCategoryId(), 
            "Task should be moved from old category");
        assertNotNull(updatedTask1.getCategoryId(), "Task should have a category");
        
        // And: the category should be deleted
        assertFalse(categoryRepository.existsById(category.getId()), 
            "Category should be deleted");
        
        // And: a CategoryDeletedEvent should be published
        assertTrue(latch.await(1, TimeUnit.SECONDS), "CategoryDeletedEvent should be published");
        assertNotNull(capturedEvent[0]);
        assertEquals(category.getId(), capturedEvent[0].getCategory().getId());
        assertEquals(2, capturedEvent[0].getAffectedTaskIds().size(), 
            "Should report 2 affected tasks");
    }
    
    @Test
    @DisplayName("Delete category with tasks - deletes tasks")
    void testDeleteCategoryWithTasks_DeleteTasks() {
        Category category = categoryRepository.save(Category.builder()
            .name("Category to Delete")
            .build());
        
        Task task1 = taskRepository.save(Task.builder()
            .title("Task 1")
            .categoryId(category.getId())
            .build());
        
        Task task2 = taskRepository.save(Task.builder()
            .title("Task 2")
            .categoryId(category.getId())
            .build());
        
        categoryService.deleteCategory(category.getId(), TaskHandlingStrategy.DELETE_TASKS);
        
        assertFalse(categoryRepository.existsById(category.getId()));
        assertFalse(taskRepository.existsById(task1.getId()), "Task 1 should be deleted");
        assertFalse(taskRepository.existsById(task2.getId()), "Task 2 should be deleted");
    }
    
    @Test
    @DisplayName("Delete category with tasks - leaves orphaned")
    void testDeleteCategoryWithTasks_LeaveOrphaned() {
        Category category = categoryRepository.save(Category.builder()
            .name("Category")
            .build());
        
        Task task = taskRepository.save(Task.builder()
            .title("Task")
            .categoryId(category.getId())
            .build());
        
        categoryService.deleteCategory(category.getId(), TaskHandlingStrategy.LEAVE_ORPHANED);
        
        assertFalse(categoryRepository.existsById(category.getId()));
        
        Task orphanedTask = taskRepository.findById(task.getId()).orElse(null);
        assertNotNull(orphanedTask, "Task should still exist");
        assertNull(orphanedTask.getCategoryId(), "Task should have no category");
    }
    
    @Test
    @DisplayName("Update category publishes CategoryUpdatedEvent")
    void testUpdateCategory() throws InterruptedException {
        Category category = categoryRepository.save(Category.builder()
            .name("Original Name")
            .description("Original description")
            .build());
        
        CountDownLatch latch = new CountDownLatch(1);
        final CategoryUpdatedEvent[] capturedEvent = new CategoryUpdatedEvent[1];
        
        eventDispatcher.subscribe(CategoryUpdatedEvent.class, event -> {
            capturedEvent[0] = event;
            latch.countDown();
        });
        
        category.setName("Updated Name");
        category.setDescription("Updated description");
        
        Category updatedCategory = categoryService.updateCategory(category);
        
        assertEquals("Updated Name", updatedCategory.getName());
        assertEquals("Updated description", updatedCategory.getDescription());
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(capturedEvent[0]);
        assertEquals("Original Name", capturedEvent[0].getOldCategory().getName());
        assertEquals("Updated Name", capturedEvent[0].getNewCategory().getName());
    }
    
    @Test
    @DisplayName("Reorder categories publishes CategoryReorderedEvent")
    void testReorderCategories() throws InterruptedException {
        Category cat1 = categoryRepository.save(Category.builder().name("Cat 1").sortOrder(0).build());
        Category cat2 = categoryRepository.save(Category.builder().name("Cat 2").sortOrder(1).build());
        Category cat3 = categoryRepository.save(Category.builder().name("Cat 3").sortOrder(2).build());
        
        CountDownLatch latch = new CountDownLatch(1);
        final CategoryReorderedEvent[] capturedEvent = new CategoryReorderedEvent[1];
        
        eventDispatcher.subscribe(CategoryReorderedEvent.class, event -> {
            capturedEvent[0] = event;
            latch.countDown();
        });
        
        List<UUID> newOrder = Arrays.asList(cat3.getId(), cat1.getId(), cat2.getId());
        categoryService.reorderCategories(newOrder);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(capturedEvent[0]);
        assertEquals(3, capturedEvent[0].getCategoryIds().size());
        
        // Verify sort orders were updated
        Category reorderedCat3 = categoryRepository.findById(cat3.getId()).orElse(null);
        assertNotNull(reorderedCat3);
        assertEquals(0, reorderedCat3.getSortOrder());
        
        Category reorderedCat1 = categoryRepository.findById(cat1.getId()).orElse(null);
        assertNotNull(reorderedCat1);
        assertEquals(1, reorderedCat1.getSortOrder());
    }
    
    @Test
    @DisplayName("Find categories by various criteria")
    void testFindCategories() {
        UUID parentId = UUID.randomUUID();
        
        Category rootCat = categoryRepository.save(Category.builder()
            .name("Root Category")
            .sortOrder(0)
            .build());
        
        Category childCat = categoryRepository.save(Category.builder()
            .name("Child Category")
            .parentCategoryId(rootCat.getId())
            .sortOrder(1)
            .build());
        
        Category defaultCat = categoryRepository.save(Category.builder()
            .name("Default")
            .isDefault(true)
            .sortOrder(2)
            .build());
        
        // Test findAll
        assertEquals(3, categoryService.findAll().size());
        
        // Test findAllOrdered
        List<Category> ordered = categoryService.findAllOrdered();
        assertEquals(3, ordered.size());
        assertEquals(rootCat.getId(), ordered.get(0).getId());
        
        // Test findRootCategories
        List<Category> rootCategories = categoryService.findRootCategories();
        assertEquals(2, rootCategories.size()); // rootCat and defaultCat
        
        // Test findByParentId
        List<Category> children = categoryService.findByParentId(rootCat.getId());
        assertEquals(1, children.size());
        assertEquals(childCat.getId(), children.get(0).getId());
        
        // Test findDefaultCategories
        List<Category> defaults = categoryService.findDefaultCategories();
        assertEquals(1, defaults.size());
        assertEquals(defaultCat.getId(), defaults.get(0).getId());
    }
}
