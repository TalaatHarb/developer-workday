package net.talaatharb.workday.facade;

import net.talaatharb.workday.dtos.FocusModeDTO;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.focusmode.FocusModeDisabledEvent;
import net.talaatharb.workday.event.focusmode.FocusModeEnabledEvent;
import net.talaatharb.workday.service.FocusModeService;
import net.talaatharb.workday.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for FocusModeFacade (Task 57).
 * Tests the integration of focus mode with notification suppression.
 */
class FocusModeFacadeTest {

    private FocusModeFacade focusModeFacade;
    private FocusModeService focusModeService;
    private NotificationService notificationService;
    private EventDispatcher eventDispatcher;

    @BeforeEach
    void setUp() {
        eventDispatcher = new EventDispatcher();
        focusModeService = new FocusModeService(eventDispatcher);
        notificationService = new NotificationService(null);
        focusModeFacade = new FocusModeFacade(focusModeService, notificationService, eventDispatcher);
    }

    @AfterEach
    void tearDown() {
        if (focusModeService != null) {
            focusModeService.shutdown();
        }
    }

    @Test
    void testEnableFocusModeSuppressesNotifications() {
        // Given: the application is running normally
        assertFalse(focusModeFacade.isFocusModeEnabled());
        assertFalse(notificationService.isNotificationsSuppressed(), 
                "Notifications should not be suppressed initially");

        // When: enabling focus mode
        focusModeFacade.enableFocusMode();

        // Then: notifications should be suppressed
        assertTrue(focusModeFacade.isFocusModeEnabled(), "Focus mode should be enabled");
        assertTrue(notificationService.isNotificationsSuppressed(), 
                "Notifications should be suppressed when focus mode is enabled");
    }

    @Test
    void testDisableFocusModeRestoresNotifications() {
        // Given: focus mode is enabled and notifications are suppressed
        focusModeFacade.enableFocusMode();
        assertTrue(notificationService.isNotificationsSuppressed());

        // When: disabling focus mode
        focusModeFacade.disableFocusMode();

        // Then: notifications should be restored
        assertFalse(focusModeFacade.isFocusModeEnabled(), "Focus mode should be disabled");
        assertFalse(notificationService.isNotificationsSuppressed(), 
                "Notifications should be restored when focus mode is disabled");
    }

    @Test
    void testToggleFocusMode() {
        // Given: focus mode is disabled
        assertFalse(focusModeFacade.isFocusModeEnabled());

        // When: toggling focus mode (first time)
        focusModeFacade.toggleFocusMode();

        // Then: focus mode should be enabled
        assertTrue(focusModeFacade.isFocusModeEnabled());

        // When: toggling focus mode (second time)
        focusModeFacade.toggleFocusMode();

        // Then: focus mode should be disabled
        assertFalse(focusModeFacade.isFocusModeEnabled());
    }

    @Test
    void testEnableFocusModeWithTimer() {
        // Given: the application is running normally
        
        // When: enabling focus mode with a 25-minute timer
        focusModeFacade.enableFocusMode(25);

        // Then: focus mode should be enabled with timer
        assertTrue(focusModeFacade.isFocusModeEnabled());
        
        FocusModeDTO state = focusModeFacade.getCurrentState();
        assertTrue(state.isEnabled());
        assertEquals(25, state.getTimerDurationMinutes());
        assertNotNull(state.getRemainingMinutes());
    }

    @Test
    void testEnableFocusModeWithTimerAndBreakInterval() {
        // Given: the application is running normally
        
        // When: enabling focus mode with custom timer and break interval
        focusModeFacade.enableFocusMode(50, 20);

        // Then: focus mode should be enabled with both configured
        FocusModeDTO state = focusModeFacade.getCurrentState();
        assertTrue(state.isEnabled());
        assertEquals(50, state.getTimerDurationMinutes());
        assertEquals(20, state.getBreakReminderIntervalMinutes());
    }

    @Test
    void testGetCurrentState() {
        // Given: focus mode is disabled
        assertFalse(focusModeFacade.isFocusModeEnabled());

        // When: getting current state
        FocusModeDTO state = focusModeFacade.getCurrentState();

        // Then: state should reflect disabled status
        assertFalse(state.isEnabled());
        assertNull(state.getTimerDurationMinutes());
        assertNull(state.getRemainingMinutes());

        // When: enabling focus mode
        focusModeFacade.enableFocusMode(30, 15);
        state = focusModeFacade.getCurrentState();

        // Then: state should reflect enabled status with configuration
        assertTrue(state.isEnabled());
        assertEquals(30, state.getTimerDurationMinutes());
        assertEquals(15, state.getBreakReminderIntervalMinutes());
        assertNotNull(state.getStartTime());
        assertNotNull(state.getEndTime());
        assertNotNull(state.getRemainingMinutes());
    }

    @Test
    void testFocusModeEventHandlers() throws InterruptedException {
        // Given: focus mode is disabled
        assertFalse(notificationService.isNotificationsSuppressed());

        // When: FocusModeEnabledEvent is dispatched
        eventDispatcher.publish(new FocusModeEnabledEvent(25));
        Thread.sleep(100); // Give event time to process

        // Then: notifications should be suppressed
        assertTrue(notificationService.isNotificationsSuppressed(), 
                "Notifications should be suppressed on FocusModeEnabledEvent");

        // When: FocusModeDisabledEvent is dispatched
        eventDispatcher.publish(new FocusModeDisabledEvent(false));
        Thread.sleep(100); // Give event time to process

        // Then: notifications should be restored
        assertFalse(notificationService.isNotificationsSuppressed(), 
                "Notifications should be restored on FocusModeDisabledEvent");
    }
}
