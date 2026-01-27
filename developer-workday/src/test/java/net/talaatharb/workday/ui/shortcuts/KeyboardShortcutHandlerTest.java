package net.talaatharb.workday.ui.shortcuts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;

/**
 * Tests for KeyboardShortcutHandler.
 */
class KeyboardShortcutHandlerTest {
    
    private static boolean jfxInitialized = false;
    private Scene scene;
    private KeyboardShortcutHandler shortcutHandler;
    
    @BeforeAll
    static void initJFX() throws InterruptedException {
        if (!jfxInitialized) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform failed to initialize");
            jfxInitialized = true;
        }
    }
    
    @BeforeEach
    void setUp() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                VBox root = new VBox();
                scene = new Scene(root, 800, 600);
                shortcutHandler = new KeyboardShortcutHandler(scene);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Setup timeout");
    }
    
    @Test
    @DisplayName("Register shortcut - action is registered in scene accelerators")
    void testRegisterShortcut() throws InterruptedException {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        KeyCombination shortcut = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
        
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                shortcutHandler.registerShortcut(shortcut, () -> actionExecuted.set(true));
                
                // Verify the shortcut is registered in scene accelerators
                assertTrue(scene.getAccelerators().containsKey(shortcut));
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timeout");
    }
    
    @Test
    @DisplayName("Unregister shortcut - shortcut is removed from scene")
    void testUnregisterShortcut() throws InterruptedException {
        KeyCombination shortcut = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                shortcutHandler.registerShortcut(shortcut, () -> {});
                assertTrue(scene.getAccelerators().containsKey(shortcut));
                
                shortcutHandler.unregisterShortcut(shortcut);
                assertFalse(scene.getAccelerators().containsKey(shortcut));
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timeout");
    }
    
    @Test
    @DisplayName("Clear all shortcuts - all shortcuts are removed")
    void testClearAllShortcuts() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                KeyCombination shortcut1 = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
                KeyCombination shortcut2 = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
                
                shortcutHandler.registerShortcut(shortcut1, () -> {});
                shortcutHandler.registerShortcut(shortcut2, () -> {});
                
                assertEquals(2, scene.getAccelerators().size());
                
                shortcutHandler.clearAllShortcuts();
                assertEquals(0, scene.getAccelerators().size());
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timeout");
    }
    
    @Test
    @DisplayName("Predefined shortcuts are available")
    void testPredefinedShortcuts() {
        assertNotNull(KeyboardShortcutHandler.NEW_TASK);
        assertNotNull(KeyboardShortcutHandler.SEARCH);
        assertNotNull(KeyboardShortcutHandler.TODAY_VIEW);
        assertNotNull(KeyboardShortcutHandler.UPCOMING_VIEW);
        assertNotNull(KeyboardShortcutHandler.CALENDAR_VIEW);
        assertNotNull(KeyboardShortcutHandler.ALL_TASKS_VIEW);
        assertNotNull(KeyboardShortcutHandler.SAVE);
        assertNotNull(KeyboardShortcutHandler.CLOSE);
        assertNotNull(KeyboardShortcutHandler.QUIT);
        assertNotNull(KeyboardShortcutHandler.REFRESH);
    }
    
    @Test
    @DisplayName("Get shortcut display text")
    void testGetShortcutDisplayText() {
        KeyCombination shortcut = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
        String displayText = KeyboardShortcutHandler.getShortcutDisplayText(shortcut);
        
        assertNotNull(displayText);
        assertFalse(displayText.isEmpty());
    }
}
