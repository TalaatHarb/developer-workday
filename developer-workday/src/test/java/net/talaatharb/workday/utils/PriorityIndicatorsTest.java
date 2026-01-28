package net.talaatharb.workday.utils;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import net.talaatharb.workday.model.Priority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class PriorityIndicatorsTest {
    
    @Start
    void start(Stage stage) {
        // Just need JavaFX environment
    }
    
    @Test
    void shouldReturnCorrectColorForUrgentPriority() {
        assertEquals(PriorityIndicators.URGENT_COLOR, 
            PriorityIndicators.getColorForPriority(Priority.URGENT));
    }
    
    @Test
    void shouldReturnCorrectColorForHighPriority() {
        assertEquals(PriorityIndicators.HIGH_COLOR, 
            PriorityIndicators.getColorForPriority(Priority.HIGH));
    }
    
    @Test
    void shouldReturnCorrectColorForMediumPriority() {
        assertEquals(PriorityIndicators.MEDIUM_COLOR, 
            PriorityIndicators.getColorForPriority(Priority.MEDIUM));
    }
    
    @Test
    void shouldReturnCorrectColorForLowPriority() {
        assertEquals(PriorityIndicators.LOW_COLOR, 
            PriorityIndicators.getColorForPriority(Priority.LOW));
    }
    
    @Test
    void shouldReturnDefaultColorForNullPriority() {
        assertEquals(PriorityIndicators.NONE_COLOR, 
            PriorityIndicators.getColorForPriority(null));
    }
    
    @Test
    void shouldReturnCorrectIconForEachPriority() {
        assertEquals(PriorityIndicators.URGENT_ICON, 
            PriorityIndicators.getIconForPriority(Priority.URGENT));
        assertEquals(PriorityIndicators.HIGH_ICON, 
            PriorityIndicators.getIconForPriority(Priority.HIGH));
        assertEquals(PriorityIndicators.MEDIUM_ICON, 
            PriorityIndicators.getIconForPriority(Priority.MEDIUM));
        assertEquals(PriorityIndicators.LOW_ICON, 
            PriorityIndicators.getIconForPriority(Priority.LOW));
    }
    
    @Test
    void shouldReturnEmptyIconForNullPriority() {
        assertEquals("", PriorityIndicators.getIconForPriority(null));
    }
    
    @Test
    void shouldReturnCorrectDisplayNames() {
        assertEquals("Urgent", PriorityIndicators.getDisplayName(Priority.URGENT));
        assertEquals("High", PriorityIndicators.getDisplayName(Priority.HIGH));
        assertEquals("Medium", PriorityIndicators.getDisplayName(Priority.MEDIUM));
        assertEquals("Low", PriorityIndicators.getDisplayName(Priority.LOW));
        assertEquals("None", PriorityIndicators.getDisplayName(null));
    }
    
    @Test
    void shouldCreatePriorityCircle() {
        double size = 20.0;
        Circle circle = PriorityIndicators.createPriorityCircle(Priority.URGENT, size);
        
        assertNotNull(circle);
        assertEquals(size / 2, circle.getRadius());
        assertNotNull(circle.getFill());
    }
    
    @Test
    void shouldCreatePriorityIndicatorWithIcon() {
        HBox indicator = PriorityIndicators.createPriorityIndicator(Priority.HIGH, false);
        
        assertNotNull(indicator);
        // Should have icon but no text
        assertEquals(1, indicator.getChildren().size());
    }
    
    @Test
    void shouldCreatePriorityIndicatorWithIconAndText() {
        HBox indicator = PriorityIndicators.createPriorityIndicator(Priority.MEDIUM, true);
        
        assertNotNull(indicator);
        // Should have both icon and text
        assertEquals(2, indicator.getChildren().size());
    }
    
    @Test
    void shouldCreatePriorityBadge() {
        double size = 24.0;
        Label badge = PriorityIndicators.createPriorityBadge(Priority.URGENT, size);
        
        assertNotNull(badge);
        assertEquals("U", badge.getText()); // First letter of "Urgent"
        assertTrue(badge.getStyle().contains("-fx-background-color"));
    }
    
    @Test
    void shouldGeneratePriorityBackgroundStyle() {
        String style = PriorityIndicators.getPriorityBackgroundStyle(Priority.URGENT, 0.5);
        
        assertNotNull(style);
        assertTrue(style.contains("rgba"));
        assertTrue(style.contains("0.50")); // opacity
    }
    
    @Test
    void shouldGeneratePriorityBorderStyle() {
        String style = PriorityIndicators.getPriorityBorderStyle(Priority.HIGH, 3.0);
        
        assertNotNull(style);
        assertTrue(style.contains("-fx-border-color"));
        assertTrue(style.contains("3.0")); // border width
    }
    
    @Test
    void shouldHandleAllPriorityLevels() {
        // Test that all priority levels can be processed without errors
        for (Priority priority : Priority.values()) {
            assertNotNull(PriorityIndicators.getColorForPriority(priority));
            assertNotNull(PriorityIndicators.getIconForPriority(priority));
            assertNotNull(PriorityIndicators.getDisplayName(priority));
            assertNotNull(PriorityIndicators.createPriorityCircle(priority, 16.0));
            assertNotNull(PriorityIndicators.createPriorityIndicator(priority, true));
            assertNotNull(PriorityIndicators.createPriorityBadge(priority, 20.0));
        }
    }
    
    @Test
    void shouldUseConsistentColorScheme() {
        // Verify that colors follow the expected pattern
        assertTrue(PriorityIndicators.URGENT_COLOR.startsWith("#"));
        assertTrue(PriorityIndicators.HIGH_COLOR.startsWith("#"));
        assertTrue(PriorityIndicators.MEDIUM_COLOR.startsWith("#"));
        assertTrue(PriorityIndicators.LOW_COLOR.startsWith("#"));
        
        // Verify color lengths (hex format)
        assertEquals(7, PriorityIndicators.URGENT_COLOR.length());
        assertEquals(7, PriorityIndicators.HIGH_COLOR.length());
        assertEquals(7, PriorityIndicators.MEDIUM_COLOR.length());
        assertEquals(7, PriorityIndicators.LOW_COLOR.length());
    }
}
