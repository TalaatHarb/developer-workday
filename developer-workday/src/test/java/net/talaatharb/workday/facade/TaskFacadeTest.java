package net.talaatharb.workday.facade;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.EventLogger;
import net.talaatharb.workday.facade.TaskFacade.TaskStatistics;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;
import net.talaatharb.workday.service.ReminderService;
import net.talaatharb.workday.service.TaskService;

/**
 * Tests for TaskFacade following the acceptance criteria.
 */
class TaskFacadeTest {
    
    private DB database;
    private TaskRepository taskRepository;
    private EventDispatcher eventDispatcher;
    private EventLogger eventLogger;
    private TaskService taskService;
    private ReminderService reminderService;
    private TaskFacade taskFacade;
    private File dbFile;
    
    @BeforeEach
    void setUp() {
        dbFile = new File("test-taskfacade-" + UUID.randomUUID() + ".db");
        database = DBMaker.fileDB(dbFile)
            .transactionEnable()
            .make();
        taskRepository = new TaskRepository(database);
        eventLogger = new EventLogger();
        eventDispatcher = new EventDispatcher(eventLogger);
        taskService = new TaskService(taskRepository, eventDispatcher);
        reminderService = new ReminderService(eventDispatcher);
        taskFacade = new TaskFacade(taskService, reminderService);
    }
    
    @AfterEach
    void tearDown() {
        if (reminderService != null) {
            reminderService.shutdown();
        }
        if (database != null && !database.isClosed()) {
            database.close();
        }
        if (dbFile != null && dbFile.exists()) {
            dbFile.delete();
        }
    }
    
    @Test
    @DisplayName("Get tasks for today - overdue first, then today's, sorted by time and priority")
    void testGetTasksForToday() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        // Create overdue task (high priority)
        Task overdueTask = taskRepository.save(Task.builder()
            .title("Overdue Task")
            .status(TaskStatus.TODO)
            .dueDate(yesterday)
            .priority(Priority.HIGH)
            .build());
        
        // Create today's task with early time (medium priority)
        Task todayEarlyTask = taskRepository.save(Task.builder()
            .title("Today Early Task")
            .status(TaskStatus.TODO)
            .scheduledDate(today)
            .dueDate(today)
            .dueTime(LocalTime.of(9, 0))
            .priority(Priority.MEDIUM)
            .build());
        
        // Create today's task with late time (low priority)
        Task todayLateTask = taskRepository.save(Task.builder()
            .title("Today Late Task")
            .status(TaskStatus.TODO)
            .scheduledDate(today)
            .dueDate(today)
            .dueTime(LocalTime.of(15, 0))
            .priority(Priority.LOW)
            .build());
        
        // Create today's task with early time (urgent priority)
        Task todayUrgentTask = taskRepository.save(Task.builder()
            .title("Today Urgent Task")
            .status(TaskStatus.TODO)
            .scheduledDate(today)
            .dueDate(today)
            .dueTime(LocalTime.of(9, 0))
            .priority(Priority.URGENT)
            .build());
        
        // When: getTasksForToday is called
        List<Task> tasksForToday = taskFacade.getTasksForToday();
        
        // Then: overdue tasks should be returned first
        assertTrue(tasksForToday.size() >= 1, "Should have at least overdue task");
        assertEquals(overdueTask.getId(), tasksForToday.get(0).getId(), 
            "Overdue task should be first");
        
        // And: tasks scheduled for today should follow
        assertTrue(tasksForToday.size() >= 4, "Should have all 4 tasks");
        
        // And: tasks should be sorted by time and priority
        // After overdue, today's tasks with same time should be sorted by priority
        int urgentIndex = -1;
        int earlyIndex = -1;
        int lateIndex = -1;
        
        for (int i = 0; i < tasksForToday.size(); i++) {
            Task task = tasksForToday.get(i);
            if (task.getId().equals(todayUrgentTask.getId())) urgentIndex = i;
            if (task.getId().equals(todayEarlyTask.getId())) earlyIndex = i;
            if (task.getId().equals(todayLateTask.getId())) lateIndex = i;
        }
        
        assertTrue(urgentIndex < earlyIndex, "Urgent task should come before medium priority task with same time");
        assertTrue(earlyIndex < lateIndex, "Early time task should come before late time task");
    }
    
    @Test
    @DisplayName("Quick add task - parses title, date, time, and tags")
    void testQuickAddTask() {
        // Given: a quick add task string
        String quickAddString = "Buy groceries tomorrow at 5pm #personal";
        
        // When: quickAddTask is called
        Task createdTask = taskFacade.quickAddTask(quickAddString);
        
        // Then: the string should be parsed for task details
        assertNotNull(createdTask);
        
        // And: a task should be created with extracted title
        assertEquals("Buy groceries", createdTask.getTitle());
        
        // And: date should be tomorrow
        assertEquals(LocalDate.now().plusDays(1), createdTask.getScheduledDate());
        assertEquals(LocalDate.now().plusDays(1), createdTask.getDueDate());
        
        // And: time should be 5pm (17:00)
        assertNotNull(createdTask.getDueTime());
        assertEquals(LocalTime.of(17, 0), createdTask.getDueTime());
        
        // And: tags should include 'personal'
        assertTrue(createdTask.getTags().contains("personal"));
    }
    
    @Test
    @DisplayName("Quick add task - handles different time formats")
    void testQuickAddTask_DifferentTimeFormats() {
        // Test 24-hour format
        Task task1 = taskFacade.quickAddTask("Meeting at 14:30");
        assertEquals(LocalTime.of(14, 30), task1.getDueTime());
        
        // Test am
        Task task2 = taskFacade.quickAddTask("Breakfast at 8am");
        assertEquals(LocalTime.of(8, 0), task2.getDueTime());
        
        // Test pm
        Task task3 = taskFacade.quickAddTask("Dinner at 7pm");
        assertEquals(LocalTime.of(19, 0), task3.getDueTime());
    }
    
    @Test
    @DisplayName("Quick add task - handles today")
    void testQuickAddTask_Today() {
        Task task = taskFacade.quickAddTask("Call client today");
        
        assertEquals("Call client", task.getTitle());
        assertEquals(LocalDate.now(), task.getScheduledDate());
    }
    
    @Test
    @DisplayName("Quick add task - handles multiple tags")
    void testQuickAddTask_MultipleTags() {
        Task task = taskFacade.quickAddTask("Review PR #work #urgent");
        
        assertEquals("Review PR", task.getTitle());
        assertTrue(task.getTags().contains("work"));
        assertTrue(task.getTags().contains("urgent"));
    }
    
    @Test
    @DisplayName("Get task statistics - calculates total, completed, overdue, and completion rate")
    void testGetTaskStatistics() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // Create completed task
        taskRepository.save(Task.builder()
            .title("Completed Task")
            .status(TaskStatus.COMPLETED)
            .dueDate(startDate.plusDays(1))
            .build());
        
        // Create another completed task
        taskRepository.save(Task.builder()
            .title("Another Completed Task")
            .status(TaskStatus.COMPLETED)
            .dueDate(startDate.plusDays(2))
            .build());
        
        // Create overdue incomplete task
        taskRepository.save(Task.builder()
            .title("Overdue Task")
            .status(TaskStatus.TODO)
            .dueDate(yesterday)
            .build());
        
        // Create current incomplete task
        taskRepository.save(Task.builder()
            .title("Current Task")
            .status(TaskStatus.TODO)
            .dueDate(endDate)
            .build());
        
        // When: getTaskStatistics is called
        TaskStatistics stats = taskFacade.getTaskStatistics(startDate, endDate);
        
        // Then: statistics should include total tasks
        assertEquals(4, stats.totalTasks());
        
        // And: completed count
        assertEquals(2, stats.completedTasks());
        
        // And: overdue count
        assertEquals(1, stats.overdueTasks());
        
        // And: completion rate (50%)
        assertEquals(50.0, stats.completionRate(), 0.01);
    }
    
    @Test
    @DisplayName("Create task coordinates with reminder service")
    void testCreateTask_WithReminder() {
        Task task = Task.builder()
            .title("Task with Reminder")
            .status(TaskStatus.TODO)
            .dueDate(LocalDate.now().plusDays(1))
            .dueTime(LocalTime.of(10, 0))
            .reminderMinutesBefore(30)
            .build();
        
        Task createdTask = taskFacade.createTask(task);
        
        assertNotNull(createdTask.getId());
        assertTrue(reminderService.hasScheduledReminder(createdTask.getId()));
    }
    
    @Test
    @DisplayName("Complete task cancels reminder")
    void testCompleteTask_CancelsReminder() {
        Task task = taskRepository.save(Task.builder()
            .title("Task")
            .status(TaskStatus.TODO)
            .dueDate(LocalDate.now().plusDays(1))
            .dueTime(LocalTime.of(10, 0))
            .reminderMinutesBefore(30)
            .build());
        
        reminderService.scheduleReminder(task);
        assertTrue(reminderService.hasScheduledReminder(task.getId()));
        
        taskFacade.completeTask(task.getId());
        
        assertFalse(reminderService.hasScheduledReminder(task.getId()));
    }
    
    @Test
    @DisplayName("Delete task cancels reminder")
    void testDeleteTask_CancelsReminder() {
        Task task = taskRepository.save(Task.builder()
            .title("Task")
            .status(TaskStatus.TODO)
            .dueDate(LocalDate.now().plusDays(1))
            .dueTime(LocalTime.of(10, 0))
            .reminderMinutesBefore(30)
            .build());
        
        reminderService.scheduleReminder(task);
        assertTrue(reminderService.hasScheduledReminder(task.getId()));
        
        taskFacade.deleteTask(task.getId());
        
        assertFalse(reminderService.hasScheduledReminder(task.getId()));
        assertFalse(taskRepository.existsById(task.getId()));
    }
    
    @Test
    @DisplayName("Find methods delegate to service")
    void testFindMethods() {
        UUID categoryId = UUID.randomUUID();
        
        taskRepository.save(Task.builder()
            .title("Task 1")
            .status(TaskStatus.TODO)
            .categoryId(categoryId)
            .build());
        
        taskRepository.save(Task.builder()
            .title("Task 2")
            .status(TaskStatus.COMPLETED)
            .build());
        
        assertEquals(2, taskFacade.findAll().size());
        assertEquals(1, taskFacade.findByStatus(TaskStatus.TODO).size());
        assertEquals(1, taskFacade.findByCategoryId(categoryId).size());
    }
    
    @Test
    @DisplayName("Search tasks by keyword in title, description, and tags")
    void testSearchTasks() {
        // Given: tasks with various titles, descriptions, and tags
        taskRepository.save(Task.builder()
            .title("Fix critical bug")
            .description("Memory leak causing crashes")
            .tags(List.of("urgent", "production"))
            .status(TaskStatus.TODO)
            .build());
        
        taskRepository.save(Task.builder()
            .title("Update documentation")
            .description("Add API documentation for endpoints")
            .tags(List.of("docs", "api"))
            .status(TaskStatus.TODO)
            .build());
        
        taskRepository.save(Task.builder()
            .title("Review code changes")
            .description("Check code quality and tests")
            .tags(List.of("review", "code"))
            .status(TaskStatus.IN_PROGRESS)
            .build());
        
        // When: searching by keyword in title
        List<Task> bugResults = taskFacade.searchTasks("bug");
        
        // Then: matching tasks should be displayed
        assertEquals(1, bugResults.size());
        assertTrue(bugResults.get(0).getTitle().toLowerCase().contains("bug"));
        
        // When: searching by keyword in description
        List<Task> apiResults = taskFacade.searchTasks("api");
        
        // Then: matching tasks should be displayed
        assertEquals(1, apiResults.size());
        assertTrue(apiResults.get(0).getDescription().toLowerCase().contains("api"));
        
        // When: searching by keyword in tags
        List<Task> reviewResults = taskFacade.searchTasks("review");
        
        // Then: matching tasks should be displayed
        assertEquals(1, reviewResults.size());
        assertTrue(reviewResults.get(0).getTags().stream()
            .anyMatch(tag -> tag.toLowerCase().contains("review")));
    }
    
    @Test
    @DisplayName("Search with filters - narrows results by status, priority, and category")
    void testSearchTasksWithFilters() {
        // Given: tasks with various attributes
        UUID category1 = UUID.randomUUID();
        UUID category2 = UUID.randomUUID();
        
        taskRepository.save(Task.builder()
            .title("Fix bug in login")
            .status(TaskStatus.TODO)
            .priority(Priority.HIGH)
            .categoryId(category1)
            .build());
        
        taskRepository.save(Task.builder()
            .title("Fix bug in search")
            .status(TaskStatus.IN_PROGRESS)
            .priority(Priority.MEDIUM)
            .categoryId(category1)
            .build());
        
        taskRepository.save(Task.builder()
            .title("Fix bug in logout")
            .status(TaskStatus.TODO)
            .priority(Priority.LOW)
            .categoryId(category2)
            .build());
        
        // When: searching with status filter
        List<Task> todoResults = taskFacade.searchTasksWithFilters("bug", TaskStatus.TODO, null, null);
        
        // Then: results should be narrowed by selected filters
        assertEquals(2, todoResults.size());
        assertTrue(todoResults.stream().allMatch(t -> t.getStatus() == TaskStatus.TODO));
        
        // When: searching with priority filter
        List<Task> highPriorityResults = taskFacade.searchTasksWithFilters("bug", null, Priority.HIGH, null);
        
        // Then: results should be narrowed by priority
        assertEquals(1, highPriorityResults.size());
        assertEquals(Priority.HIGH, highPriorityResults.get(0).getPriority());
        
        // When: searching with category filter
        List<Task> category1Results = taskFacade.searchTasksWithFilters("bug", null, null, category1);
        
        // Then: results should be narrowed by category
        assertEquals(2, category1Results.size());
        assertTrue(category1Results.stream().allMatch(t -> t.getCategoryId().equals(category1)));
        
        // When: searching with multiple filters
        List<Task> multiFilterResults = taskFacade.searchTasksWithFilters("bug", TaskStatus.TODO, Priority.HIGH, category1);
        
        // Then: results should match all filters
        assertEquals(1, multiFilterResults.size());
        Task result = multiFilterResults.get(0);
        assertEquals(TaskStatus.TODO, result.getStatus());
        assertEquals(Priority.HIGH, result.getPriority());
        assertEquals(category1, result.getCategoryId());
    }
    
    @Test
    @DisplayName("Clear search - returns to previous state")
    void testClearSearch() {
        // Given: tasks exist
        taskRepository.save(Task.builder().title("Task 1").build());
        taskRepository.save(Task.builder().title("Task 2").build());
        taskRepository.save(Task.builder().title("Different").build());
        
        // When: searching with keyword
        List<Task> searchResults = taskFacade.searchTasks("Task");
        assertEquals(2, searchResults.size());
        
        // When: clearing search (empty keyword)
        List<Task> allResults = taskFacade.searchTasks("");
        
        // Then: view should return to the previous state (all tasks)
        assertEquals(3, allResults.size());
    }
}
