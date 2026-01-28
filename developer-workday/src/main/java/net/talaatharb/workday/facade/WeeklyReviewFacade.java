package net.talaatharb.workday.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.dtos.TaskDTO;
import net.talaatharb.workday.dtos.WeeklyStatistics;
import net.talaatharb.workday.mapper.TaskMapper;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.service.TaskService;
import net.talaatharb.workday.service.WeeklyReviewService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Facade for weekly review operations.
 * Coordinates between WeeklyReviewService and TaskService.
 */
@Slf4j
@RequiredArgsConstructor
public class WeeklyReviewFacade {
    
    private final WeeklyReviewService weeklyReviewService;
    private final TaskService taskService;
    private final TaskMapper taskMapper;
    
    /**
     * Start a weekly review for the current week.
     */
    public WeeklyStatistics startWeeklyReview() {
        log.info("Starting weekly review via facade");
        return weeklyReviewService.startWeeklyReview();
    }
    
    /**
     * Start a weekly review for a specific week.
     */
    public WeeklyStatistics startWeeklyReview(LocalDate weekStart, LocalDate weekEnd) {
        log.info("Starting weekly review for custom week via facade: {} to {}", weekStart, weekEnd);
        return weeklyReviewService.startWeeklyReview(weekStart, weekEnd);
    }
    
    /**
     * Get completed tasks for the week as DTOs.
     */
    public List<TaskDTO> getCompletedTasksForWeek(LocalDate weekStart, LocalDate weekEnd) {
        List<Task> tasks = weeklyReviewService.getCompletedTasksForWeek(weekStart, weekEnd);
        return tasks.stream()
            .map(taskMapper::toDTO)
            .sorted(Comparator.comparing(TaskDTO::getCompletedAt).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Get upcoming tasks as DTOs.
     */
    public List<TaskDTO> getUpcomingTasks() {
        List<Task> tasks = weeklyReviewService.getUpcomingTasks();
        return tasks.stream()
            .map(taskMapper::toDTO)
            .sorted(Comparator
                .comparing((TaskDTO t) -> t.getScheduledDate() != null ? t.getScheduledDate() : 
                    t.getDueDate() != null ? t.getDueDate() : LocalDate.MAX)
                .thenComparing(t -> t.getPriority().ordinal()))
            .collect(Collectors.toList());
    }
    
    /**
     * Reschedule a task to a new date.
     */
    public void rescheduleTask(String taskId, LocalDate newDate) {
        log.debug("Rescheduling task {} to {}", taskId, newDate);
        taskService.scheduleTask(java.util.UUID.fromString(taskId), newDate);
    }
    
    /**
     * Complete the weekly review.
     */
    public void completeWeeklyReview(LocalDate weekStart, LocalDate weekEnd, int reviewedTasksCount) {
        log.info("Completing weekly review via facade");
        weeklyReviewService.completeWeeklyReview(weekStart, weekEnd, reviewedTasksCount);
    }
}
