package net.talaatharb.workday.event.time;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import net.talaatharb.workday.event.Event;

/**
 * Event published when time is tracked for a task
 */
@Getter
public class TimeTrackedEvent extends Event {
    private final UUID taskId;
    private final Duration trackedDuration;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    
    public TimeTrackedEvent(UUID taskId, Duration trackedDuration, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.taskId = taskId;
        this.trackedDuration = trackedDuration;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    @Override
    public String getEventType() {
        return "TIME_TRACKED";
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Task %s: tracked %s minutes from %s to %s", 
            taskId, trackedDuration.toMinutes(), startTime, endTime);
    }
}
