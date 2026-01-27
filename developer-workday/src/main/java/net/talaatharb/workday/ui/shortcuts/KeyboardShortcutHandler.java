package net.talaatharb.workday.ui.shortcuts;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles keyboard shortcuts for the application.
 * Provides methods to register and manage keyboard shortcuts.
 */
@Slf4j
public class KeyboardShortcutHandler {
    
    private final Scene scene;
    
    // Keyboard shortcuts
    public static final KeyCombination NEW_TASK = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination SEARCH = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination TODAY_VIEW = new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination UPCOMING_VIEW = new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination CALENDAR_VIEW = new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination ALL_TASKS_VIEW = new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination SAVE = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination CLOSE = new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination QUIT = new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination REFRESH = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination QUICK_ACTIONS = new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
    
    /**
     * Create a new keyboard shortcut handler for the given scene
     */
    public KeyboardShortcutHandler(Scene scene) {
        this.scene = scene;
        log.info("KeyboardShortcutHandler initialized");
    }
    
    /**
     * Register a keyboard shortcut with an action
     */
    public void registerShortcut(KeyCombination shortcut, Runnable action) {
        scene.getAccelerators().put(shortcut, action);
        log.debug("Registered shortcut: {}", shortcut.getDisplayText());
    }
    
    /**
     * Unregister a keyboard shortcut
     */
    public void unregisterShortcut(KeyCombination shortcut) {
        scene.getAccelerators().remove(shortcut);
        log.debug("Unregistered shortcut: {}", shortcut.getDisplayText());
    }
    
    /**
     * Clear all shortcuts
     */
    public void clearAllShortcuts() {
        scene.getAccelerators().clear();
        log.debug("Cleared all shortcuts");
    }
    
    /**
     * Get the display text for a shortcut
     */
    public static String getShortcutDisplayText(KeyCombination shortcut) {
        return shortcut.getDisplayText();
    }
}
