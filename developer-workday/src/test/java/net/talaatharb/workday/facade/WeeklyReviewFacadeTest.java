package net.talaatharb.workday.facade;

import net.talaatharb.workday.dtos.TaskDTO;
import net.talaatharb.workday.dtos.WeeklyStatistics;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.mapper.TaskMapper;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;
import net.talaatharb.workday.service.TaskService;
import net.talaatharb.workday.service.WeeklyReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for WeeklyReviewFacade.
 */
class WeeklyReviewFacadeTest {
    
    private DB database;
    private TaskRepository taskRepository;
    private EventDispatcher eventDispatcher;
    private TaskService taskService;
    private WeeklyReviewService weeklyReviewService;
    private WeeklyReviewFacade weeklyReviewFacade;
    private TaskMapper taskMapper;
    
    @BeforeEach
    void setUp() {
        database = DBMaker.memoryDB().make();
        taskRepository = new TaskRepository(database);
        eventDispatcher = new EventDispatcher();
        taskService = new TaskService(taskRepository, eventDispatcher);
        weeklyReviewService = new WeeklyReviewService(taskRepository, eventDispatcher);
        taskMapper = TaskMapper.INSTANCE;
        weeklyReviewFacade = new WeeklyReviewFacade(weeklyReviewService, taskService, taskMapper);
    }
    
    @AfterEach
    void tearDown() {
        if (database != null && !database.isClosed()) {
            database.close();
        }
    }
    
    @Test
    void testStartWeeklyReview_ReturnsStatistics() {
        // Given
        LocalDate weekStart = LocalDate.of(2024, 1, 1);
        LocalDate weekEnd = LocalDate.of(2024, 1, 7);
        
        // Create a completed task
        Task completedTask = Task.builder()
            .title("Completed Task")
            .status(TaskStatus.COMPLETED)
            .priority(Priority.HIGH)
            .completedAt(weekStart.plusDays(2).atTime(10, 0))
            .scheduledDate(weekStart.plusDays(2))
            .build();
        taskRepository.save(completedTask);
        
        // When
        WeeklyStatistics stats = weeklyReviewFacade.startWeeklyReview(weekStart, weekEnd);
        
        // Then
        assertNotNull(stats);
        assertEquals(weekStart, stats.getWeekStartDate());
        assertEquals(weekEnd, stats.getWeekEndDate());
        assertEquals(1, stats.getTotalTasksCompleted());
    }
    
    @Test
    void testGetCompletedTasksForWeek_ReturnsDTOs() {
        // Given
        LocalDate weekStart = LocalDate.of(2024, 1, 1);
        LocalDate weekEnd = LocalDate.of(2024, 1, 7);
        
        // Create completed tasks
        Task task1 = Task.builder()
            .title("Task 1")
            .status(TaskStatus.COMPLETED)
            .priority(Priority.HIGH)
            .completedAt(weekStart.plusDays(1).atTime(9, 0))
            .scheduledDate(weekStart.plusDays(1))
            .build();
        Task task2 = Task.builder()
            .title("Task 2")
            .status(TaskStatus.COMPLETED)
            .priority(Priority.MEDIUM)
            .completedAt(weekStart.plusDays(3).atTime(14, 0))
            .scheduledDate(weekStart.plusDays(3))
            .build();
            
        taskRepository.save(task1);
        taskRepository.save(task2);
        
        // When
        List<TaskDTO> tasks = weeklyReviewFacade.getCompletedTasksForWeek(weekStart, weekEnd);
        
        // Then
        assertEquals(2, tasks.size());
        // Should be sorted by completedAt descending
        assertEquals("Task 2", tasks.get(0).getTitle());
        assertEquals("Task 1", tasks.get(1).getTitle());
    }
    
    @Test
    void testGetUpcomingTasks_ReturnsSortedDTOs() {
        // Given
        LocalDate today = LocalDate.now();
        
        // Create upcoming tasks
        Task task1 = Task.builder()
            .title("Task Due Soon")
            .status(TaskStatus.TODO)
            .priority(Priority.HIGH)
            .dueDate(today.plusDays(1))
            .build();
        Task task2 = Task.builder()
            .title("Task Scheduled Later")
            .status(TaskStatus.TODO)
            .priority(Priority.MEDIUM)
            .scheduledDate(today.plusDays(5))
            .build();
        Task task3 = Task.builder()
            .title("Task Due Later")
            .status(TaskStatus.TODO)
            .priority(Priority.LOW)
            .dueDate(today.plusDays(10))
            .build();
            
        taskRepository.save(task1);
        taskRepository.save(task2);
        taskRepository.save(task3);
        
        // When
        List<TaskDTO> tasks = weeklyReviewFacade.getUpcomingTasks();
        
        // Then
        assertEquals(3, tasks.size());
        // Should be sorted by date then priority
        assertEquals("Task Due Soon", tasks.get(0).getTitle());
    }
    
    @Test
    void testCompleteWeeklyReview() {
        // Given
        LocalDate weekStart = LocalDate.of(2024, 1, 1);
        LocalDate weekEnd = LocalDate.of(2024, 1, 7);
        int reviewedCount = 5;
        
        // When/Then - should not throw exception
        assertDoesNotThrow(() -> 
            weeklyReviewFacade.completeWeeklyReview(weekStart, weekEnd, reviewedCount)
        );
    }
}
