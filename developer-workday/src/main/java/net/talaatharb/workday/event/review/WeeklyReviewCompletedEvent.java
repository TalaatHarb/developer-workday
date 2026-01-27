package net.talaatharb.workday.event.review;

import lombok.Getter;
import net.talaatharb.workday.event.Event;

import java.time.LocalDate;

/**
 * Event published when a weekly review is completed.
 */
@Getter
public class WeeklyReviewCompletedEvent extends Event {
    private final LocalDate weekStartDate;
    private final LocalDate weekEndDate;
    private final int reviewedTasksCount;
    
    public WeeklyReviewCompletedEvent(LocalDate weekStartDate, LocalDate weekEndDate, int reviewedTasksCount) {
        super();
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
        this.reviewedTasksCount = reviewedTasksCount;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Weekly review completed for week: %s to %s (reviewed %d tasks)", 
            weekStartDate, weekEndDate, reviewedTasksCount);
    }
}
