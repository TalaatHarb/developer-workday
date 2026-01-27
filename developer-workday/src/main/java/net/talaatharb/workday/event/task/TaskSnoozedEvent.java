package net.talaatharb.workday.event.task;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import net.talaatharb.workday.event.Event;

/**
 * Event published when a task is snoozed until a future date/time.
 */
@Getter
public class TaskSnoozedEvent extends Event {
    private final UUID taskId;
    private final LocalDateTime snoozeUntil;
    
    @Builder
    public TaskSnoozedEvent(UUID taskId, LocalDateTime snoozeUntil) {
        super();
        this.taskId = taskId;
        this.snoozeUntil = snoozeUntil;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Task snoozed: ID %s until %s", 
            taskId, snoozeUntil);
    }
}
