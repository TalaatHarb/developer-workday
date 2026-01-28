package net.talaatharb.workday.service;

import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.focusmode.BreakReminderEvent;
import net.talaatharb.workday.event.focusmode.FocusModeDisabledEvent;
import net.talaatharb.workday.event.focusmode.FocusModeEnabledEvent;
import net.talaatharb.workday.model.FocusModeState;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Service for managing focus mode / do not disturb functionality.
 */
@Slf4j
public class FocusModeService {

    private final EventDispatcher eventDispatcher;
    private FocusModeState currentState;
    private Timer focusTimer;
    private Timer breakReminderTimer;

    public FocusModeService(EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
        this.currentState = FocusModeState.builder().build();
    }

    /**
     * Enable focus mode.
     *
     * @param timerDurationMinutes Optional timer duration in minutes (null for no timer)
     * @param breakReminderIntervalMinutes Interval for break reminders (null to use default)
     */
    public void enableFocusMode(Integer timerDurationMinutes, Integer breakReminderIntervalMinutes) {
        if (currentState.isEnabled()) {
            log.warn("Focus mode is already enabled");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = timerDurationMinutes != null
                ? now.plusMinutes(timerDurationMinutes)
                : null;

        currentState = FocusModeState.builder()
                .enabled(true)
                .startTime(now)
                .endTime(endTime)
                .timerDurationMinutes(timerDurationMinutes)
                .breakReminderIntervalMinutes(
                        breakReminderIntervalMinutes != null ? breakReminderIntervalMinutes : 25)
                .lastBreakReminderTime(now)
                .build();

        log.info("Focus mode enabled. Timer: {} minutes, Break interval: {} minutes",
                timerDurationMinutes, currentState.getBreakReminderIntervalMinutes());

        // Start timer if duration is specified
        if (timerDurationMinutes != null && timerDurationMinutes > 0) {
            startFocusTimer(timerDurationMinutes);
        }

        // Start break reminder timer
        startBreakReminderTimer(currentState.getBreakReminderIntervalMinutes());

        // Dispatch event
        eventDispatcher.publish(new FocusModeEnabledEvent(timerDurationMinutes));
    }

    /**
     * Disable focus mode.
     */
    public void disableFocusMode() {
        disableFocusMode(false);
    }

    /**
     * Disable focus mode.
     *
     * @param expiredByTimer True if disabled automatically by timer expiration
     */
    private void disableFocusMode(boolean expiredByTimer) {
        if (!currentState.isEnabled()) {
            log.warn("Focus mode is not enabled");
            return;
        }

        log.info("Focus mode disabled. Expired by timer: {}", expiredByTimer);

        currentState.setEnabled(false);
        currentState.setEndTime(LocalDateTime.now());

        // Cancel timers
        cancelTimers();

        // Dispatch event
        eventDispatcher.publish(new FocusModeDisabledEvent(expiredByTimer));
    }

    /**
     * Check if focus mode is currently enabled.
     */
    public boolean isFocusModeEnabled() {
        return currentState.isEnabled();
    }

    /**
     * Get the current focus mode state.
     */
    public FocusModeState getCurrentState() {
        return FocusModeState.builder()
                .enabled(currentState.isEnabled())
                .startTime(currentState.getStartTime())
                .endTime(currentState.getEndTime())
                .timerDurationMinutes(currentState.getTimerDurationMinutes())
                .breakReminderIntervalMinutes(currentState.getBreakReminderIntervalMinutes())
                .lastBreakReminderTime(currentState.getLastBreakReminderTime())
                .build();
    }

    /**
     * Get remaining time in minutes (if timer is active).
     */
    public Integer getRemainingMinutes() {
        if (!currentState.isEnabled() || currentState.getEndTime() == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(currentState.getEndTime())) {
            return 0;
        }

        Duration duration = Duration.between(now, currentState.getEndTime());
        return (int) duration.toMinutes();
    }

    /**
     * Start the focus timer.
     */
    private void startFocusTimer(int durationMinutes) {
        focusTimer = new Timer("FocusModeTimer", true);
        focusTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info("Focus mode timer expired after {} minutes", durationMinutes);
                disableFocusMode(true);
            }
        }, durationMinutes * 60L * 1000);
        
        log.debug("Focus timer scheduled for {} minutes", durationMinutes);
    }

    /**
     * Start the break reminder timer.
     */
    private void startBreakReminderTimer(int intervalMinutes) {
        breakReminderTimer = new Timer("BreakReminderTimer", true);
        breakReminderTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (currentState.isEnabled()) {
                    log.info("Break reminder triggered after {} minutes", intervalMinutes);
                    currentState.setLastBreakReminderTime(LocalDateTime.now());
                    
                    // Calculate session duration
                    Duration sessionDuration = Duration.between(
                            currentState.getStartTime(), LocalDateTime.now());
                    int sessionMinutes = (int) sessionDuration.toMinutes();
                    
                    eventDispatcher.publish(new BreakReminderEvent(sessionMinutes));
                }
            }
        }, intervalMinutes * 60L * 1000, intervalMinutes * 60L * 1000);
        
        log.debug("Break reminder timer scheduled every {} minutes", intervalMinutes);
    }

    /**
     * Cancel all active timers.
     */
    private void cancelTimers() {
        if (focusTimer != null) {
            focusTimer.cancel();
            focusTimer = null;
            log.debug("Focus timer cancelled");
        }
        if (breakReminderTimer != null) {
            breakReminderTimer.cancel();
            breakReminderTimer = null;
            log.debug("Break reminder timer cancelled");
        }
    }

    /**
     * Clean up resources.
     */
    public void shutdown() {
        log.info("Shutting down FocusModeService");
        cancelTimers();
        if (currentState.isEnabled()) {
            disableFocusMode();
        }
    }
}
