package net.talaatharb.workday.utils;

import javafx.application.Platform;
import javafx.scene.Scene;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages application theme switching between light and dark modes.
 * Provides centralized theme management for all scenes.
 */
@Slf4j
public class ThemeManager {
    
    private static final String LIGHT_THEME_CSS = "/net/talaatharb/workday/ui/theme-light.css";
    private static final String DARK_THEME_CSS = "/net/talaatharb/workday/ui/theme-dark.css";
    private static final String LEGACY_THEME_CSS = "/net/talaatharb/workday/ui/theme.css";
    
    private static ThemeManager instance;
    private final List<Scene> registeredScenes = new ArrayList<>();
    private String currentTheme = "light";
    
    private ThemeManager() {
        log.info("ThemeManager initialized");
    }
    
    /**
     * Get the singleton instance of ThemeManager.
     */
    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    /**
     * Register a scene to be managed by the theme manager.
     * The scene will automatically have themes applied when changed.
     */
    public void registerScene(Scene scene) {
        if (!registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
            applyThemeToScene(scene, currentTheme);
            log.debug("Registered scene for theme management");
        }
    }
    
    /**
     * Unregister a scene from theme management.
     */
    public void unregisterScene(Scene scene) {
        registeredScenes.remove(scene);
        log.debug("Unregistered scene from theme management");
    }
    
    /**
     * Apply a theme to all registered scenes.
     * @param theme "light" or "dark"
     */
    public void applyTheme(String theme) {
        if (theme == null || (!theme.equals("light") && !theme.equals("dark"))) {
            log.warn("Invalid theme: {}, defaulting to light", theme);
            theme = "light";
        }
        
        final String finalTheme = theme;
        
        if (Platform.isFxApplicationThread()) {
            applyThemeInternal(finalTheme);
        } else {
            Platform.runLater(() -> applyThemeInternal(finalTheme));
        }
    }
    
    private void applyThemeInternal(String theme) {
        currentTheme = theme;
        log.info("Applying {} theme to {} scenes", theme, registeredScenes.size());
        
        for (Scene scene : new ArrayList<>(registeredScenes)) {
            applyThemeToScene(scene, theme);
        }
    }
    
    private void applyThemeToScene(Scene scene, String theme) {
        if (scene == null) {
            return;
        }
        
        try {
            // Remove all existing theme stylesheets
            scene.getStylesheets().removeIf(stylesheet -> 
                stylesheet.contains("theme.css") || 
                stylesheet.contains("theme-light.css") || 
                stylesheet.contains("theme-dark.css")
            );
            
            // Add the appropriate theme
            String themeResource = theme.equals("dark") ? DARK_THEME_CSS : LIGHT_THEME_CSS;
            String themeUrl = getClass().getResource(themeResource).toExternalForm();
            scene.getStylesheets().add(themeUrl);
            
            log.debug("Applied {} theme to scene", theme);
        } catch (Exception e) {
            log.error("Failed to apply theme to scene", e);
        }
    }
    
    /**
     * Get the current theme name.
     * @return "light" or "dark"
     */
    public String getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * Check if dark theme is currently active.
     */
    public boolean isDarkTheme() {
        return "dark".equals(currentTheme);
    }
    
    /**
     * Check if light theme is currently active.
     */
    public boolean isLightTheme() {
        return "light".equals(currentTheme);
    }
}
