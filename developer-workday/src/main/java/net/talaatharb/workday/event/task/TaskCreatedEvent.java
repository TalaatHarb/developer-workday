package net.talaatharb.workday.event.task;

import lombok.Getter;
import net.talaatharb.workday.event.Event;
import net.talaatharb.workday.model.Task;

/**
 * Event published when a new task is created.
 */
@Getter
public class TaskCreatedEvent extends Event {
    private final Task task;
    
    public TaskCreatedEvent(Task task) {
        super();
        this.task = task;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Task created: %s (ID: %s)", 
            task.getTitle(), task.getId());
    }
}
