package net.talaatharb.workday.repository;

import net.talaatharb.workday.config.DatabaseConfig;
import net.talaatharb.workday.model.UserPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;

import static org.junit.jupiter.api.Assertions.*;

class PreferencesRepositoryTest {
    
    private DB database;
    private PreferencesRepository repository;
    
    @BeforeEach
    void setUp() {
        database = DatabaseConfig.inMemoryDatabase();
        repository = new PreferencesRepository(database);
    }
    
    @AfterEach
    void tearDown() {
        database.close();
    }
    
    @Test
    void shouldInitializeWithDefaults() {
        UserPreferences prefs = repository.get();
        
        assertNotNull(prefs);
        assertNotNull(prefs.getId());
        assertEquals("today", prefs.getDefaultView());
        assertEquals("light", prefs.getTheme());
        assertEquals("en", prefs.getLanguage());
        assertTrue(prefs.isMinimizeToTray());
        assertFalse(prefs.isStartOnBoot());
    }
    
    @Test
    void shouldSaveAndRetrievePreferences() {
        UserPreferences prefs = repository.get();
        prefs.setTheme("dark");
        prefs.setDefaultView("calendar");
        prefs.setStartOnBoot(true);
        
        UserPreferences saved = repository.save(prefs);
        
        assertNotNull(saved.getUpdatedAt());
        assertEquals("dark", saved.getTheme());
        assertEquals("calendar", saved.getDefaultView());
        assertTrue(saved.isStartOnBoot());
        
        // Retrieve again
        UserPreferences retrieved = repository.get();
        assertEquals("dark", retrieved.getTheme());
        assertEquals("calendar", retrieved.getDefaultView());
        assertTrue(retrieved.isStartOnBoot());
    }
    
    @Test
    void shouldResetToDefaults() {
        // Modify preferences
        UserPreferences prefs = repository.get();
        prefs.setTheme("dark");
        prefs.setFontSize(18);
        repository.save(prefs);
        
        // Reset
        repository.reset();
        
        UserPreferences reset = repository.get();
        assertEquals("light", reset.getTheme());
        assertEquals(13, reset.getFontSize());
    }
    
    @Test
    void shouldPersistAcrossReinitialization() {
        // Save preferences
        UserPreferences prefs = repository.get();
        prefs.setAccentColor("#FF5733");
        prefs.setFontSize(16);
        repository.save(prefs);
        
        // Create new repository instance with same DB
        PreferencesRepository newRepository = new PreferencesRepository(database);
        UserPreferences retrieved = newRepository.get();
        
        assertEquals("#FF5733", retrieved.getAccentColor());
        assertEquals(16, retrieved.getFontSize());
    }
}
