package net.talaatharb.workday.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Predefined snooze options for tasks.
 * Provides quick snooze durations and methods to calculate the snooze time.
 */
public enum SnoozeOption {
    /**
     * Snooze until later today (3 hours from now)
     */
    LATER_TODAY("Later Today", 3),
    
    /**
     * Snooze until tomorrow at 9 AM
     */
    TOMORROW("Tomorrow", -1),
    
    /**
     * Snooze until next week (7 days from now at 9 AM)
     */
    NEXT_WEEK("Next Week", -1),
    
    /**
     * Custom snooze time (requires explicit date/time)
     */
    CUSTOM("Custom...", -1);
    
    private final String displayName;
    private final int hoursFromNow; // -1 means special calculation
    
    SnoozeOption(String displayName, int hoursFromNow) {
        this.displayName = displayName;
        this.hoursFromNow = hoursFromNow;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Calculate the snooze date/time based on this option.
     * 
     * @return The LocalDateTime when the task should reappear
     */
    public LocalDateTime calculateSnoozeTime() {
        LocalDateTime now = LocalDateTime.now();
        
        return switch (this) {
            case LATER_TODAY -> now.plusHours(hoursFromNow);
            case TOMORROW -> now.plusDays(1).with(LocalTime.of(9, 0));
            case NEXT_WEEK -> now.plusWeeks(1).with(LocalTime.of(9, 0));
            case CUSTOM -> throw new UnsupportedOperationException("CUSTOM snooze requires explicit date/time");
        };
    }
    
    /**
     * Calculate the snooze date/time from a specific reference time.
     * 
     * @param referenceTime The time to calculate from
     * @return The LocalDateTime when the task should reappear
     */
    public LocalDateTime calculateSnoozeTime(LocalDateTime referenceTime) {
        return switch (this) {
            case LATER_TODAY -> referenceTime.plusHours(hoursFromNow);
            case TOMORROW -> referenceTime.plusDays(1).with(LocalTime.of(9, 0));
            case NEXT_WEEK -> referenceTime.plusWeeks(1).with(LocalTime.of(9, 0));
            case CUSTOM -> throw new UnsupportedOperationException("CUSTOM snooze requires explicit date/time");
        };
    }
    
    /**
     * Check if a task is currently snoozed.
     * 
     * @param snoozeUntil The snooze until time, can be null
     * @return true if the task is snoozed (snoozeUntil is in the future)
     */
    public static boolean isSnoozed(LocalDateTime snoozeUntil) {
        return snoozeUntil != null && snoozeUntil.isAfter(LocalDateTime.now());
    }
}
