package net.talaatharb.workday.event.reminder;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import net.talaatharb.workday.event.Event;

/**
 * Event published when a reminder is triggered.
 */
@Getter
public class ReminderTriggeredEvent extends Event {
    private final UUID taskId;
    private final String taskTitle;
    private final LocalDateTime reminderTime;
    
    public ReminderTriggeredEvent(UUID taskId, String taskTitle, LocalDateTime reminderTime) {
        super();
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.reminderTime = reminderTime;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Reminder triggered for task: %s (ID: %s) at %s", 
            taskTitle, taskId, reminderTime);
    }
}
