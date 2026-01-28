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

import net.talaatharb.workday.dtos.CalendarMonthView;
import net.talaatharb.workday.dtos.CalendarWeekView;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.EventLogger;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;
import net.talaatharb.workday.service.TaskService;

/**
 * Tests for CalendarFacade following the acceptance criteria.
 * 
 * Feature: Calendar Facade
 *   Scenario: Get tasks for calendar month view
 *     Given a specific month and year
 *     When getTasksForMonth is called
 *     Then tasks should be grouped by date
 *     And the result should be optimized for calendar grid display
 *
 *   Scenario: Get tasks for calendar week view
 *     Given a specific week
 *     When getTasksForWeek is called
 *     Then tasks should be grouped by date and time slots
 *     And all-day tasks should be separated from timed tasks
 */
class CalendarFacadeTest {
    
    private DB database;
    private TaskRepository taskRepository;
    private EventDispatcher eventDispatcher;
    private EventLogger eventLogger;
    private TaskService taskService;
    private CalendarFacade calendarFacade;
    private File dbFile;
    
    @BeforeEach
    void setUp() {
        dbFile = new File("test-calendarfacade-" + UUID.randomUUID() + ".db");
        database = DBMaker.fileDB(dbFile)
            .transactionEnable()
            .make();
        taskRepository = new TaskRepository(database);
        eventLogger = new EventLogger();
        eventDispatcher = new EventDispatcher(eventLogger);
        taskService = new TaskService(taskRepository, eventDispatcher);
        calendarFacade = new CalendarFacade(taskService);
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
    @DisplayName("Get tasks for month - groups tasks by date for calendar grid display")
    void testGetTasksForMonth() {
        // Given: a specific month and year (January 2024)
        int year = 2024;
        int month = 1;
        
        // And: tasks on different days
        LocalDate jan5 = LocalDate.of(2024, 1, 5);
        LocalDate jan10 = LocalDate.of(2024, 1, 10);
        LocalDate jan15 = LocalDate.of(2024, 1, 15);
        
        taskRepository.save(Task.builder()
            .title("Task on Jan 5")
            .status(TaskStatus.TODO)
            .dueDate(jan5)
            .build());
        
        taskRepository.save(Task.builder()
            .title("Another task on Jan 5")
            .status(TaskStatus.TODO)
            .dueDate(jan5)
            .build());
        
        taskRepository.save(Task.builder()
            .title("Task on Jan 10")
            .status(TaskStatus.TODO)
            .dueDate(jan10)
            .build());
        
        taskRepository.save(Task.builder()
            .title("Task on Jan 15")
            .status(TaskStatus.TODO)
            .dueDate(jan15)
            .build());
        
        // When: getTasksForMonth is called
        CalendarMonthView monthView = calendarFacade.getTasksForMonth(year, month);
        
        // Then: tasks should be grouped by date
        assertNotNull(monthView);
        assertEquals(year, monthView.getYear());
        assertEquals(month, monthView.getMonth());
        
        // And: the result should be optimized for calendar grid display
        // (all days of the month should be present)
        assertEquals(31, monthView.getDays().size(), "January 2024 has 31 days");
        
        // And: days should be in order
        for (int i = 0; i < monthView.getDays().size(); i++) {
            assertEquals(i + 1, monthView.getDays().get(i).getDate().getDayOfMonth());
        }
        
        // And: Jan 5 should have 2 tasks
        CalendarMonthView.DayWithTasks jan5Day = monthView.getDays().get(4); // 0-indexed
        assertEquals(jan5, jan5Day.getDate());
        assertEquals(2, jan5Day.getTasks().size(), "Jan 5 should have 2 tasks");
        
        // And: Jan 10 should have 1 task
        CalendarMonthView.DayWithTasks jan10Day = monthView.getDays().get(9);
        assertEquals(jan10, jan10Day.getDate());
        assertEquals(1, jan10Day.getTasks().size(), "Jan 10 should have 1 task");
        
        // And: Jan 15 should have 1 task
        CalendarMonthView.DayWithTasks jan15Day = monthView.getDays().get(14);
        assertEquals(jan15, jan15Day.getDate());
        assertEquals(1, jan15Day.getTasks().size(), "Jan 15 should have 1 task");
        
        // And: days without tasks should have empty lists
        CalendarMonthView.DayWithTasks jan1Day = monthView.getDays().get(0);
        assertEquals(0, jan1Day.getTasks().size(), "Jan 1 should have no tasks");
    }
    
    @Test
    @DisplayName("Get tasks for month - handles month with no tasks")
    void testGetTasksForMonth_NoTasks() {
        // Given: a month with no tasks
        int year = 2024;
        int month = 2; // February
        
        // When: getTasksForMonth is called
        CalendarMonthView monthView = calendarFacade.getTasksForMonth(year, month);
        
        // Then: all days should have empty task lists
        assertNotNull(monthView);
        assertEquals(29, monthView.getDays().size(), "February 2024 has 29 days (leap year)");
        
        for (CalendarMonthView.DayWithTasks day : monthView.getDays()) {
            assertEquals(0, day.getTasks().size(), "Day should have no tasks");
        }
    }
    
    @Test
    @DisplayName("Get tasks for month - handles tasks without due date")
    void testGetTasksForMonth_TasksWithoutDueDate() {
        // Given: tasks without due date
        taskRepository.save(Task.builder()
            .title("Task without due date")
            .status(TaskStatus.TODO)
            .dueDate(null)
            .build());
        
        // When: getTasksForMonth is called
        CalendarMonthView monthView = calendarFacade.getTasksForMonth(2024, 1);
        
        // Then: tasks without due date should not appear in any day
        int totalTasks = monthView.getDays().stream()
            .mapToInt(day -> day.getTasks().size())
            .sum();
        assertEquals(0, totalTasks, "Tasks without due date should not appear");
    }
    
    @Test
    @DisplayName("Get tasks for week - groups tasks by date and time slots")
    void testGetTasksForWeek() {
        // Given: a specific week (week containing Jan 10, 2024 - which is a Wednesday)
        LocalDate dateInWeek = LocalDate.of(2024, 1, 10); // Wednesday
        LocalDate monday = LocalDate.of(2024, 1, 8);
        LocalDate wednesday = LocalDate.of(2024, 1, 10);
        LocalDate friday = LocalDate.of(2024, 1, 12);
        
        // And: all-day tasks
        taskRepository.save(Task.builder()
            .title("All-day task on Monday")
            .status(TaskStatus.TODO)
            .dueDate(monday)
            .dueTime(null) // all-day
            .build());
        
        // And: timed tasks
        taskRepository.save(Task.builder()
            .title("Morning task on Wednesday")
            .status(TaskStatus.TODO)
            .dueDate(wednesday)
            .dueTime(LocalTime.of(9, 0))
            .build());
        
        taskRepository.save(Task.builder()
            .title("Afternoon task on Wednesday")
            .status(TaskStatus.TODO)
            .dueDate(wednesday)
            .dueTime(LocalTime.of(14, 30))
            .build());
        
        taskRepository.save(Task.builder()
            .title("Evening task on Friday")
            .status(TaskStatus.TODO)
            .dueDate(friday)
            .dueTime(LocalTime.of(18, 0))
            .build());
        
        // When: getTasksForWeek is called
        CalendarWeekView weekView = calendarFacade.getTasksForWeek(dateInWeek);
        
        // Then: tasks should be grouped by date and time slots
        assertNotNull(weekView);
        assertEquals(monday, weekView.getStartDate(), "Week should start on Monday");
        assertEquals(LocalDate.of(2024, 1, 14), weekView.getEndDate(), "Week should end on Sunday");
        
        // And: should have 7 days
        assertEquals(7, weekView.getDays().size(), "Week should have 7 days");
        
        // And: all-day tasks should be separated from timed tasks
        CalendarWeekView.DayWithTaskSlots mondaySlots = weekView.getDays().get(0);
        assertEquals(monday, mondaySlots.getDate());
        assertEquals(1, mondaySlots.getAllDayTasks().size(), "Monday should have 1 all-day task");
        assertEquals(0, mondaySlots.getTimedTasks().size(), "Monday should have 0 timed tasks");
        
        // And: Wednesday should have timed tasks sorted by time
        CalendarWeekView.DayWithTaskSlots wednesdaySlots = weekView.getDays().get(2);
        assertEquals(wednesday, wednesdaySlots.getDate());
        assertEquals(0, wednesdaySlots.getAllDayTasks().size(), "Wednesday should have 0 all-day tasks");
        assertEquals(2, wednesdaySlots.getTimedTasks().size(), "Wednesday should have 2 timed tasks");
        
        // And: timed tasks should be sorted by time
        List<Task> wednesdayTimedTasks = wednesdaySlots.getTimedTasks();
        assertEquals(LocalTime.of(9, 0), wednesdayTimedTasks.get(0).getDueTime(), 
            "First task should be at 9:00");
        assertEquals(LocalTime.of(14, 30), wednesdayTimedTasks.get(1).getDueTime(),
            "Second task should be at 14:30");
        
        // And: Friday should have 1 timed task
        CalendarWeekView.DayWithTaskSlots fridaySlots = weekView.getDays().get(4);
        assertEquals(friday, fridaySlots.getDate());
        assertEquals(0, fridaySlots.getAllDayTasks().size());
        assertEquals(1, fridaySlots.getTimedTasks().size());
    }
    
    @Test
    @DisplayName("Get tasks for week - handles week with no tasks")
    void testGetTasksForWeek_NoTasks() {
        // Given: a week with no tasks
        LocalDate dateInWeek = LocalDate.of(2024, 1, 10);
        
        // When: getTasksForWeek is called
        CalendarWeekView weekView = calendarFacade.getTasksForWeek(dateInWeek);
        
        // Then: all days should have empty task lists
        assertNotNull(weekView);
        assertEquals(7, weekView.getDays().size());
        
        for (CalendarWeekView.DayWithTaskSlots day : weekView.getDays()) {
            assertEquals(0, day.getAllDayTasks().size(), "Day should have no all-day tasks");
            assertEquals(0, day.getTimedTasks().size(), "Day should have no timed tasks");
        }
    }
    
    @Test
    @DisplayName("Get tasks for week - handles Monday as input date")
    void testGetTasksForWeek_MondayInput() {
        // Given: Monday as the input date
        LocalDate monday = LocalDate.of(2024, 1, 8);
        
        // When: getTasksForWeek is called
        CalendarWeekView weekView = calendarFacade.getTasksForWeek(monday);
        
        // Then: week should start on the same Monday
        assertEquals(monday, weekView.getStartDate());
        assertEquals(monday.plusDays(6), weekView.getEndDate());
    }
    
    @Test
    @DisplayName("Get tasks for week - handles Sunday as input date")
    void testGetTasksForWeek_SundayInput() {
        // Given: Sunday as the input date
        LocalDate sunday = LocalDate.of(2024, 1, 14);
        
        // When: getTasksForWeek is called
        CalendarWeekView weekView = calendarFacade.getTasksForWeek(sunday);
        
        // Then: week should start on the previous Monday
        assertEquals(LocalDate.of(2024, 1, 8), weekView.getStartDate());
        assertEquals(sunday, weekView.getEndDate());
    }
    
    @Test
    @DisplayName("Get tasks for week - tasks without due date are excluded")
    void testGetTasksForWeek_TasksWithoutDueDate() {
        // Given: tasks without due date
        taskRepository.save(Task.builder()
            .title("Task without due date")
            .status(TaskStatus.TODO)
            .dueDate(null)
            .build());
        
        // When: getTasksForWeek is called
        CalendarWeekView weekView = calendarFacade.getTasksForWeek(LocalDate.of(2024, 1, 10));
        
        // Then: tasks without due date should not appear
        int totalTasks = weekView.getDays().stream()
            .mapToInt(day -> day.getAllDayTasks().size() + day.getTimedTasks().size())
            .sum();
        assertEquals(0, totalTasks, "Tasks without due date should not appear");
    }
}
