package net.talaatharb.workday.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;

/**
 * Tests for AccessibilityManager.
 */
class AccessibilityManagerTest {
    
    private AccessibilityManager accessibilityManager;
    
    @BeforeEach
    void setUp() {
        accessibilityManager = AccessibilityManager.getInstance();
    }
    
    @Test
    @DisplayName("Get instance returns singleton")
    void testGetInstance() {
        AccessibilityManager instance1 = AccessibilityManager.getInstance();
        AccessibilityManager instance2 = AccessibilityManager.getInstance();
        
        assertSame(instance1, instance2, "Should return same instance");
    }
    
    @Test
    @DisplayName("High contrast mode starts disabled")
    void testInitialState() {
        assertFalse(accessibilityManager.isHighContrastMode(), "High contrast should be disabled by default");
    }
    
    @Test
    @DisplayName("Set accessible name on node")
    void testSetAccessibleName() {
        Button button = new Button("Test");
        
        AccessibilityManager.setAccessibleName(button, "Test Button");
        
        assertEquals("Test Button", button.getAccessibleText());
    }
    
    @Test
    @DisplayName("Set accessible help on node")
    void testSetAccessibleHelp() {
        TextField field = new TextField();
        
        AccessibilityManager.setAccessibleHelp(field, "Enter your text here");
        
        assertEquals("Enter your text here", field.getAccessibleHelp());
    }
    
    @Test
    @DisplayName("Make node focusable")
    void testMakeFocusable() {
        Button button = new Button("Test");
        button.setFocusTraversable(false);
        
        AccessibilityManager.makeFocusable(button);
        
        assertTrue(button.isFocusTraversable());
    }
    
    @Test
    @DisplayName("Set accessible name handles null node")
    void testSetAccessibleName_NullNode() {
        assertDoesNotThrow(() -> AccessibilityManager.setAccessibleName(null, "Test"));
    }
    
    @Test
    @DisplayName("Set accessible name handles null name")
    void testSetAccessibleName_NullName() {
        Button button = new Button("Test");
        assertDoesNotThrow(() -> AccessibilityManager.setAccessibleName(button, null));
    }
    
    @Test
    @DisplayName("Make focusable handles null node")
    void testMakeFocusable_NullNode() {
        assertDoesNotThrow(() -> AccessibilityManager.makeFocusable(null));
    }
}
