package net.talaatharb.workday.event.task;

import lombok.Getter;
import net.talaatharb.workday.event.Event;
import net.talaatharb.workday.model.Task;

/**
 * Event published when a task is deleted.
 */
@Getter
public class TaskDeletedEvent extends Event {
    private final Task task;
    
    public TaskDeletedEvent(Task task) {
        super();
        this.task = task;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Task deleted: %s (ID: %s)", 
            task.getTitle(), task.getId());
    }
}
