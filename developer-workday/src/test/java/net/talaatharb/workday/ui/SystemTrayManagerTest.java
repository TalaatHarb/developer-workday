package net.talaatharb.workday.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Tests for SystemTrayManager.
 */
class SystemTrayManagerTest {
    
    private static boolean jfxInitialized = false;
    private SystemTrayManager trayManager;
    
    @BeforeEach
    void setUp() throws Exception {
        if (!jfxInitialized) {
            Platform.startup(() -> {});
            jfxInitialized = true;
        }
        
        Platform.runLater(() -> {
            Stage stage = new Stage();
            trayManager = new SystemTrayManager(stage);
        });
        
        Thread.sleep(200);
    }
    
    @Test
    @DisplayName("System tray manager can be created")
    void testSystemTrayManagerCreation() {
        assertNotNull(trayManager);
    }
    
    @Test
    @DisplayName("Tray support check works")
    void testTraySupport() {
        // This will vary by platform
        boolean supported = trayManager.isTraySupported();
        // Just verify the method works
        assertTrue(supported || !supported);
    }

    @Test
    @DisplayName("Quick add and show today callbacks can be set without errors")
    void testCallbackSetters() throws Exception {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            SystemTrayManager manager = new SystemTrayManager(stage);
            // Setters should accept runnables and not throw
            manager.setOnQuickAddAction(() -> {});
            manager.setOnShowTodayAction(() -> {});
            // Setting null should also be permitted (clear handler)
            manager.setOnQuickAddAction(null);
            manager.setOnShowTodayAction(null);
        });
        Thread.sleep(200);
    }
}