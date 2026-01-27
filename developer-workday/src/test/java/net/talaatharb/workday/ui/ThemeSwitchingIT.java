package net.talaatharb.workday.ui;

import javafx.scene.Scene;
import javafx.stage.Stage;
import net.talaatharb.workday.config.DatabaseConfig;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.facade.PreferencesFacade;
import net.talaatharb.workday.model.UserPreferences;
import net.talaatharb.workday.repository.PreferencesRepository;
import net.talaatharb.workday.service.PreferencesService;
import net.talaatharb.workday.utils.ThemeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapdb.DB;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import javafx.scene.layout.VBox;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for theme switching functionality.
 */
@ExtendWith(ApplicationExtension.class)
class ThemeSwitchingIT {
    
    private DB database;
    private PreferencesFacade preferencesFacade;
    private ThemeManager themeManager;
    private Scene scene;
    
    @Start
    void start(Stage stage) {
        database = DatabaseConfig.inMemoryDatabase();
        PreferencesRepository repository = new PreferencesRepository(database);
        EventDispatcher eventDispatcher = new EventDispatcher();
        PreferencesService service = new PreferencesService(repository, eventDispatcher);
        preferencesFacade = new PreferencesFacade(service);
        
        themeManager = ThemeManager.getInstance();
        
        VBox root = new VBox();
        scene = new Scene(root, 400, 300);
        themeManager.registerScene(scene);
        
        stage.setScene(scene);
        stage.show();
    }
    
    @AfterEach
    void tearDown() {
        if (database != null) {
            database.close();
        }
        if (scene != null) {
            themeManager.unregisterScene(scene);
        }
    }
    
    @Test
    void shouldSwitchThemeAndPersist() {
        // Start with light theme
        themeManager.applyTheme("light");
        assertEquals("light", themeManager.getCurrentTheme());
        
        // Save dark theme preference
        UserPreferences prefs = preferencesFacade.getPreferences();
        prefs.setTheme("dark");
        preferencesFacade.updatePreferences(prefs);
        
        // Apply dark theme
        themeManager.applyTheme("dark");
        assertEquals("dark", themeManager.getCurrentTheme());
        
        // Verify scene has dark theme
        boolean hasDarkTheme = scene.getStylesheets().stream()
            .anyMatch(s -> s.contains("theme-dark.css"));
        assertTrue(hasDarkTheme);
        
        // Verify preference was saved
        UserPreferences retrieved = preferencesFacade.getPreferences();
        assertEquals("dark", retrieved.getTheme());
    }
    
    @Test
    void shouldApplyThemeImmediately() {
        // Switch to dark theme
        themeManager.applyTheme("dark");
        
        // Verify immediate application
        assertTrue(scene.getStylesheets().stream()
            .anyMatch(s -> s.contains("theme-dark.css")));
        
        // Switch back to light
        themeManager.applyTheme("light");
        
        // Verify immediate switch
        assertTrue(scene.getStylesheets().stream()
            .anyMatch(s -> s.contains("theme-light.css")));
        assertFalse(scene.getStylesheets().stream()
            .anyMatch(s -> s.contains("theme-dark.css")));
    }
    
    @Test
    void shouldLoadThemeFromPreferences() {
        // Set dark theme in preferences
        UserPreferences prefs = preferencesFacade.getPreferences();
        prefs.setTheme("dark");
        preferencesFacade.updatePreferences(prefs);
        
        // Apply theme from preferences
        UserPreferences loaded = preferencesFacade.getPreferences();
        themeManager.applyTheme(loaded.getTheme());
        
        // Verify dark theme is applied
        assertEquals("dark", themeManager.getCurrentTheme());
        assertTrue(scene.getStylesheets().stream()
            .anyMatch(s -> s.contains("theme-dark.css")));
    }
}
