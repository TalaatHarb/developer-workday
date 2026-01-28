package net.talaatharb.workday.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.time.Duration;
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
import net.talaatharb.workday.event.time.TimeTrackedEvent;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;

/**
 * Tests for TimeTrackingService following the acceptance criteria.
 */
class TimeTrackingServiceTest {
    
    private DB database;
    private TaskRepository taskRepository;
    private EventDispatcher eventDispatcher;
    private EventLogger eventLogger;
    private TaskService taskService;
    private TimeTrackingService timeTrackingService;
    private File dbFile;
    
    @BeforeEach
    void setUp() {
        dbFile = new File("test-timetracking-" + UUID.randomUUID() + ".db");
        database = DBMaker.fileDB(dbFile)
            .transactionEnable()
            .make();
        taskRepository = new TaskRepository(database);
        eventLogger = new EventLogger();
        eventDispatcher = new EventDispatcher(eventLogger);
        taskService = new TaskService(taskRepository, eventDispatcher);
        timeTrackingService = new TimeTrackingService(eventDispatcher, taskService);
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
    @DisplayName("Start task timer - timer starts and is active")
    void testStartTimer() {
        // Given: a task without active timer
        Task task = taskService.createTask(Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .build());
        
        assertFalse(timeTrackingService.isTimerActive(task.getId()));
        
        // When: clicking the play button (starting timer)
        timeTrackingService.startTimer(task.getId());
        
        // Then: a timer should start for that task
        assertTrue(timeTrackingService.isTimerActive(task.getId()));
        
        // And: the elapsed time should be available
        Optional<Duration> elapsed = timeTrackingService.getElapsedTime(task.getId());
        assertTrue(elapsed.isPresent());
        assertTrue(elapsed.get().getSeconds() >= 0);
    }
    
    @Test
    @DisplayName("Stop task timer - timer stops and time is recorded")
    void testStopTimer() throws InterruptedException {
        // Given: a task with active timer
        Task task = taskService.createTask(Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .build());
        
        CountDownLatch latch = new CountDownLatch(1);
        final TimeTrackedEvent[] capturedEvent = new TimeTrackedEvent[1];
        
        eventDispatcher.subscribe(TimeTrackedEvent.class, event -> {
            capturedEvent[0] = event;
            latch.countDown();
        });
        
        timeTrackingService.startTimer(task.getId());
        
        // Wait a bit to track some time
        Thread.sleep(100);
        
        // When: clicking the stop button
        Duration elapsed = timeTrackingService.stopTimer(task.getId());
        
        // Then: the timer should stop
        assertFalse(timeTrackingService.isTimerActive(task.getId()));
        
        // And: the elapsed time should be added to actual duration
        Task updatedTask = taskService.findById(task.getId()).orElseThrow();
        assertNotNull(updatedTask.getActualDuration());
        assertTrue(updatedTask.getActualDuration().toMillis() > 0);
        
        // And: a TimeTrackedEvent should be published
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(capturedEvent[0]);
        assertEquals(task.getId(), capturedEvent[0].getTaskId());
        assertNotNull(capturedEvent[0].getTrackedDuration());
    }
    
    @Test
    @DisplayName("Get elapsed time - returns current elapsed time for active timer")
    void testGetElapsedTime() throws InterruptedException {
        Task task = taskService.createTask(Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .build());
        
        timeTrackingService.startTimer(task.getId());
        
        // Wait a bit
        Thread.sleep(100);
        
        Optional<Duration> elapsed = timeTrackingService.getElapsedTime(task.getId());
        assertTrue(elapsed.isPresent());
        assertTrue(elapsed.get().toMillis() >= 100);
    }
    
    @Test
    @DisplayName("Start timer when already active - throws exception")
    void testStartTimerWhenAlreadyActive() {
        Task task = taskService.createTask(Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .build());
        
        timeTrackingService.startTimer(task.getId());
        
        assertThrows(IllegalStateException.class, () -> {
            timeTrackingService.startTimer(task.getId());
        });
    }
    
    @Test
    @DisplayName("Stop timer when not active - throws exception")
    void testStopTimerWhenNotActive() {
        Task task = taskService.createTask(Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .build());
        
        assertThrows(IllegalStateException.class, () -> {
            timeTrackingService.stopTimer(task.getId());
        });
    }
    
    @Test
    @DisplayName("Add manual time - time is added to task")
    void testAddManualTime() throws InterruptedException {
        // Given: a task
        Task task = taskService.createTask(Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .build());
        
        CountDownLatch latch = new CountDownLatch(1);
        eventDispatcher.subscribe(TimeTrackedEvent.class, event -> latch.countDown());
        
        // When: adding manual time entry
        Duration manualDuration = Duration.ofHours(2);
        LocalDateTime timestamp = LocalDateTime.now();
        timeTrackingService.addManualTime(task.getId(), manualDuration, timestamp);
        
        // Then: time should be added to task
        Task updatedTask = taskService.findById(task.getId()).orElseThrow();
        assertNotNull(updatedTask.getActualDuration());
        assertEquals(manualDuration, updatedTask.getActualDuration());
        
        // And: TimeTrackedEvent should be published
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("Get total time for task - returns sum of all tracked time")
    void testGetTotalTimeForTask() {
        Task task = taskService.createTask(Task.builder()
            .title("Test Task")
            .status(TaskStatus.TODO)
            .actualDuration(Duration.ofHours(5))
            .build());
        
        Duration totalTime = timeTrackingService.getTotalTimeForTask(task.getId());
        assertEquals(Duration.ofHours(5), totalTime);
    }
    
    @Test
    @DisplayName("Get total time for category - returns sum for all tasks in category")
    void testGetTotalTimeForCategory() {
        UUID categoryId = UUID.randomUUID();
        
        taskService.createTask(Task.builder()
            .title("Task 1")
            .categoryId(categoryId)
            .actualDuration(Duration.ofHours(2))
            .build());
        
        taskService.createTask(Task.builder()
            .title("Task 2")
            .categoryId(categoryId)
            .actualDuration(Duration.ofHours(3))
            .build());
        
        Duration totalTime = timeTrackingService.getTotalTimeForCategory(categoryId);
        assertEquals(Duration.ofHours(5), totalTime);
    }
    
    @Test
    @DisplayName("Stop all timers - all active timers are stopped")
    void testStopAllTimers() throws InterruptedException {
        Task task1 = taskService.createTask(Task.builder().title("Task 1").status(TaskStatus.TODO).build());
        Task task2 = taskService.createTask(Task.builder().title("Task 2").status(TaskStatus.TODO).build());
        
        timeTrackingService.startTimer(task1.getId());
        timeTrackingService.startTimer(task2.getId());
        
        Thread.sleep(100);
        
        assertEquals(2, timeTrackingService.getActiveTimerCount());
        
        timeTrackingService.stopAllTimers();
        
        assertEquals(0, timeTrackingService.getActiveTimerCount());
        assertFalse(timeTrackingService.isTimerActive(task1.getId()));
        assertFalse(timeTrackingService.isTimerActive(task2.getId()));
    }
}
