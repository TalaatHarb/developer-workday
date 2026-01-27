package net.talaatharb.workday.repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.mapdb.DB;
import org.mapdb.Serializer;

import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.UserPreferences;

/**
 * Repository for user preferences persistence using MapDB.
 * Stores a single preferences object with ID 1L.
 */
@Slf4j
public class PreferencesRepository {
    
    private static final String PREFERENCES_MAP = "userPreferences";
    private static final Long DEFAULT_PREFS_ID = 1L;
    
    private final DB database;
    private final Map<Long, UserPreferences> preferencesMap;
    
    public PreferencesRepository(DB database) {
        this.database = database;
        this.preferencesMap = database.hashMap(PREFERENCES_MAP, Serializer.LONG, Serializer.JAVA).createOrOpen();
        log.info("PreferencesRepository initialized");
        
        // Initialize default preferences if not exists
        if (preferencesMap.isEmpty()) {
            UserPreferences defaultPrefs = UserPreferences.builder()
                .id(DEFAULT_PREFS_ID)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            preferencesMap.put(DEFAULT_PREFS_ID, defaultPrefs);
            database.commit();
            log.info("Initialized default user preferences");
        }
    }
    
    /**
     * Get user preferences. Returns default preferences if none exist.
     */
    public UserPreferences get() {
        return Optional.ofNullable(preferencesMap.get(DEFAULT_PREFS_ID))
            .orElseGet(() -> {
                UserPreferences defaultPrefs = UserPreferences.builder()
                    .id(DEFAULT_PREFS_ID)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                save(defaultPrefs);
                return defaultPrefs;
            });
    }
    
    /**
     * Save user preferences.
     */
    public UserPreferences save(UserPreferences preferences) {
        if (preferences.getId() == null) {
            preferences.setId(DEFAULT_PREFS_ID);
        }
        
        if (preferences.getCreatedAt() == null) {
            preferences.setCreatedAt(LocalDateTime.now());
        }
        preferences.setUpdatedAt(LocalDateTime.now());
        
        preferencesMap.put(preferences.getId(), preferences);
        database.commit();
        log.debug("Saved user preferences");
        return preferences;
    }
    
    /**
     * Reset preferences to defaults.
     */
    public void reset() {
        UserPreferences existing = get();
        UserPreferences defaultPrefs = UserPreferences.builder()
            .id(DEFAULT_PREFS_ID)
            .createdAt(existing.getCreatedAt())
            .updatedAt(LocalDateTime.now())
            .build();
        save(defaultPrefs);
        log.info("Reset user preferences to defaults");
    }
}
