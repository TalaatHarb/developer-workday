package net.talaatharb.workday.event.focusmode;

import lombok.Getter;
import net.talaatharb.workday.event.Event;

import java.time.LocalDateTime;

/**
 * Event fired when it's time for a break reminder during focus mode.
 */
@Getter
public class BreakReminderEvent extends Event {
    private final LocalDateTime reminderTime;
    private final int sessionDurationMinutes;

    public BreakReminderEvent(int sessionDurationMinutes) {
        this.reminderTime = LocalDateTime.now();
        this.sessionDurationMinutes = sessionDurationMinutes;
    }

    @Override
    public String getEventDetails() {
        return String.format("Break reminder after %d minutes of focus", sessionDurationMinutes);
    }
}
