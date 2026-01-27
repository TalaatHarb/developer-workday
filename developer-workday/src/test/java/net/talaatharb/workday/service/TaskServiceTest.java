package net.talaatharb.workday.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
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
import net.talaatharb.workday.event.task.*;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.RecurrenceRule;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;

/**
 * Tests for TaskService following the acceptance criteria.
 */
class TaskServiceTest {
    
    private DB database;
    private TaskRepository taskRepository;
    private EventDispatcher eventDispatcher;
    private EventLogger eventLogger;
    private TaskService taskService;
    private File dbFile;
    
    @BeforeEach
    void setUp() {
        dbFile = new File("test-taskservice-" + UUID.randomUUID() + ".db");
        database = DBMaker.fileDB(dbFile)
            .transactionEnable()
            .make();
        taskRepository = new TaskRepository(database);
        eventLogger = new EventLogger();
        eventDispatcher = new EventDispatcher(eventLogger);
        taskService = new TaskService(taskRepository, eventDispatcher);
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
    @DisplayName("Create a new task - saves via repository, publishes event, returns task")
    void testCreateTask() throws InterruptedException {
        // Given: valid task creation data
        CountDownLatch latch = new CountDownLatch(1);
        final TaskCreatedEvent[] capturedEvent = new TaskCreatedEvent[1];
        
        eventDispatcher.subscribe(TaskCreatedEvent.class, event -> {
            capturedEvent[0] = event;
            latch.countDown();
        });
        
        Task task = Task.builder()
            .title("New Task")
            .description("Task description")
            .priority(Priority.MEDIUM)
            .status(TaskStatus.TODO)
            .build();
        
        // When: createTask is called
        Task createdTask = taskService.createTask(task);
        
        // Then: the task should be saved via repository
        assertNotNull(createdTask.getId(), "Task should have an ID");
        assertNotNull(createdTask.getCreatedAt(), "Task should have createdAt timestamp");
        
        Optional<Task> savedTask = taskRepository.findById(createdTask.getId());
        assertTrue(savedTask.isPresent(), "Task should be in repository");
        assertEquals(createdTask.getTitle(), savedTask.get().getTitle());
        
        // And: a TaskCreatedEvent should be published
        assertTrue(latch.await(1, TimeUnit.SECONDS), "TaskCreatedEvent should be published");
        assertNotNull(capturedEvent[0], "Event should be captured");
        assertEquals(createdTask.getId(), capturedEvent[0].getTask().getId());
        
        // And: the created task should be returned
        assertNotNull(createdTask);
        assertEquals("New Task", createdTask.getTitle());
        assertEquals(Priority.MEDIUM, createdTask.getPriority());
    }
    
    @Test
    @DisplayName("Complete a task - sets status, timestamp, publishes event")
    void testCompleteTask() throws InterruptedException {
        // Given: an existing incomplete task
        Task task = Task.builder()
            .title("Task to Complete")
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .build();
        
        Task savedTask = taskRepository.save(task);
        
        CountDownLatch latch = new CountDownLatch(1);
        final TaskCompletedEvent[] capturedEvent = new TaskCompletedEvent[1];
        
        eventDispatcher.subscribe(TaskCompletedEvent.class, event -> {
            capturedEvent[0] = event;
            latch.countDown();
        });
        
        // When: completeTask is called
        Task completedTask = taskService.completeTask(savedTask.getId());
        
        // Then: the task status should be set to COMPLETED
        assertEquals(TaskStatus.COMPLETED, completedTask.getStatus());
        
        // And: completedAt should be set to current timestamp
        assertNotNull(completedTask.getCompletedAt());
        assertTrue(completedTask.getCompletedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        
        // And: a TaskCompletedEvent should be published
        assertTrue(latch.await(1, TimeUnit.SECONDS), "TaskCompletedEvent should be published");
        assertNotNull(capturedEvent[0]);
        assertEquals(savedTask.getId(), capturedEvent[0].getTaskId());
        assertEquals(completedTask.getCompletedAt(), capturedEvent[0].getCompletionTimestamp());
    }
    
    @Test
    @DisplayName("Handle recurring task completion - creates next occurrence")
    void testCompleteRecurringTask() throws InterruptedException {
        // Given: a recurring task
        RecurrenceRule recurrence = RecurrenceRule.builder()
            .type(RecurrenceRule.RecurrenceType.DAILY)
            .interval(1)
            .build();
        
        Task recurringTask = Task.builder()
            .title("Daily Recurring Task")
            .status(TaskStatus.TODO)
            .scheduledDate(LocalDate.now())
            .recurrence(recurrence)
            .priority(Priority.MEDIUM)
            .build();
        
        Task savedTask = taskRepository.save(recurringTask);
        
        CountDownLatch createdLatch = new CountDownLatch(1);
        final TaskCreatedEvent[] createdEvent = new TaskCreatedEvent[1];
        
        eventDispatcher.subscribe(TaskCreatedEvent.class, event -> {
            createdEvent[0] = event;
            createdLatch.countDown();
        });
        
        long initialCount = taskRepository.count();
        
        // When: the task is completed
        Task completedTask = taskService.completeTask(savedTask.getId());
        
        // Then: the original task should be marked complete
        assertEquals(TaskStatus.COMPLETED, completedTask.getStatus());
        assertNotNull(completedTask.getCompletedAt());
        
        // And: a new task instance should be created for the next occurrence
        assertTrue(createdLatch.await(1, TimeUnit.SECONDS), 
            "New task should be created");
        assertNotNull(createdEvent[0], "TaskCreatedEvent should be published for next occurrence");
        
        Task nextTask = createdEvent[0].getTask();
        assertNotNull(nextTask);
        assertEquals(recurringTask.getTitle(), nextTask.getTitle());
        assertEquals(TaskStatus.TODO, nextTask.getStatus());
        assertEquals(LocalDate.now().plusDays(1), nextTask.getScheduledDate());
        
        // Verify new task was saved
        assertEquals(initialCount + 1, taskRepository.count());
    }
    
    @Test
    @DisplayName("Update task publishes TaskUpdatedEvent")
    void testUpdateTask() throws InterruptedException {
        // Create initial task
        Task task = Task.builder()
            .title("Original Title")
            .description("Original description")
            .priority(Priority.LOW)
            .status(TaskStatus.TODO)
            .build();
        
        Task savedTask = taskRepository.save(task);
        
        CountDownLatch latch = new CountDownLatch(1);
        final TaskUpdatedEvent[] capturedEvent = new TaskUpdatedEvent[1];
        
        eventDispatcher.subscribe(TaskUpdatedEvent.class, event -> {
            capturedEvent[0] = event;
            latch.countDown();
        });
        
        // Update the task
        savedTask.setTitle("Updated Title");
        savedTask.setDescription("Updated description");
        savedTask.setPriority(Priority.HIGH);
        
        Task updatedTask = taskService.updateTask(savedTask);
        
        // Verify update
        assertEquals("Updated Title", updatedTask.getTitle());
        assertEquals(Priority.HIGH, updatedTask.getPriority());
        
        // Verify event
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(capturedEvent[0]);
        assertEquals("Original Title", capturedEvent[0].getOldTask().getTitle());
        assertEquals("Updated Title", capturedEvent[0].getNewTask().getTitle());
    }
    
    @Test
    @DisplayName("Priority change publishes TaskPriorityChangedEvent")
    void testUpdateTaskPriority() throws InterruptedException {
        Task task = Task.builder()
            .title("Task")
            .priority(Priority.LOW)
            .status(TaskStatus.TODO)
            .build();
        
        Task savedTask = taskRepository.save(task);
        
        CountDownLatch latch = new CountDownLatch(1);
        final TaskPriorityChangedEvent[] capturedEvent = new TaskPriorityChangedEvent[1];
        
        eventDispatcher.subscribe(TaskPriorityChangedEvent.class, event -> {
            capturedEvent[0] = event;
            latch.countDown();
        });
        
        savedTask.setPriority(Priority.HIGH);
        taskService.updateTask(savedTask);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(capturedEvent[0]);
        assertEquals(Priority.LOW, capturedEvent[0].getOldPriority());
        assertEquals(Priority.HIGH, capturedEvent[0].getNewPriority());
    }
    
    @Test
    @DisplayName("Schedule task publishes TaskScheduledEvent")
    void testScheduleTask() throws InterruptedException {
        Task task = Task.builder()
            .title("Task to Schedule")
            .status(TaskStatus.TODO)
            .build();
        
        Task savedTask = taskRepository.save(task);
        
        CountDownLatch latch = new CountDownLatch(1);
        final TaskScheduledEvent[] capturedEvent = new TaskScheduledEvent[1];
        
        eventDispatcher.subscribe(TaskScheduledEvent.class, event -> {
            capturedEvent[0] = event;
            latch.countDown();
        });
        
        LocalDate scheduledDate = LocalDate.now().plusDays(1);
        Task scheduledTask = taskService.scheduleTask(savedTask.getId(), scheduledDate);
        
        assertEquals(scheduledDate, scheduledTask.getScheduledDate());
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(capturedEvent[0]);
        assertEquals(scheduledDate, capturedEvent[0].getScheduledDate());
    }
    
    @Test
    @DisplayName("Delete task publishes TaskDeletedEvent")
    void testDeleteTask() throws InterruptedException {
        Task task = Task.builder()
            .title("Task to Delete")
            .status(TaskStatus.TODO)
            .build();
        
        Task savedTask = taskRepository.save(task);
        
        CountDownLatch latch = new CountDownLatch(1);
        final TaskDeletedEvent[] capturedEvent = new TaskDeletedEvent[1];
        
        eventDispatcher.subscribe(TaskDeletedEvent.class, event -> {
            capturedEvent[0] = event;
            latch.countDown();
        });
        
        taskService.deleteTask(savedTask.getId());
        
        assertFalse(taskRepository.existsById(savedTask.getId()));
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(capturedEvent[0]);
        assertEquals(savedTask.getId(), capturedEvent[0].getTask().getId());
    }
    
    @Test
    @DisplayName("Find tasks by various criteria")
    void testFindTasks() {
        UUID categoryId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        
        Task task1 = taskRepository.save(Task.builder()
            .title("Task 1")
            .status(TaskStatus.TODO)
            .categoryId(categoryId)
            .scheduledDate(today)
            .build());
        
        Task task2 = taskRepository.save(Task.builder()
            .title("Task 2")
            .status(TaskStatus.COMPLETED)
            .categoryId(categoryId)
            .build());
        
        // Test findAll
        assertEquals(2, taskService.findAll().size());
        
        // Test findByStatus
        assertEquals(1, taskService.findByStatus(TaskStatus.TODO).size());
        assertEquals(1, taskService.findByStatus(TaskStatus.COMPLETED).size());
        
        // Test findByCategoryId
        assertEquals(2, taskService.findByCategoryId(categoryId).size());
        
        // Test findByScheduledDate
        assertEquals(1, taskService.findByScheduledDate(today).size());
        
        // Test findById
        assertTrue(taskService.findById(task1.getId()).isPresent());
    }
}
