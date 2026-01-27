package net.talaatharb.workday.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.time.TimeTrackedEvent;
import net.talaatharb.workday.model.Task;

/**
 * Service for tracking time spent on tasks.
 * Provides start/stop timer functionality and time reporting.
 */
@Slf4j
@RequiredArgsConstructor
public class TimeTrackingService {
    
    private final EventDispatcher eventDispatcher;
    private final TaskService taskService;
    
    // Map of task ID to start time for active timers
    private final Map<UUID, LocalDateTime> activeTimers = new ConcurrentHashMap<>();
    
    /**
     * Start timer for a task
     */
    public void startTimer(UUID taskId) {
        if (activeTimers.containsKey(taskId)) {
            log.warn("Timer already running for task: {}", taskId);
            throw new IllegalStateException("Timer already running for this task");
        }
        
        LocalDateTime startTime = LocalDateTime.now();
        activeTimers.put(taskId, startTime);
        log.info("Started timer for task: {} at {}", taskId, startTime);
    }
    
    /**
     * Stop timer for a task and record the elapsed time
     */
    public Duration stopTimer(UUID taskId) {
        LocalDateTime startTime = activeTimers.remove(taskId);
        if (startTime == null) {
            log.warn("No active timer for task: {}", taskId);
            throw new IllegalStateException("No active timer for this task");
        }
        
        LocalDateTime endTime = LocalDateTime.now();
        Duration elapsed = Duration.between(startTime, endTime);
        
        // Update task's actual duration
        Optional<Task> taskOpt = taskService.findById(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            Duration currentDuration = task.getActualDuration() != null ? task.getActualDuration() : Duration.ZERO;
            task.setActualDuration(currentDuration.plus(elapsed));
            taskService.updateTask(task);
        }
        
        // Publish event
        eventDispatcher.publish(new TimeTrackedEvent(taskId, elapsed, startTime, endTime));
        
        log.info("Stopped timer for task: {} - Elapsed: {} minutes", taskId, elapsed.toMinutes());
        return elapsed;
    }
    
    /**
     * Get elapsed time for an active timer
     */
    public Optional<Duration> getElapsedTime(UUID taskId) {
        LocalDateTime startTime = activeTimers.get(taskId);
        if (startTime == null) {
            return Optional.empty();
        }
        
        Duration elapsed = Duration.between(startTime, LocalDateTime.now());
        return Optional.of(elapsed);
    }
    
    /**
     * Check if a timer is active for a task
     */
    public boolean isTimerActive(UUID taskId) {
        return activeTimers.containsKey(taskId);
    }
    
    /**
     * Stop all active timers (useful for application shutdown)
     */
    public void stopAllTimers() {
        log.info("Stopping all active timers ({} timers)", activeTimers.size());
        
        for (UUID taskId : activeTimers.keySet()) {
            try {
                stopTimer(taskId);
            } catch (Exception e) {
                log.error("Error stopping timer for task: {}", taskId, e);
            }
        }
    }
    
    /**
     * Manually add time to a task (for manual time entry)
     */
    public void addManualTime(UUID taskId, Duration duration, LocalDateTime timestamp) {
        log.info("Adding manual time entry for task: {} - Duration: {} minutes", taskId, duration.toMinutes());
        
        Optional<Task> taskOpt = taskService.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        
        Task task = taskOpt.get();
        Duration currentDuration = task.getActualDuration() != null ? task.getActualDuration() : Duration.ZERO;
        task.setActualDuration(currentDuration.plus(duration));
        taskService.updateTask(task);
        
        // Publish event
        LocalDateTime startTime = timestamp.minus(duration);
        eventDispatcher.publish(new TimeTrackedEvent(taskId, duration, startTime, timestamp));
        
        log.info("Manual time entry added for task: {}", taskId);
    }
    
    /**
     * Get total time tracked for a task
     */
    public Duration getTotalTimeForTask(UUID taskId) {
        Optional<Task> taskOpt = taskService.findById(taskId);
        if (taskOpt.isEmpty()) {
            return Duration.ZERO;
        }
        
        Duration actualDuration = taskOpt.get().getActualDuration();
        return actualDuration != null ? actualDuration : Duration.ZERO;
    }
    
    /**
     * Get total time tracked for all tasks in a category
     */
    public Duration getTotalTimeForCategory(UUID categoryId) {
        return taskService.findByCategoryId(categoryId).stream()
            .map(task -> task.getActualDuration() != null ? task.getActualDuration() : Duration.ZERO)
            .reduce(Duration.ZERO, Duration::plus);
    }
    
    /**
     * Get count of active timers
     */
    public int getActiveTimerCount() {
        return activeTimers.size();
    }
}
