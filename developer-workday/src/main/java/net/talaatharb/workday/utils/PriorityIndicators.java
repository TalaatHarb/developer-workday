package net.talaatharb.workday.utils;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import net.talaatharb.workday.model.Priority;

/**
 * Utility class for creating visual indicators for task priorities.
 * Provides consistent priority colors and indicators across the application.
 */
public class PriorityIndicators {
    
    // Priority colors
    public static final String URGENT_COLOR = "#c5000b";   // Red
    public static final String HIGH_COLOR = "#ff8c00";     // Orange
    public static final String MEDIUM_COLOR = "#ffd700";   // Yellow
    public static final String LOW_COLOR = "#95a5a6";      // Gray
    public static final String NONE_COLOR = "#bdc3c7";     // Light gray
    
    // Priority icons (using Unicode symbols)
    public static final String URGENT_ICON = "🔴";
    public static final String HIGH_ICON = "🟠";
    public static final String MEDIUM_ICON = "🟡";
    public static final String LOW_ICON = "⚪";
    
    private PriorityIndicators() {
        // Utility class
    }
    
    /**
     * Get the color for a priority level.
     */
    public static String getColorForPriority(Priority priority) {
        if (priority == null) {
            return NONE_COLOR;
        }
        
        switch (priority) {
            case URGENT:
                return URGENT_COLOR;
            case HIGH:
                return HIGH_COLOR;
            case MEDIUM:
                return MEDIUM_COLOR;
            case LOW:
                return LOW_COLOR;
            default:
                return NONE_COLOR;
        }
    }
    
    /**
     * Get the icon for a priority level.
     */
    public static String getIconForPriority(Priority priority) {
        if (priority == null) {
            return "";
        }
        
        switch (priority) {
            case URGENT:
                return URGENT_ICON;
            case HIGH:
                return HIGH_ICON;
            case MEDIUM:
                return MEDIUM_ICON;
            case LOW:
                return LOW_ICON;
            default:
                return "";
        }
    }
    
    /**
     * Get the display name for a priority level.
     */
    public static String getDisplayName(Priority priority) {
        if (priority == null) {
            return "None";
        }
        
        switch (priority) {
            case URGENT:
                return "Urgent";
            case HIGH:
                return "High";
            case MEDIUM:
                return "Medium";
            case LOW:
                return "Low";
            default:
                return "None";
        }
    }
    
    /**
     * Create a visual priority indicator as a colored circle.
     * @param priority The task priority
     * @param size The diameter of the circle in pixels
     * @return A Circle node with appropriate color
     */
    public static Circle createPriorityCircle(Priority priority, double size) {
        Circle circle = new Circle(size / 2);
        String color = getColorForPriority(priority);
        circle.setFill(Color.web(color));
        circle.setStroke(Color.web(color).darker());
        circle.setStrokeWidth(1);
        return circle;
    }
    
    /**
     * Create a visual priority indicator as a label with icon and text.
     * @param priority The task priority
     * @param showText Whether to show the priority name
     * @return An HBox containing the priority indicator
     */
    public static HBox createPriorityIndicator(Priority priority, boolean showText) {
        HBox container = new HBox(5);
        container.setStyle("-fx-alignment: center-left;");
        
        // Add icon
        String icon = getIconForPriority(priority);
        if (!icon.isEmpty()) {
            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 14px;");
            container.getChildren().add(iconLabel);
        }
        
        // Add text if requested
        if (showText) {
            Label textLabel = new Label(getDisplayName(priority));
            String color = getColorForPriority(priority);
            textLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-weight: bold;", color));
            container.getChildren().add(textLabel);
        }
        
        return container;
    }
    
    /**
     * Create a compact priority badge (circle with first letter).
     * @param priority The task priority
     * @param size The size of the badge in pixels
     * @return A Label styled as a priority badge
     */
    public static Label createPriorityBadge(Priority priority, double size) {
        String displayName = getDisplayName(priority);
        String firstLetter = displayName.isEmpty() ? "" : displayName.substring(0, 1);
        
        Label badge = new Label(firstLetter);
        String color = getColorForPriority(priority);
        
        badge.setStyle(String.format(
            "-fx-background-color: %s;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: %f;" +
            "-fx-min-width: %f;" +
            "-fx-min-height: %f;" +
            "-fx-max-width: %f;" +
            "-fx-max-height: %f;" +
            "-fx-font-size: %f;" +
            "-fx-font-weight: bold;" +
            "-fx-alignment: center;",
            color, size / 2, size, size, size, size, size * 0.6
        ));
        
        return badge;
    }
    
    /**
     * Apply priority-based background color to a node.
     * @param node The node to style
     * @param priority The task priority
     * @param opacity The opacity of the color (0.0 to 1.0)
     * @return The CSS style string
     */
    public static String getPriorityBackgroundStyle(Priority priority, double opacity) {
        String color = getColorForPriority(priority);
        
        // Convert hex color to RGB for opacity
        Color jfxColor = Color.web(color);
        return String.format(
            "-fx-background-color: rgba(%d, %d, %d, %.2f);",
            (int) (jfxColor.getRed() * 255),
            (int) (jfxColor.getGreen() * 255),
            (int) (jfxColor.getBlue() * 255),
            opacity
        );
    }
    
    /**
     * Get a border style for the given priority.
     * @param priority The task priority
     * @param width The border width in pixels
     * @return The CSS style string
     */
    public static String getPriorityBorderStyle(Priority priority, double width) {
        String color = getColorForPriority(priority);
        return String.format(
            "-fx-border-color: %s; -fx-border-width: 0 0 0 %f;",
            color, width
        );
    }
}
