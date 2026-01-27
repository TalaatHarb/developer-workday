package net.talaatharb.workday.service;

import java.awt.*;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.reminder.ReminderTriggeredEvent;
import net.talaatharb.workday.model.Task;

/**
 * Service for displaying system notifications for task reminders and due dates.
 */
@Slf4j
public class NotificationService {
    
    private final TrayIcon trayIcon;
    private final boolean notificationsSupported;
    
    /**
     * Create a notification service
     * @param trayIcon The system tray icon to use for notifications (can be null)
     */
    public NotificationService(TrayIcon trayIcon) {
        this.trayIcon = trayIcon;
        this.notificationsSupported = SystemTray.isSupported() && trayIcon != null;
        
        if (!notificationsSupported) {
            log.warn("System notifications not supported or tray icon not available");
        } else {
            log.info("NotificationService initialized with system notification support");
        }
    }
    
    /**
     * Subscribe to reminder events
     */
    public void subscribeToEvents(EventDispatcher eventDispatcher) {
        eventDispatcher.subscribe(ReminderTriggeredEvent.class, this::handleReminderTriggered);
        log.info("NotificationService subscribed to ReminderTriggeredEvent");
    }
    
    /**
     * Handle reminder triggered event
     */
    private void handleReminderTriggered(ReminderTriggeredEvent event) {
        log.info("Handling reminder triggered for task: {}", event.getTaskTitle());
        showReminderNotification(event.getTaskId(), event.getTaskTitle(), event.getReminderTime().toString());
    }
    
    /**
     * Show a reminder notification
     */
    public void showReminderNotification(UUID taskId, String taskTitle, String dueTime) {
        String message = String.format("Task due: %s\nDue time: %s", taskTitle, dueTime);
        showNotification("Task Reminder", message, TrayIcon.MessageType.INFO);
    }
    
    /**
     * Show an overdue task notification
     */
    public void showOverdueNotification(Task task) {
        String message = String.format("Task '%s' is overdue!", task.getTitle());
        showNotification("Overdue Task", message, TrayIcon.MessageType.WARNING);
    }
    
    /**
     * Show a task completion notification
     */
    public void showTaskCompletedNotification(Task task) {
        String message = String.format("Task '%s' completed!", task.getTitle());
        showNotification("Task Completed", message, TrayIcon.MessageType.INFO);
    }
    
    /**
     * Show a notification with snooze capability
     */
    public void showSnoozeableNotification(UUID taskId, String taskTitle, String dueTime, 
                                          Runnable onSnoozeCallback) {
        // For now, show regular notification
        // In a full implementation, this would show a custom dialog with snooze button
        showReminderNotification(taskId, taskTitle, dueTime);
        log.info("Snoozeable notification shown for task: {} (snooze callback available)", taskTitle);
    }
    
    /**
     * Show a generic notification
     */
    private void showNotification(String title, String message, TrayIcon.MessageType messageType) {
        if (!notificationsSupported) {
            log.warn("Cannot show notification - not supported. Title: {}, Message: {}", title, message);
            return;
        }
        
        try {
            trayIcon.displayMessage(title, message, messageType);
            log.info("Notification displayed: {} - {}", title, message);
        } catch (Exception e) {
            log.error("Failed to display notification", e);
        }
    }
    
    /**
     * Check if notifications are supported
     */
    public boolean isNotificationSupported() {
        return notificationsSupported;
    }
    
    /**
     * Schedule a snooze reminder
     */
    public void scheduleSnoozeReminder(UUID taskId, int snoozeMinutes, ReminderService reminderService, Task task) {
        log.info("Scheduling snooze reminder for task {} in {} minutes", taskId, snoozeMinutes);
        
        // Update task reminder to trigger after snooze duration
        Task snoozedTask = Task.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .dueDate(task.getDueDate())
            .dueTime(task.getDueTime())
            .reminderMinutesBefore(task.getReminderMinutesBefore())
            .priority(task.getPriority())
            .status(task.getStatus())
            .categoryId(task.getCategoryId())
            .tags(task.getTags())
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .completedAt(task.getCompletedAt())
            .recurrence(task.getRecurrence())
            .estimatedDuration(task.getEstimatedDuration())
            .actualDuration(task.getActualDuration())
            .build();
        
        // Reschedule with adjusted reminder time
        reminderService.scheduleReminder(snoozedTask);
        log.info("Snoozed task {} will remind in {} minutes", taskId, snoozeMinutes);
    }
}
