package net.talaatharb.workday.event.review;

import lombok.Getter;
import net.talaatharb.workday.event.Event;

import java.time.LocalDate;

/**
 * Event published when a weekly review is started.
 */
@Getter
public class WeeklyReviewStartedEvent extends Event {
    private final LocalDate weekStartDate;
    private final LocalDate weekEndDate;
    
    public WeeklyReviewStartedEvent(LocalDate weekStartDate, LocalDate weekEndDate) {
        super();
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Weekly review started for week: %s to %s", 
            weekStartDate, weekEndDate);
    }
}
