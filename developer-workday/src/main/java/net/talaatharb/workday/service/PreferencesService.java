package net.talaatharb.workday.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.preferences.PreferencesUpdatedEvent;
import net.talaatharb.workday.model.UserPreferences;
import net.talaatharb.workday.repository.PreferencesRepository;

/**
 * Service class for user preferences business logic.
 */
@Slf4j
@RequiredArgsConstructor
public class PreferencesService {
    
    private final PreferencesRepository preferencesRepository;
    private final EventDispatcher eventDispatcher;
    
    /**
     * Get current user preferences.
     */
    public UserPreferences getPreferences() {
        log.debug("Getting user preferences");
        return preferencesRepository.get();
    }
    
    /**
     * Update user preferences.
     */
    public UserPreferences updatePreferences(UserPreferences preferences) {
        log.debug("Updating user preferences");
        
        UserPreferences oldPreferences = preferencesRepository.get();
        UserPreferences savedPreferences = preferencesRepository.save(preferences);
        
        // Publish event
        eventDispatcher.publish(new PreferencesUpdatedEvent(oldPreferences, savedPreferences));
        
        log.info("Updated user preferences");
        return savedPreferences;
    }
    
    /**
     * Reset preferences to defaults.
     */
    public UserPreferences resetToDefaults() {
        log.debug("Resetting preferences to defaults");
        
        UserPreferences oldPreferences = preferencesRepository.get();
        preferencesRepository.reset();
        UserPreferences newPreferences = preferencesRepository.get();
        
        // Publish event
        eventDispatcher.publish(new PreferencesUpdatedEvent(oldPreferences, newPreferences));
        
        log.info("Reset preferences to defaults");
        return newPreferences;
    }
}
