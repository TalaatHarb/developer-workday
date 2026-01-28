package net.talaatharb.workday.event.task;

import lombok.Getter;
import net.talaatharb.workday.event.Event;
import net.talaatharb.workday.model.Task;

/**
 * Event published when a task is updated.
 * Contains both the old and new states of the task.
 */
@Getter
public class TaskUpdatedEvent extends Event {
    private final Task oldTask;
    private final Task newTask;
    
    public TaskUpdatedEvent(Task oldTask, Task newTask) {
        super();
        this.oldTask = oldTask;
        this.newTask = newTask;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Task updated: %s (ID: %s)", 
            newTask.getTitle(), newTask.getId());
    }
}
