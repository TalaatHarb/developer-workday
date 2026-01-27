package net.talaatharb.workday.facade;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import net.talaatharb.workday.dtos.CategoryTreeNode;
import net.talaatharb.workday.dtos.CategoryWithTaskCount;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.EventLogger;
import net.talaatharb.workday.model.Category;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.CategoryRepository;
import net.talaatharb.workday.repository.TaskRepository;
import net.talaatharb.workday.service.CategoryService;
import net.talaatharb.workday.service.TaskService;

/**
 * Tests for CategoryFacade following the acceptance criteria.
 * 
 * Feature: Category Facade
 *   Scenario: Get category tree
 *     Given categories with parent-child relationships
 *     When getCategoryTree is called
 *     Then a hierarchical tree structure of categories should be returned
 *
 *   Scenario: Get category with task count
 *     Given categories with associated tasks
 *     When getCategoriesWithTaskCount is called
 *     Then each category should include the count of active tasks
 */
class CategoryFacadeTest {
    
    private DB database;
    private CategoryRepository categoryRepository;
    private TaskRepository taskRepository;
    private EventDispatcher eventDispatcher;
    private EventLogger eventLogger;
    private CategoryService categoryService;
    private TaskService taskService;
    private CategoryFacade categoryFacade;
    private File dbFile;
    
    @BeforeEach
    void setUp() {
        dbFile = new File("test-categoryfacade-" + UUID.randomUUID() + ".db");
        database = DBMaker.fileDB(dbFile)
            .transactionEnable()
            .make();
        categoryRepository = new CategoryRepository(database);
        taskRepository = new TaskRepository(database);
        eventLogger = new EventLogger();
        eventDispatcher = new EventDispatcher(eventLogger);
        categoryService = new CategoryService(categoryRepository, taskRepository, eventDispatcher);
        taskService = new TaskService(taskRepository, eventDispatcher);
        categoryFacade = new CategoryFacade(categoryService, taskService);
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
    @DisplayName("Get category tree - returns hierarchical tree structure")
    void testGetCategoryTree_WithHierarchy() {
        // Given: categories with parent-child relationships
        Category rootCategory1 = categoryRepository.save(Category.builder()
            .name("Work")
            .sortOrder(0)
            .build());
        
        Category rootCategory2 = categoryRepository.save(Category.builder()
            .name("Personal")
            .sortOrder(1)
            .build());
        
        Category childCategory1 = categoryRepository.save(Category.builder()
            .name("Project A")
            .parentCategoryId(rootCategory1.getId())
            .sortOrder(0)
            .build());
        
        Category childCategory2 = categoryRepository.save(Category.builder()
            .name("Project B")
            .parentCategoryId(rootCategory1.getId())
            .sortOrder(1)
            .build());
        
        Category grandchildCategory = categoryRepository.save(Category.builder()
            .name("Sprint 1")
            .parentCategoryId(childCategory1.getId())
            .sortOrder(0)
            .build());
        
        // When: getCategoryTree is called
        List<CategoryTreeNode> tree = categoryFacade.getCategoryTree();
        
        // Then: a hierarchical tree structure should be returned
        assertNotNull(tree);
        assertEquals(2, tree.size(), "Should have 2 root categories");
        
        // And: root categories should be in correct order
        CategoryTreeNode workNode = tree.stream()
            .filter(n -> n.getName().equals("Work"))
            .findFirst()
            .orElse(null);
        assertNotNull(workNode, "Work category should be in tree");
        
        CategoryTreeNode personalNode = tree.stream()
            .filter(n -> n.getName().equals("Personal"))
            .findFirst()
            .orElse(null);
        assertNotNull(personalNode, "Personal category should be in tree");
        
        // And: Work category should have 2 children
        assertEquals(2, workNode.getChildren().size(), "Work should have 2 child categories");
        
        // And: children should be properly structured
        CategoryTreeNode projectA = workNode.getChildren().stream()
            .filter(n -> n.getName().equals("Project A"))
            .findFirst()
            .orElse(null);
        assertNotNull(projectA, "Project A should be child of Work");
        
        CategoryTreeNode projectB = workNode.getChildren().stream()
            .filter(n -> n.getName().equals("Project B"))
            .findFirst()
            .orElse(null);
        assertNotNull(projectB, "Project B should be child of Work");
        
        // And: Project A should have 1 grandchild
        assertEquals(1, projectA.getChildren().size(), "Project A should have 1 child");
        assertEquals("Sprint 1", projectA.getChildren().get(0).getName(), 
            "Sprint 1 should be child of Project A");
        
        // And: Personal category should have no children
        assertEquals(0, personalNode.getChildren().size(), "Personal should have no children");
    }
    
    @Test
    @DisplayName("Get category tree - handles flat structure")
    void testGetCategoryTree_FlatStructure() {
        // Given: categories without parent relationships
        categoryRepository.save(Category.builder()
            .name("Category 1")
            .sortOrder(0)
            .build());
        
        categoryRepository.save(Category.builder()
            .name("Category 2")
            .sortOrder(1)
            .build());
        
        categoryRepository.save(Category.builder()
            .name("Category 3")
            .sortOrder(2)
            .build());
        
        // When: getCategoryTree is called
        List<CategoryTreeNode> tree = categoryFacade.getCategoryTree();
        
        // Then: all categories should be root nodes
        assertEquals(3, tree.size(), "Should have 3 root categories");
        
        // And: none should have children
        for (CategoryTreeNode node : tree) {
            assertEquals(0, node.getChildren().size(), 
                "Flat structure categories should have no children");
        }
    }
    
    @Test
    @DisplayName("Get category tree - handles orphaned categories")
    void testGetCategoryTree_OrphanedCategories() {
        // Given: category with non-existent parent
        Category orphanedCategory = categoryRepository.save(Category.builder()
            .name("Orphaned")
            .parentCategoryId(UUID.randomUUID()) // Non-existent parent
            .sortOrder(0)
            .build());
        
        // When: getCategoryTree is called
        List<CategoryTreeNode> tree = categoryFacade.getCategoryTree();
        
        // Then: orphaned category should be treated as root
        assertEquals(1, tree.size(), "Orphaned category should become root");
        assertEquals("Orphaned", tree.get(0).getName());
    }
    
    @Test
    @DisplayName("Get category tree - returns empty list when no categories")
    void testGetCategoryTree_Empty() {
        // Given: no categories exist
        
        // When: getCategoryTree is called
        List<CategoryTreeNode> tree = categoryFacade.getCategoryTree();
        
        // Then: empty list should be returned
        assertNotNull(tree);
        assertEquals(0, tree.size(), "Should return empty list when no categories");
    }
    
    @Test
    @DisplayName("Get categories with task count - includes active task counts")
    void testGetCategoriesWithTaskCount() {
        // Given: categories with associated tasks
        Category workCategory = categoryRepository.save(Category.builder()
            .name("Work")
            .build());
        
        Category personalCategory = categoryRepository.save(Category.builder()
            .name("Personal")
            .build());
        
        // And: Work category has 3 active tasks
        taskRepository.save(Task.builder()
            .title("Work Task 1")
            .status(TaskStatus.TODO)
            .categoryId(workCategory.getId())
            .build());
        
        taskRepository.save(Task.builder()
            .title("Work Task 2")
            .status(TaskStatus.IN_PROGRESS)
            .categoryId(workCategory.getId())
            .build());
        
        taskRepository.save(Task.builder()
            .title("Work Task 3")
            .status(TaskStatus.TODO)
            .categoryId(workCategory.getId())
            .build());
        
        // And: Work category has 1 completed task (should not be counted)
        taskRepository.save(Task.builder()
            .title("Work Task Completed")
            .status(TaskStatus.COMPLETED)
            .categoryId(workCategory.getId())
            .build());
        
        // And: Work category has 1 cancelled task (should not be counted)
        taskRepository.save(Task.builder()
            .title("Work Task Cancelled")
            .status(TaskStatus.CANCELLED)
            .categoryId(workCategory.getId())
            .build());
        
        // And: Personal category has 1 active task
        taskRepository.save(Task.builder()
            .title("Personal Task 1")
            .status(TaskStatus.TODO)
            .categoryId(personalCategory.getId())
            .build());
        
        // When: getCategoriesWithTaskCount is called
        List<CategoryWithTaskCount> categories = categoryFacade.getCategoriesWithTaskCount();
        
        // Then: each category should include the count of active tasks
        assertNotNull(categories);
        assertEquals(2, categories.size(), "Should return 2 categories");
        
        // And: Work category should have count of 3 (active tasks only)
        CategoryWithTaskCount workResult = categories.stream()
            .filter(c -> c.getName().equals("Work"))
            .findFirst()
            .orElse(null);
        assertNotNull(workResult, "Work category should be in results");
        assertEquals(3, workResult.getTaskCount(), 
            "Work should have 3 active tasks (excluding completed and cancelled)");
        
        // And: Personal category should have count of 1
        CategoryWithTaskCount personalResult = categories.stream()
            .filter(c -> c.getName().equals("Personal"))
            .findFirst()
            .orElse(null);
        assertNotNull(personalResult, "Personal category should be in results");
        assertEquals(1, personalResult.getTaskCount(), "Personal should have 1 active task");
    }
    
    @Test
    @DisplayName("Get categories with task count - handles categories with no tasks")
    void testGetCategoriesWithTaskCount_NoTasks() {
        // Given: categories with no associated tasks
        categoryRepository.save(Category.builder()
            .name("Empty Category 1")
            .build());
        
        categoryRepository.save(Category.builder()
            .name("Empty Category 2")
            .build());
        
        // When: getCategoriesWithTaskCount is called
        List<CategoryWithTaskCount> categories = categoryFacade.getCategoriesWithTaskCount();
        
        // Then: categories should have task count of 0
        assertEquals(2, categories.size());
        for (CategoryWithTaskCount category : categories) {
            assertEquals(0, category.getTaskCount(), 
                "Categories with no tasks should have count of 0");
        }
    }
    
    @Test
    @DisplayName("Get categories with task count - returns empty list when no categories")
    void testGetCategoriesWithTaskCount_NoCategories() {
        // Given: no categories exist
        
        // When: getCategoriesWithTaskCount is called
        List<CategoryWithTaskCount> categories = categoryFacade.getCategoriesWithTaskCount();
        
        // Then: empty list should be returned
        assertNotNull(categories);
        assertEquals(0, categories.size(), "Should return empty list when no categories");
    }
    
    @Test
    @DisplayName("Get categories with task count - preserves category metadata")
    void testGetCategoriesWithTaskCount_PreservesMetadata() {
        // Given: category with full metadata
        Category category = categoryRepository.save(Category.builder()
            .name("Test Category")
            .description("Test Description")
            .color("#FF5733")
            .icon("test-icon")
            .sortOrder(5)
            .isDefault(true)
            .build());
        
        // When: getCategoriesWithTaskCount is called
        List<CategoryWithTaskCount> categories = categoryFacade.getCategoriesWithTaskCount();
        
        // Then: all metadata should be preserved
        assertEquals(1, categories.size());
        CategoryWithTaskCount result = categories.get(0);
        
        assertEquals(category.getId(), result.getId());
        assertEquals("Test Category", result.getName());
        assertEquals("Test Description", result.getDescription());
        assertEquals("#FF5733", result.getColor());
        assertEquals("test-icon", result.getIcon());
        assertEquals(5, result.getSortOrder());
        assertTrue(result.getIsDefault());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
    }
}
