package net.talaatharb.workday.utils;

import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SearchIndexManager.
 */
class SearchIndexManagerTest {
    
    private SearchIndexManager searchIndexManager;
    
    @BeforeEach
    void setUp() {
        searchIndexManager = SearchIndexManager.getInstance();
        searchIndexManager.clearIndex();
    }
    
    @AfterEach
    void tearDown() {
        searchIndexManager.clearIndex();
    }
    
    @Test
    void testGetInstance_returnsSameInstance() {
        SearchIndexManager instance1 = SearchIndexManager.getInstance();
        SearchIndexManager instance2 = SearchIndexManager.getInstance();
        
        assertSame(instance1, instance2, "Should return same singleton instance");
    }
    
    @Test
    void testIndexTask_singleTask() {
        Task task = createTask("Complete project documentation", "Write comprehensive docs");
        
        searchIndexManager.indexTask(task);
        
        assertEquals(1, searchIndexManager.getIndexedTaskCount());
        assertTrue(searchIndexManager.isIndexed(task.getId()));
    }
    
    @Test
    void testIndexTask_withNullTask_ignored() {
        searchIndexManager.indexTask(null);
        
        assertEquals(0, searchIndexManager.getIndexedTaskCount());
    }
    
    @Test
    void testIndexTasks_multipleTasksAtOnce() {
        List<Task> tasks = Arrays.asList(
            createTask("Task 1", "Description 1"),
            createTask("Task 2", "Description 2"),
            createTask("Task 3", "Description 3")
        );
        
        searchIndexManager.indexTasks(tasks);
        
        assertEquals(3, searchIndexManager.getIndexedTaskCount());
        assertTrue(searchIndexManager.getLastIndexTime() >= 0);
    }
    
    @Test
    void testIndexTasks_withEmptyList_ignored() {
        searchIndexManager.indexTasks(Collections.emptyList());
        
        assertEquals(0, searchIndexManager.getIndexedTaskCount());
    }
    
    @Test
    void testRemoveFromIndex() {
        Task task = createTask("Test task", "Test description");
        searchIndexManager.indexTask(task);
        
        assertEquals(1, searchIndexManager.getIndexedTaskCount());
        
        searchIndexManager.removeFromIndex(task.getId());
        
        assertEquals(0, searchIndexManager.getIndexedTaskCount());
        assertFalse(searchIndexManager.isIndexed(task.getId()));
    }
    
    @Test
    void testRemoveFromIndex_nonExistentTask_ignored() {
        assertDoesNotThrow(() -> searchIndexManager.removeFromIndex(UUID.randomUUID()));
    }
    
    @Test
    void testSearch_byTitle() {
        Task task1 = createTask("Complete documentation", "Write docs");
        Task task2 = createTask("Review code", "Check pull requests");
        Task task3 = createTask("Documentation review", "Review the docs");
        
        searchIndexManager.indexTasks(Arrays.asList(task1, task2, task3));
        
        List<Task> results = searchIndexManager.search("documentation");
        
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(t -> t.getId().equals(task1.getId())));
        assertTrue(results.stream().anyMatch(t -> t.getId().equals(task3.getId())));
    }
    
    @Test
    void testSearch_byDescription() {
        Task task1 = createTask("Task 1", "Write unit tests");
        Task task2 = createTask("Task 2", "Write integration tests");
        Task task3 = createTask("Task 3", "Deploy application");
        
        searchIndexManager.indexTasks(Arrays.asList(task1, task2, task3));
        
        List<Task> results = searchIndexManager.search("tests");
        
        assertEquals(2, results.size());
    }
    
    @Test
    void testSearch_caseInsensitive() {
        Task task = createTask("IMPORTANT Task", "very IMPORTANT description");
        searchIndexManager.indexTask(task);
        
        List<Task> results = searchIndexManager.search("important");
        
        assertEquals(1, results.size());
        assertEquals(task.getId(), results.get(0).getId());
    }
    
    @Test
    void testSearch_withMultipleWords() {
        Task task1 = createTask("Fix bug in payment system", "Critical issue");
        Task task2 = createTask("Implement payment gateway", "New feature");
        Task task3 = createTask("Deploy to production", "Release");
        
        searchIndexManager.indexTasks(Arrays.asList(task1, task2, task3));
        
        List<Task> results = searchIndexManager.search("payment system");
        
        // Should match both task1 (payment and system) and task2 (payment)
        assertTrue(results.size() >= 2);
    }
    
    @Test
    void testSearch_withEmptyQuery_returnsAllTasks() {
        List<Task> tasks = Arrays.asList(
            createTask("Task 1", "Desc 1"),
            createTask("Task 2", "Desc 2")
        );
        searchIndexManager.indexTasks(tasks);
        
        List<Task> results = searchIndexManager.search("");
        
        assertEquals(2, results.size());
    }
    
    @Test
    void testSearch_noMatches_returnsEmptyList() {
        Task task = createTask("Simple task", "Simple description");
        searchIndexManager.indexTask(task);
        
        List<Task> results = searchIndexManager.search("nonexistent");
        
        assertTrue(results.isEmpty());
    }
    
    @Test
    void testSearch_byTags() {
        Task task1 = createTask("Task 1", "Description");
        task1.setTags(Arrays.asList("urgent", "backend"));
        
        Task task2 = createTask("Task 2", "Description");
        task2.setTags(Arrays.asList("frontend", "ui"));
        
        searchIndexManager.indexTasks(Arrays.asList(task1, task2));
        
        List<Task> results = searchIndexManager.search("urgent");
        
        assertEquals(1, results.size());
        assertEquals(task1.getId(), results.get(0).getId());
    }
    
    @Test
    void testSearch_fastPerformance() {
        // Create 1000 tasks
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            tasks.add(createTask("Task " + i, "Description for task " + i));
        }
        
        searchIndexManager.indexTasks(tasks);
        
        // Search should be fast (< 200ms as per acceptance criteria)
        long startTime = System.currentTimeMillis();
        List<Task> results = searchIndexManager.search("task");
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue(duration < 200, "Search should complete within 200ms, took: " + duration + "ms");
        assertEquals(1000, results.size());
    }
    
    @Test
    void testRebuildIndex() {
        List<Task> initialTasks = Arrays.asList(
            createTask("Task 1", "Desc 1"),
            createTask("Task 2", "Desc 2")
        );
        searchIndexManager.indexTasks(initialTasks);
        
        List<Task> newTasks = Arrays.asList(
            createTask("Task 3", "Desc 3"),
            createTask("Task 4", "Desc 4"),
            createTask("Task 5", "Desc 5")
        );
        
        searchIndexManager.rebuildIndex(newTasks);
        
        assertEquals(3, searchIndexManager.getIndexedTaskCount());
        List<Task> results = searchIndexManager.search("task");
        assertEquals(3, results.size());
    }
    
    @Test
    void testClearIndex() {
        List<Task> tasks = Arrays.asList(
            createTask("Task 1", "Desc 1"),
            createTask("Task 2", "Desc 2")
        );
        searchIndexManager.indexTasks(tasks);
        
        searchIndexManager.clearIndex();
        
        assertEquals(0, searchIndexManager.getIndexedTaskCount());
        List<Task> results = searchIndexManager.search("task");
        assertTrue(results.isEmpty());
    }
    
    @Test
    void testGetStatistics() {
        List<Task> tasks = Arrays.asList(
            createTask("Task 1", "Desc 1"),
            createTask("Task 2", "Desc 2")
        );
        searchIndexManager.indexTasks(tasks);
        
        Map<String, Object> stats = searchIndexManager.getStatistics();
        
        assertNotNull(stats);
        assertEquals(2L, ((Number) stats.get("indexedTaskCount")).longValue());
        assertTrue(((Number) stats.get("uniqueWords")).longValue() > 0);
        assertEquals(2L, ((Number) stats.get("cachedTasks")).longValue());
    }
    
    @Test
    void testHandleLargeTaskCollection_scenario() {
        // Given 10,000+ tasks in the database
        List<Task> largeTasks = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeTasks.add(createTask("Task " + i, "Description " + i));
        }
        
        // When loading and displaying tasks (indexing them)
        long startTime = System.currentTimeMillis();
        searchIndexManager.indexTasks(largeTasks);
        long indexTime = System.currentTimeMillis() - startTime;
        
        // Then UI should remain responsive (indexing should be reasonably fast)
        assertTrue(indexTime < 5000, "Indexing 10k tasks should complete within 5s, took: " + indexTime + "ms");
        assertEquals(10000, searchIndexManager.getIndexedTaskCount());
    }
    
    @Test
    void testFastSearchPerformance_scenario() {
        // Given a large task database
        List<Task> largeTasks = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeTasks.add(createTask("Task number " + i, "Description for task " + i));
        }
        searchIndexManager.indexTasks(largeTasks);
        
        // When searching for tasks
        long startTime = System.currentTimeMillis();
        List<Task> results = searchIndexManager.search("number 5000");
        long searchTime = System.currentTimeMillis() - startTime;
        
        // Then results should appear within 200ms
        assertTrue(searchTime < 200, "Search should complete within 200ms, took: " + searchTime + "ms");
        assertFalse(results.isEmpty(), "Should find matching tasks");
    }
    
    private Task createTask(String title, String description) {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(TaskStatus.TODO);
        task.setPriority(Priority.MEDIUM);
        task.setCreatedAt(LocalDateTime.now());
        task.setDueDate(LocalDate.now().plusDays(7));
        return task;
    }
}
