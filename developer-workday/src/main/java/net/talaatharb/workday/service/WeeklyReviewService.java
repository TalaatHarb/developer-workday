package net.talaatharb.workday.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.dtos.WeeklyStatistics;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.review.WeeklyReviewCompletedEvent;
import net.talaatharb.workday.event.review.WeeklyReviewStartedEvent;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for weekly review functionality.
 */
@Slf4j
@RequiredArgsConstructor
public class WeeklyReviewService {
    
    private final TaskRepository taskRepository;
    private final EventDispatcher eventDispatcher;
    
    /**
     * Start a weekly review for the current week.
     */
    public WeeklyStatistics startWeeklyReview() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        return startWeeklyReview(weekStart, weekEnd);
    }
    
    /**
     * Start a weekly review for a specific week range.
     */
    public WeeklyStatistics startWeeklyReview(LocalDate weekStart, LocalDate weekEnd) {
        log.info("Starting weekly review for week: {} to {}", weekStart, weekEnd);
        
        // Publish event
        eventDispatcher.publish(new WeeklyReviewStartedEvent(weekStart, weekEnd));
        
        // Calculate statistics
        return calculateWeeklyStatistics(weekStart, weekEnd);
    }
    
    /**
     * Calculate weekly statistics for the given week.
     */
    public WeeklyStatistics calculateWeeklyStatistics(LocalDate weekStart, LocalDate weekEnd) {
        log.debug("Calculating statistics for week: {} to {}", weekStart, weekEnd);
        
        List<Task> allTasks = taskRepository.findAll();
        
        // Filter completed tasks from the week
        List<Task> completedTasks = allTasks.stream()
            .filter(task -> task.getCompletedAt() != null)
            .filter(task -> {
                LocalDate completedDate = task.getCompletedAt().toLocalDate();
                return !completedDate.isBefore(weekStart) && !completedDate.isAfter(weekEnd);
            })
            .collect(Collectors.toList());
        
        // Filter planned tasks (scheduled or due within the week)
        List<Task> plannedTasks = allTasks.stream()
            .filter(task -> {
                boolean scheduledInWeek = task.getScheduledDate() != null &&
                    !task.getScheduledDate().isBefore(weekStart) &&
                    !task.getScheduledDate().isAfter(weekEnd);
                    
                boolean dueInWeek = task.getDueDate() != null &&
                    !task.getDueDate().isBefore(weekStart) &&
                    !task.getDueDate().isAfter(weekEnd);
                    
                return scheduledInWeek || dueInWeek;
            })
            .collect(Collectors.toList());
        
        int totalCompleted = completedTasks.size();
        int totalPlanned = plannedTasks.size();
        double completionRate = totalPlanned > 0 ? (double) totalCompleted / totalPlanned * 100 : 0.0;
        
        // Count by priority
        long highPriorityCompleted = completedTasks.stream()
            .filter(task -> task.getPriority() == Priority.HIGH)
            .count();
        long mediumPriorityCompleted = completedTasks.stream()
            .filter(task -> task.getPriority() == Priority.MEDIUM)
            .count();
        long lowPriorityCompleted = completedTasks.stream()
            .filter(task -> task.getPriority() == Priority.LOW)
            .count();
        
        // Count overdue tasks
        int overdueTasks = (int) taskRepository.findOverdueTasks().stream()
            .filter(task -> task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED)
            .count();
        
        return WeeklyStatistics.builder()
            .weekStartDate(weekStart)
            .weekEndDate(weekEnd)
            .totalTasksCompleted(totalCompleted)
            .totalTasksPlanned(totalPlanned)
            .completionRate(completionRate)
            .highPriorityCompleted((int) highPriorityCompleted)
            .mediumPriorityCompleted((int) mediumPriorityCompleted)
            .lowPriorityCompleted((int) lowPriorityCompleted)
            .overdueTasks(overdueTasks)
            .build();
    }
    
    /**
     * Get completed tasks for the given week.
     */
    public List<Task> getCompletedTasksForWeek(LocalDate weekStart, LocalDate weekEnd) {
        return taskRepository.findAll().stream()
            .filter(task -> task.getCompletedAt() != null)
            .filter(task -> {
                LocalDate completedDate = task.getCompletedAt().toLocalDate();
                return !completedDate.isBefore(weekStart) && !completedDate.isAfter(weekEnd);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get upcoming tasks (not completed, scheduled or due in the future).
     */
    public List<Task> getUpcomingTasks() {
        LocalDate today = LocalDate.now();
        return taskRepository.findAll().stream()
            .filter(task -> task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED)
            .filter(task -> {
                boolean hasScheduledDate = task.getScheduledDate() != null && 
                    !task.getScheduledDate().isBefore(today);
                boolean hasDueDate = task.getDueDate() != null && 
                    !task.getDueDate().isBefore(today);
                return hasScheduledDate || hasDueDate;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Complete the weekly review process.
     */
    public void completeWeeklyReview(LocalDate weekStart, LocalDate weekEnd, int reviewedTasksCount) {
        log.info("Completing weekly review for week: {} to {}", weekStart, weekEnd);
        eventDispatcher.publish(new WeeklyReviewCompletedEvent(weekStart, weekEnd, reviewedTasksCount));
    }
}
