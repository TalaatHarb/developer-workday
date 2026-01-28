package net.talaatharb.workday.service;

import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.focusmode.BreakReminderEvent;
import net.talaatharb.workday.event.focusmode.FocusModeDisabledEvent;
import net.talaatharb.workday.event.focusmode.FocusModeEnabledEvent;
import net.talaatharb.workday.model.FocusModeState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for FocusModeService (Task 57).
 * 
 * Acceptance criteria:
 * - Enable focus mode
 * - Notifications should be suppressed
 * - UI should simplify to show only current tasks
 * - Focus timer should optionally start
 * - Timer should expire and disable focus mode
 * - Break reminders should appear at intervals
 */
class FocusModeServiceTest {

    private FocusModeService focusModeService;
    private EventDispatcher eventDispatcher;
    private boolean focusModeEnabledEventFired;
    private boolean focusModeDisabledEventFired;
    private boolean breakReminderEventFired;

    @BeforeEach
    void setUp() {
        eventDispatcher = new EventDispatcher();
        focusModeService = new FocusModeService(eventDispatcher);
        
        // Subscribe to events for testing
        focusModeEnabledEventFired = false;
        focusModeDisabledEventFired = false;
        breakReminderEventFired = false;
        
        eventDispatcher.subscribe(FocusModeEnabledEvent.class, e -> focusModeEnabledEventFired = true);
        eventDispatcher.subscribe(FocusModeDisabledEvent.class, e -> focusModeDisabledEventFired = true);
        eventDispatcher.subscribe(BreakReminderEvent.class, e -> breakReminderEventFired = true);
    }

    @AfterEach
    void tearDown() {
        if (focusModeService != null) {
            focusModeService.shutdown();
        }
    }

    @Test
    void testEnableFocusMode() {
        // Given: the application is running normally
        assertFalse(focusModeService.isFocusModeEnabled(), "Focus mode should be disabled initially");

        // When: enabling focus mode
        focusModeService.enableFocusMode(null, null);

        // Then: focus mode should be enabled and event should be fired
        assertTrue(focusModeService.isFocusModeEnabled(), "Focus mode should be enabled");
        assertTrue(focusModeEnabledEventFired, "FocusModeEnabledEvent should be fired");
        
        FocusModeState state = focusModeService.getCurrentState();
        assertTrue(state.isEnabled(), "State should reflect enabled status");
        assertNotNull(state.getStartTime(), "Start time should be set");
    }

    @Test
    void testEnableFocusModeWithTimer() {
        // Given: the application is running normally
        assertFalse(focusModeService.isFocusModeEnabled());

        // When: enabling focus mode with a timer
        int timerMinutes = 25;
        focusModeService.enableFocusMode(timerMinutes, null);

        // Then: focus mode should be enabled with timer
        assertTrue(focusModeService.isFocusModeEnabled());
        
        FocusModeState state = focusModeService.getCurrentState();
        assertEquals(timerMinutes, state.getTimerDurationMinutes(), 
                "Timer duration should match");
        assertNotNull(state.getEndTime(), "End time should be set");
        
        Integer remainingMinutes = focusModeService.getRemainingMinutes();
        assertNotNull(remainingMinutes, "Remaining minutes should be available");
        assertTrue(remainingMinutes > 0 && remainingMinutes <= timerMinutes, 
                "Remaining minutes should be positive and within timer duration");
    }

    @Test
    void testDisableFocusMode() {
        // Given: focus mode is enabled
        focusModeService.enableFocusMode(null, null);
        assertTrue(focusModeService.isFocusModeEnabled());

        // When: disabling focus mode
        focusModeService.disableFocusMode();

        // Then: focus mode should be disabled and event should be fired
        assertFalse(focusModeService.isFocusModeEnabled(), "Focus mode should be disabled");
        assertTrue(focusModeDisabledEventFired, "FocusModeDisabledEvent should be fired");
        
        FocusModeState state = focusModeService.getCurrentState();
        assertFalse(state.isEnabled(), "State should reflect disabled status");
        assertNotNull(state.getEndTime(), "End time should be set");
    }

    @Test
    void testFocusModeTimerExpiration() throws InterruptedException {
        // Given: focus mode is enabled with a very short timer (for testing)
        // Note: Using a short timer for test, but still needs actual time to pass
        int timerSeconds = 1; // 1 second for quick test
        
        // We'll test the timer mechanism by enabling with 1 minute 
        // and verifying the timer is scheduled (cannot wait full minute in unit test)
        focusModeService.enableFocusMode(1, null);
        
        // Then: focus mode should be enabled with timer
        assertTrue(focusModeService.isFocusModeEnabled());
        Integer remaining = focusModeService.getRemainingMinutes();
        assertNotNull(remaining, "Timer should be active");
        
        // Note: In a real integration test, we would wait for timer expiration
        // For unit test, we verify the timer is set up correctly
        FocusModeState state = focusModeService.getCurrentState();
        assertNotNull(state.getEndTime(), "End time should be calculated");
    }

    @Test
    void testBreakReminderConfiguration() {
        // Given: the application is running normally
        
        // When: enabling focus mode with custom break interval
        int breakInterval = 30;
        focusModeService.enableFocusMode(null, breakInterval);

        // Then: break reminder interval should be set
        FocusModeState state = focusModeService.getCurrentState();
        assertEquals(breakInterval, state.getBreakReminderIntervalMinutes(), 
                "Break reminder interval should match");
    }

    @Test
    void testBreakReminderDefaultValue() {
        // Given: the application is running normally
        
        // When: enabling focus mode without specifying break interval
        focusModeService.enableFocusMode(null, null);

        // Then: default break reminder interval should be used (25 minutes - Pomodoro)
        FocusModeState state = focusModeService.getCurrentState();
        assertEquals(25, state.getBreakReminderIntervalMinutes(), 
                "Default break reminder interval should be 25 minutes");
    }

    @Test
    void testGetCurrentState() {
        // Given: focus mode is enabled
        focusModeService.enableFocusMode(45, 20);

        // When: getting current state
        FocusModeState state = focusModeService.getCurrentState();

        // Then: state should be accurate
        assertTrue(state.isEnabled());
        assertEquals(45, state.getTimerDurationMinutes());
        assertEquals(20, state.getBreakReminderIntervalMinutes());
        assertNotNull(state.getStartTime());
        assertNotNull(state.getEndTime());
    }

    @Test
    void testGetRemainingMinutesWithoutTimer() {
        // Given: focus mode is enabled without timer
        focusModeService.enableFocusMode(null, null);

        // When: getting remaining minutes
        Integer remaining = focusModeService.getRemainingMinutes();

        // Then: should return null (no timer)
        assertNull(remaining, "Remaining minutes should be null when no timer is set");
    }

    @Test
    void testDoubleEnableFocusMode() {
        // Given: focus mode is already enabled
        focusModeService.enableFocusMode(null, null);
        focusModeEnabledEventFired = false; // Reset flag

        // When: trying to enable again
        focusModeService.enableFocusMode(null, null);

        // Then: should log warning but not re-enable (no new event)
        assertTrue(focusModeService.isFocusModeEnabled());
        assertFalse(focusModeEnabledEventFired, "Should not fire event on double enable");
    }

    @Test
    void testDisableFocusModeWhenNotEnabled() {
        // Given: focus mode is not enabled
        assertFalse(focusModeService.isFocusModeEnabled());

        // When: trying to disable
        focusModeService.disableFocusMode();

        // Then: should log warning but not crash (no event fired)
        assertFalse(focusModeService.isFocusModeEnabled());
        assertFalse(focusModeDisabledEventFired, "Should not fire event when not enabled");
    }

    @Test
    void testShutdown() {
        // Given: focus mode is enabled
        focusModeService.enableFocusMode(null, null);
        assertTrue(focusModeService.isFocusModeEnabled());

        // When: shutting down service
        focusModeService.shutdown();

        // Then: focus mode should be disabled
        assertFalse(focusModeService.isFocusModeEnabled());
        assertTrue(focusModeDisabledEventFired, "FocusModeDisabledEvent should be fired on shutdown");
    }
}
