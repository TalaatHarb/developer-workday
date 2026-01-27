package net.talaatharb.workday.service;

import net.talaatharb.workday.dtos.WeeklyStatistics;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.review.WeeklyReviewCompletedEvent;
import net.talaatharb.workday.event.review.WeeklyReviewStartedEvent;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for WeeklyReviewService.
 */
class WeeklyReviewServiceTest {
    
    private DB database;
    private TaskRepository taskRepository;
    private EventDispatcher eventDispatcher;
    private WeeklyReviewService weeklyReviewService;
    
    @BeforeEach
    void setUp() {
        database = DBMaker.memoryDB().make();
        taskRepository = new TaskRepository(database);
        eventDispatcher = new EventDispatcher();
        weeklyReviewService = new WeeklyReviewService(taskRepository, eventDispatcher);
    }
    
    @AfterEach
    void tearDown() {
        if (database != null && !database.isClosed()) {
            database.close();
        }
    }
    
    @Test
    void testStartWeeklyReview_CurrentWeek() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        // Create test tasks
        Task completedTask = createCompletedTask("Task 1", weekStart.plusDays(1), Priority.HIGH);
        taskRepository.save(completedTask);
        
        // When
        WeeklyStatistics stats = weeklyReviewService.startWeeklyReview();
        
        // Then
        assertNotNull(stats);
        assertEquals(weekStart, stats.getWeekStartDate());
        assertEquals(weekEnd, stats.getWeekEndDate());
        assertEquals(1, stats.getTotalTasksCompleted());
    }
    
    @Test
    void testCalculateWeeklyStatistics_WithCompletedTasks() {
        // Given
        LocalDate weekStart = LocalDate.of(2024, 1, 1); // Monday
        LocalDate weekEnd = LocalDate.of(2024, 1, 7);   // Sunday
        
        // Create completed tasks in the week
        Task task1 = createCompletedTask("High Priority Task", weekStart.plusDays(1), Priority.HIGH);
        Task task2 = createCompletedTask("Medium Priority Task", weekStart.plusDays(2), Priority.MEDIUM);
        Task task3 = createCompletedTask("Low Priority Task", weekStart.plusDays(3), Priority.LOW);
        
        taskRepository.save(task1);
        taskRepository.save(task2);
        taskRepository.save(task3);
        
        // When
        WeeklyStatistics stats = weeklyReviewService.calculateWeeklyStatistics(weekStart, weekEnd);
        
        // Then
        assertEquals(3, stats.getTotalTasksCompleted());
        assertEquals(1, stats.getHighPriorityCompleted());
        assertEquals(1, stats.getMediumPriorityCompleted());
        assertEquals(1, stats.getLowPriorityCompleted());
    }
    
    @Test
    void testCalculateWeeklyStatistics_CompletionRate() {
        // Given
        LocalDate weekStart = LocalDate.of(2024, 1, 1);
        LocalDate weekEnd = LocalDate.of(2024, 1, 7);
        
        // Create 3 completed tasks
        Task completed1 = createCompletedTask("Completed 1", weekStart.plusDays(1), Priority.HIGH);
        Task completed2 = createCompletedTask("Completed 2", weekStart.plusDays(2), Priority.MEDIUM);
        Task completed3 = createCompletedTask("Completed 3", weekStart.plusDays(3), Priority.LOW);
        
        taskRepository.save(completed1);
        taskRepository.save(completed2);
        taskRepository.save(completed3);
        
        // Create 2 planned but not completed tasks
        Task planned1 = Task.builder()
            .title("Planned 1")
            .scheduledDate(weekStart.plusDays(4))
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .build();
        Task planned2 = Task.builder()
            .title("Planned 2")
            .dueDate(weekStart.plusDays(5))
            .status(TaskStatus.TODO)
            .priority(Priority.LOW)
            .build();
            
        taskRepository.save(planned1);
        taskRepository.save(planned2);
        
        // When
        WeeklyStatistics stats = weeklyReviewService.calculateWeeklyStatistics(weekStart, weekEnd);
        
        // Then
        assertEquals(5, stats.getTotalTasksPlanned()); // 3 completed + 2 planned
        assertEquals(3, stats.getTotalTasksCompleted());
        assertEquals(60.0, stats.getCompletionRate(), 0.1); // 3/5 = 60%
    }
    
    @Test
    void testGetCompletedTasksForWeek() {
        // Given
        LocalDate weekStart = LocalDate.of(2024, 1, 1);
        LocalDate weekEnd = LocalDate.of(2024, 1, 7);
        
        // Create completed tasks in the week
        Task task1 = createCompletedTask("Task 1", weekStart.plusDays(1), Priority.HIGH);
        Task task2 = createCompletedTask("Task 2", weekStart.plusDays(2), Priority.MEDIUM);
        
        // Create completed task outside the week
        Task taskOutside = createCompletedTask("Task Outside", weekStart.minusDays(1), Priority.LOW);
        
        taskRepository.save(task1);
        taskRepository.save(task2);
        taskRepository.save(taskOutside);
        
        // When
        List<Task> completedTasks = weeklyReviewService.getCompletedTasksForWeek(weekStart, weekEnd);
        
        // Then
        assertEquals(2, completedTasks.size());
        assertTrue(completedTasks.stream().anyMatch(t -> t.getTitle().equals("Task 1")));
        assertTrue(completedTasks.stream().anyMatch(t -> t.getTitle().equals("Task 2")));
    }
    
    @Test
    void testGetUpcomingTasks() {
        // Given
        LocalDate today = LocalDate.now();
        
        // Create upcoming scheduled task
        Task scheduledTask = Task.builder()
            .title("Scheduled Task")
            .scheduledDate(today.plusDays(3))
            .status(TaskStatus.TODO)
            .priority(Priority.HIGH)
            .build();
            
        // Create upcoming due task
        Task dueTask = Task.builder()
            .title("Due Task")
            .dueDate(today.plusDays(5))
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .build();
            
        // Create past task (should not be included)
        Task pastTask = Task.builder()
            .title("Past Task")
            .scheduledDate(today.minusDays(2))
            .status(TaskStatus.TODO)
            .priority(Priority.LOW)
            .build();
            
        taskRepository.save(scheduledTask);
        taskRepository.save(dueTask);
        taskRepository.save(pastTask);
        
        // When
        List<Task> upcomingTasks = weeklyReviewService.getUpcomingTasks();
        
        // Then
        assertEquals(2, upcomingTasks.size());
        assertTrue(upcomingTasks.stream().anyMatch(t -> t.getTitle().equals("Scheduled Task")));
        assertTrue(upcomingTasks.stream().anyMatch(t -> t.getTitle().equals("Due Task")));
    }
    
    @Test
    void testCompleteWeeklyReview_PublishesEvent() {
        // Given
        LocalDate weekStart = LocalDate.of(2024, 1, 1);
        LocalDate weekEnd = LocalDate.of(2024, 1, 7);
        int reviewedCount = 5;
        
        // Subscribe to event
        final boolean[] eventReceived = {false};
        eventDispatcher.subscribe(WeeklyReviewCompletedEvent.class, event -> {
            eventReceived[0] = true;
            assertEquals(weekStart, event.getWeekStartDate());
            assertEquals(weekEnd, event.getWeekEndDate());
            assertEquals(reviewedCount, event.getReviewedTasksCount());
        });
        
        // When
        weeklyReviewService.completeWeeklyReview(weekStart, weekEnd, reviewedCount);
        
        // Then
        assertTrue(eventReceived[0]);
    }
    
    @Test
    void testStartWeeklyReview_PublishesEvent() {
        // Given
        LocalDate weekStart = LocalDate.of(2024, 1, 1);
        LocalDate weekEnd = LocalDate.of(2024, 1, 7);
        
        // Subscribe to event
        final boolean[] eventReceived = {false};
        eventDispatcher.subscribe(WeeklyReviewStartedEvent.class, event -> {
            eventReceived[0] = true;
            assertEquals(weekStart, event.getWeekStartDate());
            assertEquals(weekEnd, event.getWeekEndDate());
        });
        
        // When
        weeklyReviewService.startWeeklyReview(weekStart, weekEnd);
        
        // Then
        assertTrue(eventReceived[0]);
    }
    
    private Task createCompletedTask(String title, LocalDate completedDate, Priority priority) {
        return Task.builder()
            .title(title)
            .status(TaskStatus.COMPLETED)
            .priority(priority)
            .completedAt(completedDate.atTime(12, 0))
            .scheduledDate(completedDate)
            .build();
    }
}
