package net.talaatharb.workday.service;

import net.talaatharb.workday.dtos.ProductivityStatistics;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for StatisticsService.
 */
class StatisticsServiceTest {
    
    private DB database;
    private TaskRepository taskRepository;
    private StatisticsService statisticsService;
    
    @BeforeEach
    void setUp() {
        database = DBMaker.memoryDB().make();
        taskRepository = new TaskRepository(database);
        statisticsService = new StatisticsService(taskRepository);
    }
    
    @AfterEach
    void tearDown() {
        if (database != null && !database.isClosed()) {
            database.close();
        }
    }
    
    @Test
    void testCalculateStatistics_EmptyRepository() {
        // When
        ProductivityStatistics stats = statisticsService.calculateStatistics();
        
        // Then
        assertEquals(0, stats.getTotalTasksCompleted());
        assertEquals(0, stats.getTotalTasksCreated());
        assertEquals(0.0, stats.getCompletionRate());
    }
    
    @Test
    void testCalculateStatistics_WithCompletedTasks() {
        // Given
        createCompletedTask("Task 1", LocalDate.now());
        createCompletedTask("Task 2", LocalDate.now().minusDays(1));
        createOpenTask("Task 3");
        
        // When
        ProductivityStatistics stats = statisticsService.calculateStatistics();
        
        // Then
        assertEquals(2, stats.getTotalTasksCompleted());
        assertEquals(3, stats.getTotalTasksCreated());
        assertEquals(66.67, stats.getCompletionRate(), 0.01);
        assertEquals(1, stats.getTasksCompletedToday());
    }
    
    @Test
    void testCalculateCurrentStreak() {
        // Given: Tasks completed on consecutive days
        createCompletedTask("Task Today", LocalDate.now());
        createCompletedTask("Task Yesterday", LocalDate.now().minusDays(1));
        createCompletedTask("Task 2 days ago", LocalDate.now().minusDays(2));
        
        // When
        ProductivityStatistics stats = statisticsService.calculateStatistics();
        
        // Then
        assertEquals(3, stats.getCurrentStreak());
    }
    
    @Test
    void testCalculateLongestStreak() {
        // Given: Two separate streaks
        createCompletedTask("Task 1", LocalDate.now().minusDays(10));
        createCompletedTask("Task 2", LocalDate.now().minusDays(9));
        createCompletedTask("Task 3", LocalDate.now().minusDays(8));
        // Gap
        createCompletedTask("Task 4", LocalDate.now().minusDays(2));
        createCompletedTask("Task 5", LocalDate.now().minusDays(1));
        createCompletedTask("Task 6", LocalDate.now());
        
        // When
        ProductivityStatistics stats = statisticsService.calculateStatistics();
        
        // Then: Longest streak is 3
        assertTrue(stats.getLongestStreak() >= 3);
    }
    
    @Test
    void testDailyCompletions() {
        // Given
        LocalDate today = LocalDate.now();
        createCompletedTask("Task 1", today);
        createCompletedTask("Task 2", today);
        createCompletedTask("Task 3", today.minusDays(1));
        
        // When
        ProductivityStatistics stats = statisticsService.calculateStatistics();
        
        // Then
        Map<LocalDate, Integer> dailyCompletions = stats.getDailyCompletions();
        assertNotNull(dailyCompletions);
        assertEquals(2, dailyCompletions.get(today));
        assertEquals(1, dailyCompletions.get(today.minusDays(1)));
    }
    
    private void createCompletedTask(String title, LocalDate completedDate) {
        Task task = Task.builder()
            .title(title)
            .status(TaskStatus.COMPLETED)
            .priority(Priority.MEDIUM)
            .completedAt(completedDate.atTime(12, 0))
            .build();
        taskRepository.save(task);
    }
    
    private void createOpenTask(String title) {
        Task task = Task.builder()
            .title(title)
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .build();
        taskRepository.save(task);
    }
}
