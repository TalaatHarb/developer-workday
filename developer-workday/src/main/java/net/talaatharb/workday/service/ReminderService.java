package net.talaatharb.workday.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.reminder.ReminderTriggeredEvent;
import net.talaatharb.workday.model.Task;

/**
 * Service for scheduling and triggering task reminders.
 */
@Slf4j
@RequiredArgsConstructor
public class ReminderService {
    
    private final EventDispatcher eventDispatcher;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<UUID, ScheduledFuture<?>> scheduledReminders = new ConcurrentHashMap<>();
    
    /**
     * Schedule a reminder for a task
     */
    public void scheduleReminder(Task task) {
        if (task.getDueDate() == null || task.getReminderMinutesBefore() == null) {
            log.debug("Task {} has no due date or reminder configuration", task.getId());
            return;
        }
        
        // Calculate reminder time
        LocalDateTime dueDateTime = task.getDueDate().atTime(
            task.getDueTime() != null ? task.getDueTime() : java.time.LocalTime.of(23, 59));
        LocalDateTime reminderTime = dueDateTime.minusMinutes(task.getReminderMinutesBefore());
        
        // Check if reminder time is in the future
        LocalDateTime now = LocalDateTime.now();
        if (reminderTime.isBefore(now)) {
            log.debug("Reminder time {} is in the past for task {}", reminderTime, task.getId());
            return;
        }
        
        // Calculate delay
        long delaySeconds = java.time.Duration.between(now, reminderTime).getSeconds();
        
        // Cancel existing reminder if any
        cancelReminder(task.getId());
        
        // Schedule new reminder
        ScheduledFuture<?> future = scheduler.schedule(
            () -> triggerReminder(task.getId(), task.getTitle(), reminderTime),
            delaySeconds,
            TimeUnit.SECONDS
        );
        
        scheduledReminders.put(task.getId(), future);
        log.info("Scheduled reminder for task {} at {}", task.getId(), reminderTime);
    }
    
    /**
     * Trigger a reminder
     */
    private void triggerReminder(UUID taskId, String taskTitle, LocalDateTime reminderTime) {
        log.info("Triggering reminder for task: {} at {}", taskId, reminderTime);
        
        // Publish event
        eventDispatcher.publish(new ReminderTriggeredEvent(taskId, taskTitle, reminderTime));
        
        // Remove from scheduled reminders
        scheduledReminders.remove(taskId);
        
        // In a real application, this would also show a system notification
        // For now, we just log and publish the event
    }
    
    /**
     * Cancel a scheduled reminder
     */
    public void cancelReminder(UUID taskId) {
        ScheduledFuture<?> future = scheduledReminders.remove(taskId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.info("Cancelled reminder for task {}", taskId);
        }
    }
    
    /**
     * Update reminder when task is modified
     */
    public void updateReminder(Task task) {
        // Cancel existing reminder
        cancelReminder(task.getId());
        
        // Schedule new reminder if task is not completed
        if (task.getCompletedAt() == null && 
            task.getStatus() != net.talaatharb.workday.model.TaskStatus.COMPLETED) {
            scheduleReminder(task);
        }
    }
    
    /**
     * Check if a task has a scheduled reminder
     */
    public boolean hasScheduledReminder(UUID taskId) {
        ScheduledFuture<?> future = scheduledReminders.get(taskId);
        return future != null && !future.isDone();
    }
    
    /**
     * Get count of scheduled reminders
     */
    public int getScheduledReminderCount() {
        return (int) scheduledReminders.values().stream()
            .filter(f -> !f.isDone())
            .count();
    }
    
    /**
     * Shutdown the scheduler
     */
    public void shutdown() {
        log.info("Shutting down ReminderService");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
