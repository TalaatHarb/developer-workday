package net.talaatharb.workday.utils;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * Utility class for creating smooth animations and transitions throughout the UI.
 * Provides reusable animation methods for common UI patterns.
 */
public class AnimationHelper {
    
    // Standard animation durations
    public static final Duration FAST = Duration.millis(150);
    public static final Duration NORMAL = Duration.millis(300);
    public static final Duration SLOW = Duration.millis(500);
    
    /**
     * Animate a node fading in
     */
    public static FadeTransition fadeIn(Node node) {
        return fadeIn(node, NORMAL);
    }
    
    /**
     * Animate a node fading in with custom duration
     */
    public static FadeTransition fadeIn(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        return fade;
    }
    
    /**
     * Animate a node fading out
     */
    public static FadeTransition fadeOut(Node node) {
        return fadeOut(node, NORMAL);
    }
    
    /**
     * Animate a node fading out with custom duration
     */
    public static FadeTransition fadeOut(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        return fade;
    }
    
    /**
     * Animate a node sliding in from the right
     */
    public static TranslateTransition slideInFromRight(Node node) {
        return slideInFromRight(node, NORMAL);
    }
    
    /**
     * Animate a node sliding in from the right with custom duration
     */
    public static TranslateTransition slideInFromRight(Node node, Duration duration) {
        double width = node instanceof Region ? ((Region) node).getWidth() : 300;
        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setFromX(width);
        slide.setToX(0);
        return slide;
    }
    
    /**
     * Animate a node sliding out to the right
     */
    public static TranslateTransition slideOutToRight(Node node) {
        return slideOutToRight(node, NORMAL);
    }
    
    /**
     * Animate a node sliding out to the right with custom duration
     */
    public static TranslateTransition slideOutToRight(Node node, Duration duration) {
        double width = node instanceof Region ? ((Region) node).getWidth() : 300;
        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setFromX(0);
        slide.setToX(width);
        return slide;
    }
    
    /**
     * Animate a node sliding in from the left
     */
    public static TranslateTransition slideInFromLeft(Node node) {
        return slideInFromLeft(node, NORMAL);
    }
    
    /**
     * Animate a node sliding in from the left with custom duration
     */
    public static TranslateTransition slideInFromLeft(Node node, Duration duration) {
        double width = node instanceof Region ? ((Region) node).getWidth() : 300;
        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setFromX(-width);
        slide.setToX(0);
        return slide;
    }
    
    /**
     * Animate a node sliding down (for adding items to list)
     */
    public static TranslateTransition slideDown(Node node) {
        return slideDown(node, FAST);
    }
    
    /**
     * Animate a node sliding down with custom duration
     */
    public static TranslateTransition slideDown(Node node, Duration duration) {
        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setFromY(-20);
        slide.setToY(0);
        return slide;
    }
    
    /**
     * Animate a node sliding up (for removing items from list)
     */
    public static TranslateTransition slideUp(Node node) {
        return slideUp(node, FAST);
    }
    
    /**
     * Animate a node sliding up with custom duration
     */
    public static TranslateTransition slideUp(Node node, Duration duration) {
        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setFromY(0);
        slide.setToY(-20);
        return slide;
    }
    
    /**
     * Animate adding a new item to a list
     */
    public static ParallelTransition addItemAnimation(Node node) {
        FadeTransition fade = fadeIn(node, FAST);
        TranslateTransition slide = slideDown(node);
        
        ParallelTransition parallel = new ParallelTransition(fade, slide);
        return parallel;
    }
    
    /**
     * Animate removing an item from a list
     */
    public static SequentialTransition removeItemAnimation(Node node, Runnable onComplete) {
        FadeTransition fade = fadeOut(node, FAST);
        ScaleTransition scale = new ScaleTransition(FAST, node);
        scale.setToX(0.8);
        scale.setToY(0.8);
        
        ParallelTransition parallel = new ParallelTransition(fade, scale);
        
        SequentialTransition sequence = new SequentialTransition(parallel);
        if (onComplete != null) {
            sequence.setOnFinished(e -> onComplete.run());
        }
        
        return sequence;
    }
    
    /**
     * Animate panel sliding in from right with fade
     */
    public static ParallelTransition slideInPanelFromRight(Node node) {
        FadeTransition fade = fadeIn(node, NORMAL);
        TranslateTransition slide = slideInFromRight(node, NORMAL);
        
        ParallelTransition parallel = new ParallelTransition(fade, slide);
        return parallel;
    }
    
    /**
     * Animate panel sliding out to right with fade
     */
    public static ParallelTransition slideOutPanelToRight(Node node) {
        FadeTransition fade = fadeOut(node, NORMAL);
        TranslateTransition slide = slideOutToRight(node, NORMAL);
        
        ParallelTransition parallel = new ParallelTransition(fade, slide);
        return parallel;
    }
    
    /**
     * Animate checkbox check with scale effect
     */
    public static SequentialTransition checkboxCheckAnimation(CheckBox checkBox) {
        // Scale up slightly then back to normal
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), checkBox);
        scaleUp.setToX(1.2);
        scaleUp.setToY(1.2);
        
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), checkBox);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        
        SequentialTransition sequence = new SequentialTransition(scaleUp, scaleDown);
        return sequence;
    }
    
    /**
     * Animate task completion with strikethrough effect
     */
    public static ParallelTransition taskCompletionAnimation(Node taskNode) {
        // Fade slightly and scale down
        FadeTransition fade = new FadeTransition(NORMAL, taskNode);
        fade.setToValue(0.6);
        
        ScaleTransition scale = new ScaleTransition(NORMAL, taskNode);
        scale.setToX(0.98);
        scale.setToY(0.98);
        
        ParallelTransition parallel = new ParallelTransition(fade, scale);
        return parallel;
    }
    
    /**
     * Animate task restoration (uncomplete)
     */
    public static ParallelTransition taskRestorationAnimation(Node taskNode) {
        // Fade back to full opacity and restore scale
        FadeTransition fade = new FadeTransition(FAST, taskNode);
        fade.setToValue(1.0);
        
        ScaleTransition scale = new ScaleTransition(FAST, taskNode);
        scale.setToX(1.0);
        scale.setToY(1.0);
        
        ParallelTransition parallel = new ParallelTransition(fade, scale);
        return parallel;
    }
    
    /**
     * Create a subtle pulse animation for highlighting
     */
    public static Timeline pulseAnimation(Node node) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(600), node);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(Duration.ZERO, e -> pulse.play()));
        
        return timeline;
    }
    
    /**
     * Create a shake animation for error states
     */
    public static TranslateTransition shakeAnimation(Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        
        return shake;
    }
}
