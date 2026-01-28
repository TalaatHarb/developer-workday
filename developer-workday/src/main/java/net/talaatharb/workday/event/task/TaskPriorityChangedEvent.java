package net.talaatharb.workday.event.task;

import java.util.UUID;

import lombok.Getter;
import net.talaatharb.workday.event.Event;
import net.talaatharb.workday.model.Priority;

/**
 * Event published when a task's priority is changed.
 */
@Getter
public class TaskPriorityChangedEvent extends Event {
    private final UUID taskId;
    private final Priority oldPriority;
    private final Priority newPriority;
    
    public TaskPriorityChangedEvent(UUID taskId, Priority oldPriority, Priority newPriority) {
        super();
        this.taskId = taskId;
        this.oldPriority = oldPriority;
        this.newPriority = newPriority;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Task priority changed: ID %s from %s to %s", 
            taskId, oldPriority, newPriority);
    }
}
