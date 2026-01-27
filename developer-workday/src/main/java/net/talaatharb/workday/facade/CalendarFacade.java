package net.talaatharb.workday.facade;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.dtos.CalendarMonthView;
import net.talaatharb.workday.dtos.CalendarWeekView;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.service.TaskService;

/**
 * Facade for calendar-view specific operations.
 * Provides data formatted for calendar grid displays.
 */
@Slf4j
@RequiredArgsConstructor
public class CalendarFacade {
    
    private final TaskService taskService;
    
    /**
     * Get tasks for a calendar month view, grouped by date.
     * Optimized for calendar grid display.
     * 
     * @param year the year
     * @param month the month (1-12)
     * @return calendar month view with tasks grouped by date
     */
    public CalendarMonthView getTasksForMonth(int year, int month) {
        log.debug("Getting tasks for month: {}-{}", year, month);
        
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        // Get all tasks in the month
        List<Task> tasks = taskService.findByDueDateBetween(startDate, endDate);
        
        // Group tasks by date
        Map<LocalDate, List<Task>> tasksByDate = new HashMap<>();
        for (Task task : tasks) {
            LocalDate taskDate = task.getDueDate();
            if (taskDate != null) {
                tasksByDate.computeIfAbsent(taskDate, k -> new ArrayList<>()).add(task);
            }
        }
        
        // Build calendar view with all days in the month
        List<CalendarMonthView.DayWithTasks> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            List<Task> dayTasks = tasksByDate.getOrDefault(date, new ArrayList<>());
            
            CalendarMonthView.DayWithTasks dayWithTasks = CalendarMonthView.DayWithTasks.builder()
                .date(date)
                .tasks(dayTasks)
                .build();
            
            days.add(dayWithTasks);
        }
        
        CalendarMonthView monthView = CalendarMonthView.builder()
            .year(year)
            .month(month)
            .days(days)
            .build();
        
        log.debug("Built calendar month view with {} days and {} total tasks", 
            days.size(), tasks.size());
        
        return monthView;
    }
    
    /**
     * Get tasks for a calendar week view, grouped by date and time slots.
     * Separates all-day tasks from timed tasks.
     * 
     * @param date any date within the desired week
     * @return calendar week view with tasks grouped by date and time
     */
    public CalendarWeekView getTasksForWeek(LocalDate date) {
        log.debug("Getting tasks for week containing: {}", date);
        
        // Get week boundaries (Monday to Sunday)
        LocalDate startDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        // Get all tasks in the week
        List<Task> tasks = taskService.findByDueDateBetween(startDate, endDate);
        
        // Group tasks by date
        Map<LocalDate, List<Task>> tasksByDate = new HashMap<>();
        for (Task task : tasks) {
            LocalDate taskDate = task.getDueDate();
            if (taskDate != null) {
                tasksByDate.computeIfAbsent(taskDate, k -> new ArrayList<>()).add(task);
            }
        }
        
        // Build calendar week view with all 7 days
        List<CalendarWeekView.DayWithTaskSlots> days = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            List<Task> dayTasks = tasksByDate.getOrDefault(currentDate, new ArrayList<>());
            
            // Separate all-day tasks from timed tasks
            List<Task> allDayTasks = dayTasks.stream()
                .filter(task -> task.getDueTime() == null)
                .collect(Collectors.toList());
            
            List<Task> timedTasks = dayTasks.stream()
                .filter(task -> task.getDueTime() != null)
                .sorted((t1, t2) -> t1.getDueTime().compareTo(t2.getDueTime()))
                .collect(Collectors.toList());
            
            CalendarWeekView.DayWithTaskSlots dayWithSlots = CalendarWeekView.DayWithTaskSlots.builder()
                .date(currentDate)
                .allDayTasks(allDayTasks)
                .timedTasks(timedTasks)
                .build();
            
            days.add(dayWithSlots);
            currentDate = currentDate.plusDays(1);
        }
        
        CalendarWeekView weekView = CalendarWeekView.builder()
            .startDate(startDate)
            .endDate(endDate)
            .days(days)
            .build();
        
        log.debug("Built calendar week view from {} to {} with {} total tasks",
            startDate, endDate, tasks.size());
        
        return weekView;
    }
    
    /**
     * Get tasks for a specified date period.
     *
     * @param startDate the start of the period
     * @param endDate the end of the period
     * @return list of tasks within the period
     */
    public List<Task> getTasksForPeriod(LocalDate startDate, LocalDate endDate) {
        log.debug("Getting tasks for period: {} to {}", startDate, endDate);
        return taskService.findByDueDateBetween(startDate, endDate);
    }
    
    /**
     * Get tasks for a specific day.
     *
     * @param date the date to get tasks for
     * @return list of tasks for that day
     */
    public List<Task> getTasksForDay(LocalDate date) {
        log.debug("Getting tasks for day: {}", date);
        return taskService.findByDueDateBetween(date, date);
    }
}
