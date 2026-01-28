package net.talaatharb.workday.utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for managing responsive layout behavior.
 * Handles window size changes and layout adaptation.
 */
@Slf4j
public class ResponsiveLayoutManager {
    
    // Breakpoint thresholds
    public static final double NARROW_WIDTH_THRESHOLD = 800;
    public static final double MEDIUM_WIDTH_THRESHOLD = 1200;
    public static final double WIDE_WIDTH_THRESHOLD = 1600;
    
    // Layout state properties
    private final BooleanProperty narrowLayout = new SimpleBooleanProperty(false);
    private final BooleanProperty compactMode = new SimpleBooleanProperty(false);
    
    private Stage stage;
    
    /**
     * Initialize responsive layout manager for a stage
     */
    public void initialize(Stage stage) {
        this.stage = stage;
        
        // Listen to window width changes
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateLayoutForWidth(newVal.doubleValue());
        });
        
        // Initial layout update
        updateLayoutForWidth(stage.getWidth());
    }
    
    /**
     * Update layout based on window width
     */
    private void updateLayoutForWidth(double width) {
        boolean wasNarrow = narrowLayout.get();
        boolean isNarrow = width < NARROW_WIDTH_THRESHOLD;
        
        if (wasNarrow != isNarrow) {
            narrowLayout.set(isNarrow);
            log.debug("Layout changed to: {}", isNarrow ? "narrow" : "wide");
        }
    }
    
    /**
     * Get narrow layout property
     */
    public BooleanProperty narrowLayoutProperty() {
        return narrowLayout;
    }
    
    /**
     * Check if currently in narrow layout
     */
    public boolean isNarrowLayout() {
        return narrowLayout.get();
    }
    
    /**
     * Get compact mode property
     */
    public BooleanProperty compactModeProperty() {
        return compactMode;
    }
    
    /**
     * Check if compact mode is enabled
     */
    public boolean isCompactMode() {
        return compactMode.get();
    }
    
    /**
     * Set compact mode
     */
    public void setCompactMode(boolean enabled) {
        compactMode.set(enabled);
        log.debug("Compact mode: {}", enabled);
    }
    
    /**
     * Apply compact mode styles to a node
     */
    public void applyCompactMode(Node node) {
        if (compactMode.get()) {
            node.getStyleClass().add("compact-mode");
        } else {
            node.getStyleClass().remove("compact-mode");
        }
    }
    
    /**
     * Collapse sidebar to icons only
     */
    public void collapseSidebar(Region sidebar, boolean collapse) {
        if (collapse) {
            sidebar.setMaxWidth(60);
            sidebar.setMinWidth(60);
            sidebar.getStyleClass().add("sidebar-collapsed");
        } else {
            sidebar.setMaxWidth(250);
            sidebar.setMinWidth(200);
            sidebar.getStyleClass().remove("sidebar-collapsed");
        }
    }
    
    /**
     * Toggle detail panel as modal overlay for narrow layouts
     */
    public void toggleDetailPanelModal(Region detailPanel, boolean asModal) {
        if (asModal) {
            detailPanel.getStyleClass().add("detail-panel-modal");
            // Could add overlay background here
        } else {
            detailPanel.getStyleClass().remove("detail-panel-modal");
        }
    }
    
    /**
     * Get recommended spacing for current mode
     */
    public double getSpacing() {
        if (compactMode.get()) {
            return 5.0;
        } else if (narrowLayout.get()) {
            return 8.0;
        } else {
            return 10.0;
        }
    }
    
    /**
     * Get recommended padding for current mode
     */
    public double getPadding() {
        if (compactMode.get()) {
            return 8.0;
        } else if (narrowLayout.get()) {
            return 12.0;
        } else {
            return 15.0;
        }
    }
    
    /**
     * Get recommended font size multiplier for current mode
     */
    public double getFontSizeMultiplier() {
        if (compactMode.get()) {
            return 0.9;
        } else if (narrowLayout.get()) {
            return 0.95;
        } else {
            return 1.0;
        }
    }
    
    /**
     * Get window size category
     */
    public WindowSize getWindowSize() {
        if (stage == null) {
            return WindowSize.MEDIUM;
        }
        
        double width = stage.getWidth();
        if (width < NARROW_WIDTH_THRESHOLD) {
            return WindowSize.NARROW;
        } else if (width < MEDIUM_WIDTH_THRESHOLD) {
            return WindowSize.MEDIUM;
        } else if (width < WIDE_WIDTH_THRESHOLD) {
            return WindowSize.WIDE;
        } else {
            return WindowSize.EXTRA_WIDE;
        }
    }
    
    /**
     * Window size categories
     */
    public enum WindowSize {
        NARROW,
        MEDIUM,
        WIDE,
        EXTRA_WIDE
    }
}
