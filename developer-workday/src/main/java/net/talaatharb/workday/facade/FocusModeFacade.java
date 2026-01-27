package net.talaatharb.workday.facade;

import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.dtos.FocusModeDTO;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.focusmode.BreakReminderEvent;
import net.talaatharb.workday.event.focusmode.FocusModeDisabledEvent;
import net.talaatharb.workday.event.focusmode.FocusModeEnabledEvent;
import net.talaatharb.workday.model.FocusModeState;
import net.talaatharb.workday.service.FocusModeService;
import net.talaatharb.workday.service.NotificationService;

/**
 * Facade for focus mode operations.
 */
@Slf4j
public class FocusModeFacade {

    private final FocusModeService focusModeService;
    private final NotificationService notificationService;
    private final EventDispatcher eventDispatcher;

    public FocusModeFacade(FocusModeService focusModeService,
                           NotificationService notificationService,
                           EventDispatcher eventDispatcher) {
        this.focusModeService = focusModeService;
        this.notificationService = notificationService;
        this.eventDispatcher = eventDispatcher;
        
        // Subscribe to focus mode events to sync notification suppression
        eventDispatcher.subscribe(FocusModeEnabledEvent.class, this::handleFocusModeEnabled);
        eventDispatcher.subscribe(FocusModeDisabledEvent.class, this::handleFocusModeDisabled);
        
        log.info("FocusModeFacade initialized");
    }

    /**
     * Enable focus mode.
     */
    public void enableFocusMode(Integer timerDurationMinutes, Integer breakReminderIntervalMinutes) {
        log.info("Enabling focus mode via facade. Timer: {} minutes", timerDurationMinutes);
        focusModeService.enableFocusMode(timerDurationMinutes, breakReminderIntervalMinutes);
    }

    /**
     * Enable focus mode with default break interval.
     */
    public void enableFocusMode(Integer timerDurationMinutes) {
        enableFocusMode(timerDurationMinutes, null);
    }

    /**
     * Enable focus mode without timer.
     */
    public void enableFocusMode() {
        enableFocusMode(null, null);
    }

    /**
     * Disable focus mode.
     */
    public void disableFocusMode() {
        log.info("Disabling focus mode via facade");
        focusModeService.disableFocusMode();
    }

    /**
     * Toggle focus mode on/off.
     */
    public void toggleFocusMode() {
        if (isFocusModeEnabled()) {
            disableFocusMode();
        } else {
            enableFocusMode();
        }
    }

    /**
     * Check if focus mode is enabled.
     */
    public boolean isFocusModeEnabled() {
        return focusModeService.isFocusModeEnabled();
    }

    /**
     * Get current focus mode state as DTO.
     */
    public FocusModeDTO getCurrentState() {
        FocusModeState state = focusModeService.getCurrentState();
        Integer remainingMinutes = focusModeService.getRemainingMinutes();
        
        return FocusModeDTO.builder()
                .enabled(state.isEnabled())
                .startTime(state.getStartTime())
                .endTime(state.getEndTime())
                .timerDurationMinutes(state.getTimerDurationMinutes())
                .remainingMinutes(remainingMinutes)
                .breakReminderIntervalMinutes(state.getBreakReminderIntervalMinutes())
                .build();
    }

    /**
     * Handle focus mode enabled event.
     */
    private void handleFocusModeEnabled(FocusModeEnabledEvent event) {
        log.info("Focus mode enabled - suppressing notifications");
        notificationService.setSuppressNotifications(true);
    }

    /**
     * Handle focus mode disabled event.
     */
    private void handleFocusModeDisabled(FocusModeDisabledEvent event) {
        log.info("Focus mode disabled - re-enabling notifications");
        notificationService.setSuppressNotifications(false);
    }
}
