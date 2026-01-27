package net.talaatharb.workday.service;

import net.talaatharb.workday.event.EventDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test notification suppression during focus mode (Task 57).
 */
class NotificationServiceFocusModeTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Create notification service without tray icon (notifications not supported in test)
        notificationService = new NotificationService(null);
    }

    @Test
    void testNotificationSuppression() {
        // Given: notifications are not suppressed initially
        assertFalse(notificationService.isNotificationsSuppressed(), 
                "Notifications should not be suppressed initially");

        // When: enabling notification suppression
        notificationService.setSuppressNotifications(true);

        // Then: notifications should be suppressed
        assertTrue(notificationService.isNotificationsSuppressed(), 
                "Notifications should be suppressed");
    }

    @Test
    void testNotificationUnSuppression() {
        // Given: notifications are suppressed
        notificationService.setSuppressNotifications(true);
        assertTrue(notificationService.isNotificationsSuppressed());

        // When: disabling notification suppression
        notificationService.setSuppressNotifications(false);

        // Then: notifications should not be suppressed
        assertFalse(notificationService.isNotificationsSuppressed(), 
                "Notifications should not be suppressed after disabling");
    }

    @Test
    void testNotificationSupportedCheck() {
        // When: checking if notifications are supported (without tray icon)
        boolean supported = notificationService.isNotificationSupported();

        // Then: should not be supported in test environment
        assertFalse(supported, "Notifications should not be supported without tray icon");
    }
}
