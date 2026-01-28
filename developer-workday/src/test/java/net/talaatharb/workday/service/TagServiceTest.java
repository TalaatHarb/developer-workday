package net.talaatharb.workday.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;

/**
 * Tests for TagService following acceptance criteria for task #42.
 * 
 * Feature: Task Tags
 *   Scenario: Add tags to task
 *   Scenario: Filter by tags
 *   Scenario: Tag management
 */
class TagServiceTest {
    
    private DB database;
    private TaskRepository taskRepository;
    private TagService tagService;
    private File dbFile;
    
    @BeforeEach
    void setUp() {
        dbFile = new File("test-tagservice-" + UUID.randomUUID() + ".db");
        database = DBMaker.fileDB(dbFile)
            .transactionEnable()
            .make();
        taskRepository = new TaskRepository(database);
        tagService = new TagService(taskRepository);
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
    @DisplayName("Scenario: Add tags to task - multiple tags supported")
    void testMultipleTags() {
        // Given: a task being created with multiple tags
        Task task = taskRepository.save(Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .tags(List.of("bug", "urgent", "frontend"))
            .build());
        
        // Then: multiple tags should be supported
        assertNotNull(task.getTags());
        assertEquals(3, task.getTags().size());
        assertTrue(task.getTags().contains("bug"));
        assertTrue(task.getTags().contains("urgent"));
        assertTrue(task.getTags().contains("frontend"));
    }
    
    @Test
    @DisplayName("Scenario: Tags autocomplete from existing tags")
    void testTagAutocomplete() {
        // Given: tasks with various tags
        taskRepository.save(Task.builder()
            .title("Task 1")
            .tags(List.of("bug", "backend", "critical"))
            .build());
        
        taskRepository.save(Task.builder()
            .title("Task 2")
            .tags(List.of("feature", "frontend", "ui"))
            .build());
        
        taskRepository.save(Task.builder()
            .title("Task 3")
            .tags(List.of("bug", "frontend", "minor"))
            .build());
        
        // When: getting tags matching prefix "b"
        List<String> matchingTags = tagService.getTagsMatchingPrefix("b");
        
        // Then: tags should autocomplete from existing tags
        assertTrue(matchingTags.contains("bug"));
        assertTrue(matchingTags.contains("backend"));
        assertFalse(matchingTags.contains("frontend"));
        assertFalse(matchingTags.contains("feature"));
    }
    
    @Test
    @DisplayName("Get all unique tags from tasks")
    void testGetAllTags() {
        // Given: tasks with overlapping tags
        taskRepository.save(Task.builder()
            .title("Task 1")
            .tags(List.of("bug", "urgent"))
            .build());
        
        taskRepository.save(Task.builder()
            .title("Task 2")
            .tags(List.of("bug", "feature"))
            .build());
        
        taskRepository.save(Task.builder()
            .title("Task 3")
            .tags(List.of("feature", "ui"))
            .build());
        
        // When: getting all tags
        Set<String> allTags = tagService.getAllTags();
        
        // Then: should return unique tags
        assertEquals(4, allTags.size());
        assertTrue(allTags.contains("bug"));
        assertTrue(allTags.contains("urgent"));
        assertTrue(allTags.contains("feature"));
        assertTrue(allTags.contains("ui"));
    }
    
    @Test
    @DisplayName("Scenario: Filter by tags with AND logic")
    void testFilterByTagsWithAndLogic() {
        // Given: tasks with various tags
        Task task1 = taskRepository.save(Task.builder()
            .title("Task 1")
            .status(TaskStatus.TODO)
            .tags(List.of("bug", "urgent", "frontend"))
            .build());
        
        Task task2 = taskRepository.save(Task.builder()
            .title("Task 2")
            .status(TaskStatus.TODO)
            .tags(List.of("bug", "frontend"))
            .build());
        
        Task task3 = taskRepository.save(Task.builder()
            .title("Task 3")
            .status(TaskStatus.TODO)
            .tags(List.of("feature", "frontend"))
            .build());
        
        List<Task> allTasks = taskRepository.findAll();
        
        // When: selecting a tag filter "bug"
        Set<String> filterTags = Set.of("bug");
        List<Task> filteredTasks = tagService.filterByTags(allTasks, filterTags);
        
        // Then: only tasks with that tag should be displayed
        assertEquals(2, filteredTasks.size());
        assertTrue(filteredTasks.stream().anyMatch(t -> t.getId().equals(task1.getId())));
        assertTrue(filteredTasks.stream().anyMatch(t -> t.getId().equals(task2.getId())));
        
        // And: multiple tag filters should work with AND logic
        Set<String> multipleFilters = Set.of("bug", "urgent");
        List<Task> strictFiltered = tagService.filterByTags(allTasks, multipleFilters);
        
        assertEquals(1, strictFiltered.size());
        assertEquals(task1.getId(), strictFiltered.get(0).getId());
    }
    
    @Test
    @DisplayName("Scenario: Tag management - rename tag")
    void testRenameTag() {
        // Given: existing tags
        taskRepository.save(Task.builder()
            .title("Task 1")
            .tags(List.of("bug", "urgent"))
            .build());
        
        taskRepository.save(Task.builder()
            .title("Task 2")
            .tags(List.of("bug", "feature"))
            .build());
        
        taskRepository.save(Task.builder()
            .title("Task 3")
            .tags(List.of("feature", "ui"))
            .build());
        
        // When: managing tags - renaming "bug" to "defect"
        int updatedCount = tagService.renameTag("bug", "defect");
        
        // Then: tag should be renamed in all tasks
        assertEquals(2, updatedCount);
        
        Set<String> allTags = tagService.getAllTags();
        assertFalse(allTags.contains("bug"));
        assertTrue(allTags.contains("defect"));
        
        // Verify tasks were updated
        List<Task> allTasks = taskRepository.findAll();
        long defectCount = allTasks.stream()
            .filter(t -> t.getTags() != null && t.getTags().contains("defect"))
            .count();
        assertEquals(2, defectCount);
    }
    
    @Test
    @DisplayName("Scenario: Tag management - delete tag")
    void testDeleteTag() {
        // Given: existing tags
        Task task1 = taskRepository.save(Task.builder()
            .title("Task 1")
            .tags(List.of("bug", "urgent"))
            .build());
        
        Task task2 = taskRepository.save(Task.builder()
            .title("Task 2")
            .tags(List.of("bug", "feature"))
            .build());
        
        Task task3 = taskRepository.save(Task.builder()
            .title("Task 3")
            .tags(List.of("feature", "ui"))
            .build());
        
        // When: deleting a tag
        int updatedCount = tagService.deleteTag("bug");
        
        // Then: deleting a tag should remove it from all tasks
        assertEquals(2, updatedCount);
        
        Set<String> allTags = tagService.getAllTags();
        assertFalse(allTags.contains("bug"));
        
        // Verify tasks were updated
        List<Task> allTasks = taskRepository.findAll();
        for (Task task : allTasks) {
            if (task.getTags() != null) {
                assertFalse(task.getTags().contains("bug"));
            }
        }
    }
    
    @Test
    @DisplayName("Get tag usage count")
    void testTagUsageCount() {
        // Given: tasks with various tags
        taskRepository.save(Task.builder()
            .title("Task 1")
            .tags(List.of("bug", "urgent"))
            .build());
        
        taskRepository.save(Task.builder()
            .title("Task 2")
            .tags(List.of("bug", "feature"))
            .build());
        
        taskRepository.save(Task.builder()
            .title("Task 3")
            .tags(List.of("feature", "ui"))
            .build());
        
        // When: getting tag usage count
        Map<String, Long> usageCount = tagService.getTagUsageCount();
        
        // Then: should return count for each tag
        assertEquals(2L, usageCount.get("bug"));
        assertEquals(2L, usageCount.get("feature"));
        assertEquals(1L, usageCount.get("urgent"));
        assertEquals(1L, usageCount.get("ui"));
    }
    
    @Test
    @DisplayName("Empty tags should be handled gracefully")
    void testEmptyTags() {
        // Given: tasks without tags
        taskRepository.save(Task.builder()
            .title("Task without tags")
            .build());
        
        // When: getting all tags
        Set<String> allTags = tagService.getAllTags();
        
        // Then: should return empty set
        assertTrue(allTags.isEmpty());
    }
    
    @Test
    @DisplayName("Tag autocomplete returns all tags when prefix is empty")
    void testAutocompleteWithEmptyPrefix() {
        // Given: tasks with tags
        taskRepository.save(Task.builder()
            .title("Task 1")
            .tags(List.of("bug", "feature", "ui"))
            .build());
        
        // When: getting tags with empty prefix
        List<String> matchingTags = tagService.getTagsMatchingPrefix("");
        
        // Then: should return all tags
        assertEquals(3, matchingTags.size());
    }
}
