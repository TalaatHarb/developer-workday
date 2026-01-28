package net.talaatharb.workday.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SnoozeOption enum
 */
class SnoozeOptionTest {
    
    @Test
    @DisplayName("LATER_TODAY should snooze 3 hours from now")
    void laterTodayShouldSnoozeThreeHours() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // When
        LocalDateTime snoozeTime = SnoozeOption.LATER_TODAY.calculateSnoozeTime(now);
        
        // Then
        assertEquals(now.plusHours(3), snoozeTime);
    }
    
    @Test
    @DisplayName("TOMORROW should snooze to tomorrow at 9 AM")
    void tomorrowShouldSnoozeToTomorrowAt9AM() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expected = now.plusDays(1).with(LocalTime.of(9, 0));
        
        // When
        LocalDateTime snoozeTime = SnoozeOption.TOMORROW.calculateSnoozeTime(now);
        
        // Then
        assertEquals(expected, snoozeTime);
    }
    
    @Test
    @DisplayName("NEXT_WEEK should snooze to next week at 9 AM")
    void nextWeekShouldSnoozeToNextWeekAt9AM() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expected = now.plusWeeks(1).with(LocalTime.of(9, 0));
        
        // When
        LocalDateTime snoozeTime = SnoozeOption.NEXT_WEEK.calculateSnoozeTime(now);
        
        // Then
        assertEquals(expected, snoozeTime);
    }
    
    @Test
    @DisplayName("CUSTOM should throw exception when calculating time")
    void customShouldThrowException() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // Then
        assertThrows(UnsupportedOperationException.class, 
            () -> SnoozeOption.CUSTOM.calculateSnoozeTime(now),
            "CUSTOM snooze should require explicit date/time");
    }
    
    @Test
    @DisplayName("isSnoozed should return true for future times")
    void isSnoozedShouldReturnTrueForFutureTimes() {
        // Given
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        
        // When
        boolean snoozed = SnoozeOption.isSnoozed(futureTime);
        
        // Then
        assertTrue(snoozed, "Task with future snoozeUntil should be snoozed");
    }
    
    @Test
    @DisplayName("isSnoozed should return false for past times")
    void isSnoozedShouldReturnFalseForPastTimes() {
        // Given
        LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
        
        // When
        boolean snoozed = SnoozeOption.isSnoozed(pastTime);
        
        // Then
        assertFalse(snoozed, "Task with past snoozeUntil should not be snoozed");
    }
    
    @Test
    @DisplayName("isSnoozed should return false for null snoozeUntil")
    void isSnoozedShouldReturnFalseForNull() {
        // When
        boolean snoozed = SnoozeOption.isSnoozed(null);
        
        // Then
        assertFalse(snoozed, "Task with null snoozeUntil should not be snoozed");
    }
    
    @Test
    @DisplayName("Display names should be properly set")
    void displayNamesShouldBeProperlySet() {
        assertEquals("Later Today", SnoozeOption.LATER_TODAY.getDisplayName());
        assertEquals("Tomorrow", SnoozeOption.TOMORROW.getDisplayName());
        assertEquals("Next Week", SnoozeOption.NEXT_WEEK.getDisplayName());
        assertEquals("Custom...", SnoozeOption.CUSTOM.getDisplayName());
    }
    
    @Test
    @DisplayName("calculateSnoozeTime without parameter should use current time")
    void calculateSnoozeTimeWithoutParameterShouldUseCurrentTime() {
        // When
        LocalDateTime laterToday = SnoozeOption.LATER_TODAY.calculateSnoozeTime();
        LocalDateTime tomorrow = SnoozeOption.TOMORROW.calculateSnoozeTime();
        LocalDateTime nextWeek = SnoozeOption.NEXT_WEEK.calculateSnoozeTime();
        
        // Then
        assertTrue(laterToday.isAfter(LocalDateTime.now()), "Later today should be in the future");
        assertTrue(tomorrow.isAfter(LocalDateTime.now()), "Tomorrow should be in the future");
        assertTrue(nextWeek.isAfter(LocalDateTime.now()), "Next week should be in the future");
    }
}
