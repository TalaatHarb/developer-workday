package net.talaatharb.workday.ui.shortcuts;

import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * Global keyboard shortcut manager for Linux platform.
 */
@Slf4j
public class LinuxGlobalShortcutManager {
    private final Stage primaryStage;
    private boolean shortcutRegistered = false;
    private static final String DEFAULT_SHORTCUT = "Ctrl+Shift+A";
    
    public LinuxGlobalShortcutManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    
    public boolean registerShortcut() {
        log.info("Registering Linux global shortcut (stub): {}", DEFAULT_SHORTCUT);
        shortcutRegistered = true;
        return true;
    }
    
    public void unregisterShortcut() {
        shortcutRegistered = false;
    }
    
    public boolean isShortcutRegistered() {
        return shortcutRegistered;
    }
}
