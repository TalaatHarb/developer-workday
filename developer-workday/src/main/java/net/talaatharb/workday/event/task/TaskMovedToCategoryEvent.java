package net.talaatharb.workday.event.task;

import java.util.UUID;

import lombok.Getter;
import net.talaatharb.workday.event.Event;

/**
 * Event published when a task is moved to a different category.
 */
@Getter
public class TaskMovedToCategoryEvent extends Event {
    private final UUID taskId;
    private final UUID oldCategoryId;
    private final UUID newCategoryId;
    
    public TaskMovedToCategoryEvent(UUID taskId, UUID oldCategoryId, UUID newCategoryId) {
        super();
        this.taskId = taskId;
        this.oldCategoryId = oldCategoryId;
        this.newCategoryId = newCategoryId;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Task moved: ID %s from category %s to %s", 
            taskId, oldCategoryId, newCategoryId);
    }
}
