package net.talaatharb.workday.facade;

import net.talaatharb.workday.config.DatabaseConfig;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.model.UserPreferences;
import net.talaatharb.workday.repository.PreferencesRepository;
import net.talaatharb.workday.service.PreferencesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;

import static org.junit.jupiter.api.Assertions.*;

class PreferencesFacadeTest {
    
    private DB database;
    private PreferencesRepository repository;
    private PreferencesService service;
    private PreferencesFacade facade;
    
    @BeforeEach
    void setUp() {
        database = DatabaseConfig.inMemoryDatabase();
        repository = new PreferencesRepository(database);
        EventDispatcher eventDispatcher = new EventDispatcher();
        service = new PreferencesService(repository, eventDispatcher);
        facade = new PreferencesFacade(service);
    }
    
    @AfterEach
    void tearDown() {
        database.close();
    }
    
    @Test
    void shouldGetPreferences() {
        UserPreferences prefs = facade.getPreferences();
        
        assertNotNull(prefs);
        assertEquals("today", prefs.getDefaultView());
    }
    
    @Test
    void shouldUpdatePreferences() {
        UserPreferences prefs = facade.getPreferences();
        prefs.setTheme("dark");
        prefs.setAccentColor("#FF0000");
        
        UserPreferences updated = facade.updatePreferences(prefs);
        
        assertEquals("dark", updated.getTheme());
        assertEquals("#FF0000", updated.getAccentColor());
        
        // Verify persistence
        UserPreferences retrieved = facade.getPreferences();
        assertEquals("dark", retrieved.getTheme());
        assertEquals("#FF0000", retrieved.getAccentColor());
    }
    
    @Test
    void shouldResetToDefaults() {
        // Modify
        UserPreferences prefs = facade.getPreferences();
        prefs.setFontSize(20);
        prefs.setDefaultView("calendar");
        facade.updatePreferences(prefs);
        
        // Reset
        UserPreferences reset = facade.resetToDefaults();
        
        assertEquals(13, reset.getFontSize());
        assertEquals("today", reset.getDefaultView());
    }
}
