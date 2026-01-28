package net.talaatharb.workday.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.UserPreferences;
import net.talaatharb.workday.service.PreferencesService;

/**
 * Facade for preferences-related operations.
 * Provides a simplified interface for UI controllers.
 */
@Slf4j
@RequiredArgsConstructor
public class PreferencesFacade {
    
    private final PreferencesService preferencesService;
    
    /**
     * Get current user preferences.
     */
    public UserPreferences getPreferences() {
        log.debug("Getting user preferences via facade");
        return preferencesService.getPreferences();
    }
    
    /**
     * Update user preferences.
     */
    public UserPreferences updatePreferences(UserPreferences preferences) {
        log.debug("Updating user preferences via facade");
        return preferencesService.updatePreferences(preferences);
    }
    
    /**
     * Reset preferences to defaults.
     */
    public UserPreferences resetToDefaults() {
        log.debug("Resetting preferences to defaults via facade");
        return preferencesService.resetToDefaults();
    }
}
