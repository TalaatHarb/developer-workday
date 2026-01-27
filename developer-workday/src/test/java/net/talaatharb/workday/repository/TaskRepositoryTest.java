package net.talaatharb.workday.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

class TaskRepositoryTest {
    
    @TempDir
    Path tempDir;
    
    private DB database;
    private TaskRepository repository;
    
    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test.db");
        database = DBMaker.fileDB(dbPath.toFile()).transactionEnable().make();
        repository = new TaskRepository(database);
    }
    
    @AfterEach
    void tearDown() {
        if (database != null && !database.isClosed()) {
            database.close();
        }
    }
    
    @Test
    void testSaveAndFindById() {
        Task task = Task.builder()
            .title("Test Task")
            .description("Test Description")
            .priority(Priority.HIGH)
            .status(TaskStatus.TODO)
            .build();
        
        Task saved = repository.save(task);
        
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        
        UUID id = saved.getId();
        Task found = repository.findById(id).orElse(null);
        
        assertNotNull(found);
        assertEquals("Test Task", found.getTitle());
        assertEquals(Priority.HIGH, found.getPriority());
    }
    
    @Test
    void testFindByCategoryId() {
        UUID categoryId1 = UUID.randomUUID();
        UUID categoryId2 = UUID.randomUUID();
        
        repository.save(Task.builder().title("Task 1").categoryId(categoryId1).build());
        repository.save(Task.builder().title("Task 2").categoryId(categoryId1).build());
        repository.save(Task.builder().title("Task 3").categoryId(categoryId2).build());
        
        List<Task> category1Tasks = repository.findByCategoryId(categoryId1);
        assertEquals(2, category1Tasks.size());
        
        List<Task> category2Tasks = repository.findByCategoryId(categoryId2);
        assertEquals(1, category2Tasks.size());
    }
    
    @Test
    void testFindByDueDateBetween() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        
        repository.save(Task.builder().title("Task 1").dueDate(LocalDate.of(2024, 1, 15)).build());
        repository.save(Task.builder().title("Task 2").dueDate(LocalDate.of(2024, 2, 15)).build());
        repository.save(Task.builder().title("Task 3").dueDate(LocalDate.of(2024, 1, 30)).build());
        
        List<Task> tasksInRange = repository.findByDueDateBetween(start, end);
        assertEquals(2, tasksInRange.size());
    }
    
    @Test
    void testFindOverdueTasks() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        repository.save(Task.builder().title("Overdue").dueDate(yesterday).status(TaskStatus.TODO).build());
        repository.save(Task.builder().title("Not Overdue").dueDate(tomorrow).status(TaskStatus.TODO).build());
        repository.save(Task.builder().title("Completed Overdue").dueDate(yesterday).status(TaskStatus.COMPLETED).build());
        
        List<Task> overdueTasks = repository.findOverdueTasks();
        assertEquals(1, overdueTasks.size());
        assertEquals("Overdue", overdueTasks.get(0).getTitle());
    }
    
    @Test
    void testFindByStatus() {
        repository.save(Task.builder().title("Todo 1").status(TaskStatus.TODO).build());
        repository.save(Task.builder().title("Todo 2").status(TaskStatus.TODO).build());
        repository.save(Task.builder().title("Completed").status(TaskStatus.COMPLETED).build());
        
        List<Task> todoTasks = repository.findByStatus(TaskStatus.TODO);
        assertEquals(2, todoTasks.size());
        
        List<Task> completedTasks = repository.findByStatus(TaskStatus.COMPLETED);
        assertEquals(1, completedTasks.size());
    }
    
    @Test
    void testDeleteById() {
        Task task = repository.save(Task.builder().title("To Delete").build());
        UUID id = task.getId();
        
        assertTrue(repository.existsById(id));
        assertTrue(repository.deleteById(id));
        assertFalse(repository.existsById(id));
    }
    
    @Test
    void testUpdate() {
        Task task = repository.save(Task.builder().title("Original").build());
        UUID id = task.getId();
        LocalDateTime createdAt = task.getCreatedAt();
        
        task.setTitle("Updated");
        Task updated = repository.save(task);
        
        assertEquals("Updated", updated.getTitle());
        assertEquals(createdAt, updated.getCreatedAt());
        assertTrue(updated.getUpdatedAt().isAfter(createdAt));
    }
    
    @Test
    void testCount() {
        assertEquals(0, repository.count());
        
        repository.save(Task.builder().title("Task 1").build());
        repository.save(Task.builder().title("Task 2").build());
        
        assertEquals(2, repository.count());
    }
    
    @Test
    void testFindAll() {
        repository.save(Task.builder().title("Task 1").build());
        repository.save(Task.builder().title("Task 2").build());
        repository.save(Task.builder().title("Task 3").build());
        
        List<Task> allTasks = repository.findAll();
        assertEquals(3, allTasks.size());
    }
    
    @Test
    void testSearchTasks_ByTitle() {
        repository.save(Task.builder().title("Fix bug in login").build());
        repository.save(Task.builder().title("Update documentation").build());
        repository.save(Task.builder().title("Fix bug in search").build());
        
        List<Task> results = repository.searchTasks("bug");
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(t -> t.getTitle().toLowerCase().contains("bug")));
    }
    
    @Test
    void testSearchTasks_ByDescription() {
        repository.save(Task.builder()
            .title("Task 1")
            .description("This task involves fixing a critical bug")
            .build());
        repository.save(Task.builder()
            .title("Task 2")
            .description("Update the user interface")
            .build());
        repository.save(Task.builder()
            .title("Task 3")
            .description("Critical performance issue")
            .build());
        
        List<Task> results = repository.searchTasks("critical");
        assertEquals(2, results.size());
    }
    
    @Test
    void testSearchTasks_ByTags() {
        repository.save(Task.builder()
            .title("Task 1")
            .tags(List.of("bug", "production"))
            .build());
        repository.save(Task.builder()
            .title("Task 2")
            .tags(List.of("feature", "development"))
            .build());
        repository.save(Task.builder()
            .title("Task 3")
            .tags(List.of("bug", "testing"))
            .build());
        
        List<Task> results = repository.searchTasks("bug");
        assertEquals(2, results.size());
    }
    
    @Test
    void testSearchTasks_CaseInsensitive() {
        repository.save(Task.builder().title("FIX BUG").build());
        repository.save(Task.builder().title("Update Docs").build());
        
        List<Task> results = repository.searchTasks("FIX");
        assertEquals(1, results.size());
        
        results = repository.searchTasks("fix");
        assertEquals(1, results.size());
        
        results = repository.searchTasks("Fix");
        assertEquals(1, results.size());
    }
    
    @Test
    void testSearchTasks_EmptyKeyword() {
        repository.save(Task.builder().title("Task 1").build());
        repository.save(Task.builder().title("Task 2").build());
        
        List<Task> results = repository.searchTasks("");
        assertEquals(2, results.size());
        
        results = repository.searchTasks(null);
        assertEquals(2, results.size());
    }
    
    @Test
    void testSearchTasks_NoResults() {
        repository.save(Task.builder().title("Task 1").build());
        repository.save(Task.builder().title("Task 2").build());
        
        List<Task> results = repository.searchTasks("nonexistent");
        assertTrue(results.isEmpty());
    }
    
    @Test
    void testSearchTasks_MultipleFields() {
        // Task that matches in title
        repository.save(Task.builder()
            .title("Bug fix needed")
            .description("Update the login page")
            .tags(List.of("feature"))
            .build());
        
        // Task that matches in description
        repository.save(Task.builder()
            .title("Update UI")
            .description("Fix the bug in the navigation")
            .tags(List.of("ui"))
            .build());
        
        // Task that matches in tags
        repository.save(Task.builder()
            .title("Refactor code")
            .description("Improve performance")
            .tags(List.of("bug", "refactoring"))
            .build());
        
        List<Task> results = repository.searchTasks("bug");
        assertEquals(3, results.size());
    }
}
