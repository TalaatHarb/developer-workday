package net.talaatharb.workday.utils;

import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ResponsiveLayoutManager.
 */
class ResponsiveLayoutManagerTest extends ApplicationTest {

    @Override
    public void start(Stage stage) {
        stage.show();
    }

    @Test
    void testResponsiveLayoutManagerInitialization() {
        // Given
        Stage stage = new Stage();
        ResponsiveLayoutManager manager = new ResponsiveLayoutManager();
        
        // When
        manager.initialize(stage);
        
        // Then
        assertNotNull(manager.narrowLayoutProperty());
        assertNotNull(manager.compactModeProperty());
    }

    @Test
    void testNarrowLayoutDetection() {
        // Given
        Stage stage = new Stage();
        ResponsiveLayoutManager manager = new ResponsiveLayoutManager();
        manager.initialize(stage);
        
        // When: Set narrow width
        stage.setWidth(700);
        
        // Then
        assertTrue(manager.isNarrowLayout());
    }

    @Test
    void testWideLayoutDetection() {
        // Given
        Stage stage = new Stage();
        ResponsiveLayoutManager manager = new ResponsiveLayoutManager();
        manager.initialize(stage);
        
        // When: Set wide width
        stage.setWidth(1300);
        
        // Then
        assertFalse(manager.isNarrowLayout());
    }

    @Test
    void testCompactMode() {
        // Given
        ResponsiveLayoutManager manager = new ResponsiveLayoutManager();
        
        // When
        manager.setCompactMode(true);
        
        // Then
        assertTrue(manager.isCompactMode());
    }

    @Test
    void testCompactModeSpacing() {
        // Given
        ResponsiveLayoutManager manager = new ResponsiveLayoutManager();
        
        // When: Enable compact mode
        manager.setCompactMode(true);
        double compactSpacing = manager.getSpacing();
        
        // Then: Spacing should be smaller in compact mode
        assertTrue(compactSpacing < 10.0);
        assertEquals(5.0, compactSpacing);
    }

    @Test
    void testSidebarCollapse() {
        // Given
        ResponsiveLayoutManager manager = new ResponsiveLayoutManager();
        VBox sidebar = new VBox();
        sidebar.setMinWidth(200);
        sidebar.setMaxWidth(250);
        
        // When
        manager.collapseSidebar(sidebar, true);
        
        // Then
        assertEquals(60, sidebar.getMinWidth());
        assertEquals(60, sidebar.getMaxWidth());
        assertTrue(sidebar.getStyleClass().contains("sidebar-collapsed"));
    }

    @Test
    void testSidebarExpand() {
        // Given
        ResponsiveLayoutManager manager = new ResponsiveLayoutManager();
        VBox sidebar = new VBox();
        sidebar.setMinWidth(60);
        sidebar.setMaxWidth(60);
        sidebar.getStyleClass().add("sidebar-collapsed");
        
        // When
        manager.collapseSidebar(sidebar, false);
        
        // Then
        assertEquals(200, sidebar.getMinWidth());
        assertEquals(250, sidebar.getMaxWidth());
        assertFalse(sidebar.getStyleClass().contains("sidebar-collapsed"));
    }

    @Test
    void testDetailPanelModal() {
        // Given
        ResponsiveLayoutManager manager = new ResponsiveLayoutManager();
        VBox detailPanel = new VBox();
        
        // When
        manager.toggleDetailPanelModal(detailPanel, true);
        
        // Then
        assertTrue(detailPanel.getStyleClass().contains("detail-panel-modal"));
    }

    @Test
    void testWindowSizeCategories() {
        // Given
        Stage stage = new Stage();
        ResponsiveLayoutManager manager = new ResponsiveLayoutManager();
        manager.initialize(stage);
        
        // When: Narrow
        stage.setWidth(700);
        assertEquals(ResponsiveLayoutManager.WindowSize.NARROW, manager.getWindowSize());
        
        // When: Medium
        stage.setWidth(1000);
        assertEquals(ResponsiveLayoutManager.WindowSize.MEDIUM, manager.getWindowSize());
        
        // When: Wide
        stage.setWidth(1400);
        assertEquals(ResponsiveLayoutManager.WindowSize.WIDE, manager.getWindowSize());
        
        // When: Extra Wide
        stage.setWidth(1800);
        assertEquals(ResponsiveLayoutManager.WindowSize.EXTRA_WIDE, manager.getWindowSize());
    }

    @Test
    void testFontSizeMultiplier() {
        // Given
        ResponsiveLayoutManager manager = new ResponsiveLayoutManager();
        
        // When: Normal mode
        double normalMultiplier = manager.getFontSizeMultiplier();
        
        // When: Compact mode
        manager.setCompactMode(true);
        double compactMultiplier = manager.getFontSizeMultiplier();
        
        // Then
        assertEquals(1.0, normalMultiplier);
        assertTrue(compactMultiplier < normalMultiplier);
    }
}
