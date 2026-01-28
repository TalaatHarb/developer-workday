package net.talaatharb.workday.service;

import net.talaatharb.workday.config.DatabaseConfig;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.preferences.PreferencesUpdatedEvent;
import net.talaatharb.workday.model.UserPreferences;
import net.talaatharb.workday.repository.PreferencesRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;

import static org.junit.jupiter.api.Assertions.*;

class PreferencesServiceTest {
    
    private DB database;
    private PreferencesRepository repository;
    private EventDispatcher eventDispatcher;
    private PreferencesService service;
    private boolean eventPublished;
    
    @BeforeEach
    void setUp() {
        database = DatabaseConfig.inMemoryDatabase();
        repository = new PreferencesRepository(database);
        eventDispatcher = new EventDispatcher();
        service = new PreferencesService(repository, eventDispatcher);
        eventPublished = false;
        
        // Listen for events
        eventDispatcher.subscribe(PreferencesUpdatedEvent.class, event -> {
            eventPublished = true;
        });
    }
    
    @AfterEach
    void tearDown() {
        database.close();
    }
    
    @Test
    void shouldGetPreferences() {
        UserPreferences prefs = service.getPreferences();
        
        assertNotNull(prefs);
        assertNotNull(prefs.getId());
    }
    
    @Test
    void shouldUpdatePreferencesAndPublishEvent() {
        UserPreferences prefs = service.getPreferences();
        prefs.setTheme("dark");
        prefs.setLanguage("es");
        
        UserPreferences updated = service.updatePreferences(prefs);
        
        assertEquals("dark", updated.getTheme());
        assertEquals("es", updated.getLanguage());
        assertTrue(eventPublished);
    }
    
    @Test
    void shouldResetToDefaults() {
        // Modify preferences
        UserPreferences prefs = service.getPreferences();
        prefs.setFontSize(20);
        prefs.setTheme("dark");
        service.updatePreferences(prefs);
        
        eventPublished = false;
        
        // Reset
        UserPreferences reset = service.resetToDefaults();
        
        assertEquals(13, reset.getFontSize());
        assertEquals("light", reset.getTheme());
        assertTrue(eventPublished);
    }
}
