package net.talaatharb.workday.event.focusmode;

import lombok.Getter;
import net.talaatharb.workday.event.Event;

import java.time.LocalDateTime;

/**
 * Event fired when focus mode is enabled.
 */
@Getter
public class FocusModeEnabledEvent extends Event {
    private final Integer timerDurationMinutes;
    private final LocalDateTime startTime;

    public FocusModeEnabledEvent(Integer timerDurationMinutes) {
        this.timerDurationMinutes = timerDurationMinutes;
        this.startTime = LocalDateTime.now();
    }

    @Override
    public String getEventDetails() {
        return String.format("Focus mode enabled with timer: %s minutes", 
                timerDurationMinutes != null ? timerDurationMinutes : "none");
    }
}
