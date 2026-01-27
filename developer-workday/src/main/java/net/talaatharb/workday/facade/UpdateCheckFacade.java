package net.talaatharb.workday.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.UpdateInfo;
import net.talaatharb.workday.service.UpdateCheckService;

/**
 * Facade for update checking operations.
 * Coordinates update checks and provides a simplified interface for UI controllers.
 */
@Slf4j
@RequiredArgsConstructor
public class UpdateCheckFacade {
    
    private final UpdateCheckService updateCheckService;
    
    /**
     * Check for updates if auto-check is enabled and conditions are met
     * @return UpdateInfo if check was performed, null otherwise
     */
    public UpdateInfo checkForUpdatesIfEnabled() {
        if (updateCheckService.shouldCheckForUpdates()) {
            return updateCheckService.checkForUpdates();
        }
        return null;
    }
    
    /**
     * Manually check for updates (triggered by user)
     * @return UpdateInfo containing version information
     */
    public UpdateInfo checkForUpdatesManually() {
        log.info("Manual update check requested");
        return updateCheckService.checkForUpdates();
    }
    
    /**
     * Get the current application version
     * @return Current version string
     */
    public String getCurrentVersion() {
        return updateCheckService.getCurrentVersion();
    }
}
