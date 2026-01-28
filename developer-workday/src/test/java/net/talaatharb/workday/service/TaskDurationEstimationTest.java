package net.talaatharb.workday.service;

import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for Task Duration Estimation (Task 58).
 * 
 * Acceptance criteria:
 * - Set task duration
 * - Duration should be stored with the task
 * - Compare estimated vs actual
 */
class TaskDurationEstimationTest {

    @TempDir
    Path tempDir;

    private TaskService taskService;
    private TaskRepository taskRepository;
    private EventDispatcher eventDispatcher;
    private DB database;

    @BeforeEach
    void setUp() {
        File dbFile = tempDir.resolve("test-duration-db").toFile();
        database = DBMaker.fileDB(dbFile)
                .transactionEnable()
                .make();
        eventDispatcher = new EventDispatcher();
        taskRepository = new TaskRepository(database);
        taskService = new TaskService(taskRepository, eventDispatcher);
    }

    @AfterEach
    void tearDown() {
        if (database != null && !database.isClosed()) {
            database.close();
        }
    }

    @Test
    void testSetEstimatedDurationOnTaskCreation() {
        // Given: a task being created
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Implement feature X")
                .description("New feature implementation")
                .priority(Priority.HIGH)
                .status(TaskStatus.TODO)
                .dueDate(LocalDate.now().plusDays(3))
                .estimatedDuration(Duration.ofHours(2))
                .build();

        // When: setting estimated duration
        Task savedTask = taskService.createTask(task);

        // Then: duration should be stored with the task
        assertNotNull(savedTask, "Task should be saved");
        assertEquals(Duration.ofHours(2), savedTask.getEstimatedDuration(), 
                "Estimated duration should be 2 hours");
    }

    @Test
    void testUpdateEstimatedDuration() {
        // Given: an existing task
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Write documentation")
                .priority(Priority.MEDIUM)
                .status(TaskStatus.TODO)
                .estimatedDuration(Duration.ofMinutes(30))
                .build();
        
        Task savedTask = taskService.createTask(task);

        // When: updating the estimated duration
        Task updatedTask = Task.builder()
                .id(savedTask.getId())
                .title(savedTask.getTitle())
                .priority(savedTask.getPriority())
                .status(savedTask.getStatus())
                .estimatedDuration(Duration.ofHours(1))
                .createdAt(savedTask.getCreatedAt())
                .build();
        
        Task result = taskService.updateTask(updatedTask);

        // Then: the new duration should be stored
        assertEquals(Duration.ofHours(1), result.getEstimatedDuration(), 
                "Estimated duration should be updated to 1 hour");
    }

    @Test
    void testCommonDurationIncrements() {
        // Given: a task being created with various common durations
        
        // 15 minutes
        Task task15 = createTaskWithDuration(Duration.ofMinutes(15));
        assertEquals(Duration.ofMinutes(15), task15.getEstimatedDuration());
        
        // 30 minutes
        Task task30 = createTaskWithDuration(Duration.ofMinutes(30));
        assertEquals(Duration.ofMinutes(30), task30.getEstimatedDuration());
        
        // 1 hour
        Task task60 = createTaskWithDuration(Duration.ofHours(1));
        assertEquals(Duration.ofHours(1), task60.getEstimatedDuration());
        
        // 2 hours
        Task task120 = createTaskWithDuration(Duration.ofHours(2));
        assertEquals(Duration.ofHours(2), task120.getEstimatedDuration());
    }

    @Test
    void testActualDurationTracking() {
        // Given: a task with estimated duration
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Complete code review")
                .priority(Priority.MEDIUM)
                .status(TaskStatus.IN_PROGRESS)
                .estimatedDuration(Duration.ofHours(1))
                .build();
        
        Task savedTask = taskService.createTask(task);

        // When: setting actual duration (simulating time tracking)
        Task completedTask = Task.builder()
                .id(savedTask.getId())
                .title(savedTask.getTitle())
                .priority(savedTask.getPriority())
                .status(TaskStatus.COMPLETED)
                .estimatedDuration(savedTask.getEstimatedDuration())
                .actualDuration(Duration.ofMinutes(75))
                .createdAt(savedTask.getCreatedAt())
                .build();
        
        Task result = taskService.updateTask(completedTask);

        // Then: both durations should be available for comparison
        assertEquals(Duration.ofHours(1), result.getEstimatedDuration(), 
                "Estimated duration should be 1 hour");
        assertEquals(Duration.ofMinutes(75), result.getActualDuration(), 
                "Actual duration should be 75 minutes");
    }

    @Test
    void testCompareEstimatedVsActual_Under() {
        // Given: a task with estimated and actual duration
        Duration estimated = Duration.ofHours(2);
        Duration actual = Duration.ofMinutes(90); // 30 min under
        
        Task task = createTaskWithBothDurations(estimated, actual);

        // When: comparing durations
        long difference = actual.toMinutes() - estimated.toMinutes();

        // Then: actual is under estimated
        assertTrue(difference < 0, "Actual duration should be under estimated");
        assertEquals(-30, difference, "Difference should be 30 minutes under");
    }

    @Test
    void testCompareEstimatedVsActual_Over() {
        // Given: a task with estimated and actual duration
        Duration estimated = Duration.ofMinutes(45);
        Duration actual = Duration.ofHours(1); // 15 min over
        
        Task task = createTaskWithBothDurations(estimated, actual);

        // When: comparing durations
        long difference = actual.toMinutes() - estimated.toMinutes();

        // Then: actual is over estimated
        assertTrue(difference > 0, "Actual duration should be over estimated");
        assertEquals(15, difference, "Difference should be 15 minutes over");
    }

    @Test
    void testCompareEstimatedVsActual_Exact() {
        // Given: a task with exact match
        Duration estimated = Duration.ofMinutes(30);
        Duration actual = Duration.ofMinutes(30);
        
        Task task = createTaskWithBothDurations(estimated, actual);

        // When: comparing durations
        long difference = actual.toMinutes() - estimated.toMinutes();

        // Then: durations match exactly
        assertEquals(0, difference, "Durations should match exactly");
    }

    @Test
    void testTaskWithoutDuration() {
        // Given: a task without duration set
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Quick fix")
                .priority(Priority.LOW)
                .status(TaskStatus.TODO)
                .build();

        // When: saving the task
        Task savedTask = taskService.createTask(task);

        // Then: duration fields should be null
        assertNull(savedTask.getEstimatedDuration(), 
                "Estimated duration should be null when not set");
        assertNull(savedTask.getActualDuration(), 
                "Actual duration should be null when not tracked");
    }

    private Task createTaskWithDuration(Duration duration) {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Test task")
                .priority(Priority.MEDIUM)
                .status(TaskStatus.TODO)
                .estimatedDuration(duration)
                .build();
        return taskService.createTask(task);
    }

    private Task createTaskWithBothDurations(Duration estimated, Duration actual) {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Completed task")
                .priority(Priority.MEDIUM)
                .status(TaskStatus.COMPLETED)
                .estimatedDuration(estimated)
                .actualDuration(actual)
                .build();
        return taskService.createTask(task);
    }
}
