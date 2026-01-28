package net.talaatharb.workday.event.task;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import net.talaatharb.workday.event.Event;

/**
 * Event published when a task is marked as complete.
 */
@Getter
public class TaskCompletedEvent extends Event {
    private final UUID taskId;
    private final LocalDateTime completionTimestamp;
    
    public TaskCompletedEvent(UUID taskId, LocalDateTime completionTimestamp) {
        super();
        this.taskId = taskId;
        this.completionTimestamp = completionTimestamp;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Task completed: ID %s at %s", 
            taskId, completionTimestamp);
    }
}
