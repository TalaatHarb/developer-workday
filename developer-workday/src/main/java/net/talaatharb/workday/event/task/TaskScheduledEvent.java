package net.talaatharb.workday.event.task;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Getter;
import net.talaatharb.workday.event.Event;

/**
 * Event published when a task is scheduled for a specific date.
 */
@Getter
public class TaskScheduledEvent extends Event {
    private final UUID taskId;
    private final LocalDate scheduledDate;
    
    public TaskScheduledEvent(UUID taskId, LocalDate scheduledDate) {
        super();
        this.taskId = taskId;
        this.scheduledDate = scheduledDate;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Task scheduled: ID %s for %s", 
            taskId, scheduledDate);
    }
}
