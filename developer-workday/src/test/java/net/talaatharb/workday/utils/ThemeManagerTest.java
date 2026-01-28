package net.talaatharb.workday.utils;

import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class ThemeManagerTest {
    
    private Scene scene;
    private ThemeManager themeManager;
    
    @Start
    void start(Stage stage) {
        VBox root = new VBox();
        scene = new Scene(root, 300, 200);
        stage.setScene(scene);
        stage.show();
        
        themeManager = ThemeManager.getInstance();
    }
    
    @Test
    void shouldStartWithLightThemeByDefault() {
        assertEquals("light", themeManager.getCurrentTheme());
        assertTrue(themeManager.isLightTheme());
        assertFalse(themeManager.isDarkTheme());
    }
    
    @Test
    void shouldRegisterAndApplyThemeToScene() {
        themeManager.registerScene(scene);
        themeManager.applyTheme("light");
        
        // Check that scene has theme stylesheet
        boolean hasThemeStylesheet = scene.getStylesheets().stream()
            .anyMatch(s -> s.contains("theme-light.css"));
        assertTrue(hasThemeStylesheet);
    }
    
    @Test
    void shouldSwitchToDarkTheme() {
        themeManager.registerScene(scene);
        themeManager.applyTheme("dark");
        
        assertEquals("dark", themeManager.getCurrentTheme());
        assertTrue(themeManager.isDarkTheme());
        assertFalse(themeManager.isLightTheme());
        
        // Check that scene has dark theme stylesheet
        boolean hasDarkTheme = scene.getStylesheets().stream()
            .anyMatch(s -> s.contains("theme-dark.css"));
        assertTrue(hasDarkTheme);
    }
    
    @Test
    void shouldSwitchBackToLightTheme() {
        themeManager.registerScene(scene);
        themeManager.applyTheme("dark");
        themeManager.applyTheme("light");
        
        assertEquals("light", themeManager.getCurrentTheme());
        
        // Check that scene has light theme stylesheet
        boolean hasLightTheme = scene.getStylesheets().stream()
            .anyMatch(s -> s.contains("theme-light.css"));
        assertTrue(hasLightTheme);
        
        // Check that dark theme is removed
        boolean hasDarkTheme = scene.getStylesheets().stream()
            .anyMatch(s -> s.contains("theme-dark.css"));
        assertFalse(hasDarkTheme);
    }
    
    @Test
    void shouldUnregisterScene() {
        themeManager.registerScene(scene);
        themeManager.unregisterScene(scene);
        
        // Theme changes should not affect unregistered scene
        int stylesheetCountBefore = scene.getStylesheets().size();
        themeManager.applyTheme("dark");
        int stylesheetCountAfter = scene.getStylesheets().size();
        
        // Count should not change after theme switch
        assertEquals(stylesheetCountBefore, stylesheetCountAfter);
    }
    
    @Test
    void shouldHandleInvalidTheme() {
        themeManager.registerScene(scene);
        themeManager.applyTheme("invalid");
        
        // Should default to light
        assertEquals("light", themeManager.getCurrentTheme());
    }
    
    @Test
    void shouldHandleNullTheme() {
        themeManager.registerScene(scene);
        themeManager.applyTheme(null);
        
        // Should default to light
        assertEquals("light", themeManager.getCurrentTheme());
    }
}
