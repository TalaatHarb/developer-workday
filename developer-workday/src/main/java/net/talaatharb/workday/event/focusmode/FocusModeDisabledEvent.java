package net.talaatharb.workday.event.focusmode;

import lombok.Getter;
import net.talaatharb.workday.event.Event;

import java.time.LocalDateTime;

/**
 * Event fired when focus mode is disabled.
 */
@Getter
public class FocusModeDisabledEvent extends Event {
    private final LocalDateTime endTime;
    private final boolean expiredByTimer;

    public FocusModeDisabledEvent(boolean expiredByTimer) {
        this.endTime = LocalDateTime.now();
        this.expiredByTimer = expiredByTimer;
    }

    @Override
    public String getEventDetails() {
        return String.format("Focus mode disabled. Expired by timer: %s", expiredByTimer);
    }
}
