package net.talaatharb.workday.utils;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.layout.Region;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for managing accessibility features in the application.
 * Provides methods for enhancing screen reader support, keyboard navigation,
 * and high contrast mode.
 */
@Slf4j
public class AccessibilityManager {
    
    private static AccessibilityManager instance;
    private boolean highContrastMode = false;
    
    private AccessibilityManager() {
        log.info("AccessibilityManager initialized");
    }
    
    public static AccessibilityManager getInstance() {
        if (instance == null) {
            instance = new AccessibilityManager();
        }
        return instance;
    }
    
    /**
     * Enable high contrast mode for better visibility
     */
    public void enableHighContrastMode(Scene scene) {
        if (highContrastMode) {
            return;
        }
        
        log.info("Enabling high contrast mode");
        highContrastMode = true;
        
        // Apply high contrast styles
        scene.getStylesheets().add(getClass().getResource("/styles/high-contrast.css").toExternalForm());
        
        // Add accessibility class to root
        if (scene.getRoot() != null) {
            scene.getRoot().getStyleClass().add("high-contrast");
        }
    }
    
    /**
     * Disable high contrast mode
     */
    public void disableHighContrastMode(Scene scene) {
        if (!highContrastMode) {
            return;
        }
        
        log.info("Disabling high contrast mode");
        highContrastMode = false;
        
        // Remove high contrast styles
        String highContrastCss = getClass().getResource("/styles/high-contrast.css").toExternalForm();
        scene.getStylesheets().remove(highContrastCss);
        
        // Remove accessibility class from root
        if (scene.getRoot() != null) {
            scene.getRoot().getStyleClass().remove("high-contrast");
        }
    }
    
    /**
     * Check if high contrast mode is enabled
     */
    public boolean isHighContrastMode() {
        return highContrastMode;
    }
    
    /**
     * Set accessible name for a node (screen reader label)
     */
    public static void setAccessibleName(Node node, String name) {
        if (node != null && name != null) {
            node.setAccessibleText(name);
        }
    }
    
    /**
     * Set accessible help text for a node
     */
    public static void setAccessibleHelp(Node node, String help) {
        if (node != null && help != null) {
            node.setAccessibleHelp(help);
        }
    }
    
    /**
     * Make a node focusable for keyboard navigation
     */
    public static void makeFocusable(Node node) {
        if (node != null) {
            node.setFocusTraversable(true);
        }
    }
    
    /**
     * Apply focus indicators to enhance keyboard navigation visibility
     */
    public static void enhanceFocusIndicators(Scene scene) {
        // Focus indicators are applied via CSS
        scene.getRoot().getStyleClass().add("enhanced-focus");
    }
    
    /**
     * Setup accessibility for a region and its children
     */
    public static void setupAccessibility(Region region) {
        if (region == null) {
            return;
        }
        
        // Make region focusable if it's a control
        if (region instanceof Control) {
            makeFocusable(region);
        }
        
        // Recursively setup for children
        for (Node child : region.getChildrenUnmodifiable()) {
            if (child instanceof Region) {
                setupAccessibility((Region) child);
            }
        }
    }
    
    /**
     * Announce a message to screen readers
     */
    public static void announce(Node node, String message) {
        if (node != null && message != null) {
            // Set accessible text temporarily to announce
            String original = node.getAccessibleText();
            node.setAccessibleText(message);
            
            // Restore original text after a short delay
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(100);
                    node.setAccessibleText(original);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
}
