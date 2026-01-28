package net.talaatharb.workday.utils;

import javafx.animation.*;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AnimationHelper utility class.
 */
class AnimationHelperTest extends ApplicationTest {

    @BeforeAll
    static void initJavaFX() {
        // JavaFX toolkit will be initialized by TestFX
    }

    @Test
    void testFadeInAnimation() {
        // Given
        Label label = new Label("Test");
        
        // When
        FadeTransition fadeIn = AnimationHelper.fadeIn(label);
        
        // Then
        assertNotNull(fadeIn);
        assertEquals(0.0, fadeIn.getFromValue());
        assertEquals(1.0, fadeIn.getToValue());
        assertEquals(AnimationHelper.NORMAL, fadeIn.getDuration());
    }

    @Test
    void testFadeOutAnimation() {
        // Given
        Label label = new Label("Test");
        
        // When
        FadeTransition fadeOut = AnimationHelper.fadeOut(label);
        
        // Then
        assertNotNull(fadeOut);
        assertEquals(1.0, fadeOut.getFromValue());
        assertEquals(0.0, fadeOut.getToValue());
    }

    @Test
    void testSlideInFromRight() {
        // Given
        VBox box = new VBox();
        box.setPrefWidth(300);
        
        // When
        TranslateTransition slide = AnimationHelper.slideInFromRight(box);
        
        // Then
        assertNotNull(slide);
        assertEquals(0, slide.getToX());
    }

    @Test
    void testSlideDown() {
        // Given
        Label label = new Label("Test");
        
        // When
        TranslateTransition slide = AnimationHelper.slideDown(label);
        
        // Then
        assertNotNull(slide);
        assertEquals(-20, slide.getFromY());
        assertEquals(0, slide.getToY());
    }

    @Test
    void testAddItemAnimation() {
        // Given
        Label label = new Label("Test");
        
        // When
        ParallelTransition animation = AnimationHelper.addItemAnimation(label);
        
        // Then
        assertNotNull(animation);
        assertTrue(animation.getChildren().size() > 0);
    }

    @Test
    void testRemoveItemAnimation() {
        // Given
        Label label = new Label("Test");
        boolean[] called = {false};
        
        // When
        SequentialTransition animation = AnimationHelper.removeItemAnimation(label, () -> called[0] = true);
        
        // Then
        assertNotNull(animation);
        assertNotNull(animation.getOnFinished());
    }

    @Test
    void testCheckboxCheckAnimation() {
        // Given
        CheckBox checkBox = new CheckBox("Test");
        
        // When
        SequentialTransition animation = AnimationHelper.checkboxCheckAnimation(checkBox);
        
        // Then
        assertNotNull(animation);
        assertTrue(animation.getChildren().size() > 0);
    }

    @Test
    void testTaskCompletionAnimation() {
        // Given
        Label label = new Label("Test Task");
        
        // When
        ParallelTransition animation = AnimationHelper.taskCompletionAnimation(label);
        
        // Then
        assertNotNull(animation);
        assertTrue(animation.getChildren().size() > 0);
    }

    @Test
    void testTaskRestorationAnimation() {
        // Given
        Label label = new Label("Test Task");
        
        // When
        ParallelTransition animation = AnimationHelper.taskRestorationAnimation(label);
        
        // Then
        assertNotNull(animation);
        assertTrue(animation.getChildren().size() > 0);
    }

    @Test
    void testPulseAnimation() {
        // Given
        Label label = new Label("Test");
        
        // When
        Timeline animation = AnimationHelper.pulseAnimation(label);
        
        // Then
        assertNotNull(animation);
    }

    @Test
    void testShakeAnimation() {
        // Given
        Label label = new Label("Test");
        
        // When
        TranslateTransition animation = AnimationHelper.shakeAnimation(label);
        
        // Then
        assertNotNull(animation);
        assertEquals(0, animation.getFromX());
        assertEquals(10, animation.getByX());
        assertEquals(4, animation.getCycleCount());
        assertTrue(animation.isAutoReverse());
    }

    @Test
    void testAnimationDurations() {
        // Verify duration constants
        assertTrue(AnimationHelper.FAST.toMillis() < AnimationHelper.NORMAL.toMillis());
        assertTrue(AnimationHelper.NORMAL.toMillis() < AnimationHelper.SLOW.toMillis());
        
        assertEquals(150, AnimationHelper.FAST.toMillis());
        assertEquals(300, AnimationHelper.NORMAL.toMillis());
        assertEquals(500, AnimationHelper.SLOW.toMillis());
    }
}
