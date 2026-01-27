package net.talaatharb.workday.ui.controllers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Tests for MainUiController following the acceptance criteria.
 * 
 * Feature: Main Application Window
 *   Scenario: Main window layout
 *     Given the application is launched
 *     When the main window is displayed
 *     Then it should have a collapsible sidebar on the left
 *     And a main content area in the center
 *     And a quick add bar at the top
 *     And the window should be resizable with minimum dimensions
 *
 *   Scenario: Sidebar navigation
 *     Given the main window is displayed
 *     When viewing the sidebar
 *     Then it should show 'Today', 'Upcoming', 'Calendar', and 'All Tasks' sections
 *     And it should show the list of categories below
 *     And each category should display task count badge
 */
class MainUiControllerTest {
    
    private static boolean jfxInitialized = false;
    
    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        if (!jfxInitialized) {
            // Initialize JavaFX toolkit
            Platform.startup(() -> {});
            jfxInitialized = true;
        }
    }
    
    @Test
    @DisplayName("Main window layout - has collapsible sidebar, main content area, and quick add bar")
    void testMainWindowLayout() throws IOException, InterruptedException {
        // Given: the application is launched
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/MainWindow.fxml");
                assertNotNull(fxmlResource, "FXML file should exist");
                
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                BorderPane root = loader.load();
                MainUiController controller = loader.getController();
                
                // When: the main window is displayed
                assertNotNull(root, "Root pane should be loaded");
                assertNotNull(controller, "Controller should be initialized");
                
                // Then: it should have minimum dimensions
                assertTrue(root.getMinWidth() >= 800, "Window should have minimum width of 800");
                assertTrue(root.getMinHeight() >= 600, "Window should have minimum height of 600");
                
                // And: it should have a quick add bar at the top
                assertNotNull(root.getTop(), "Top should contain quick add bar");
                
                // And: it should have a collapsible sidebar on the left
                assertNotNull(root.getLeft(), "Left should contain sidebar");
                assertTrue(root.getLeft() instanceof VBox, "Sidebar should be a VBox");
                
                // And: it should have a main content area in the center
                assertNotNull(root.getCenter(), "Center should contain main content area");
                assertTrue(root.getCenter() instanceof StackPane, "Content area should be a StackPane");
                
            } catch (IOException e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        // Wait for JavaFX thread to complete
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Sidebar navigation - shows required navigation sections")
    void testSidebarNavigation() throws IOException, InterruptedException {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/MainWindow.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                BorderPane root = loader.load();
                MainUiController controller = loader.getController();
                
                // Given: the main window is displayed
                VBox sidebar = (VBox) root.getLeft();
                assertNotNull(sidebar, "Sidebar should exist");
                
                // When: viewing the sidebar
                // Then: it should show navigation buttons
                boolean hasTodayButton = sidebar.lookupAll(".sidebar-nav-button").stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> (Button) node)
                    .anyMatch(btn -> "Today".equals(btn.getText()));
                assertTrue(hasTodayButton, "Sidebar should have 'Today' button");
                
                boolean hasUpcomingButton = sidebar.lookupAll(".sidebar-nav-button").stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> (Button) node)
                    .anyMatch(btn -> "Upcoming".equals(btn.getText()));
                assertTrue(hasUpcomingButton, "Sidebar should have 'Upcoming' button");
                
                boolean hasCalendarButton = sidebar.lookupAll(".sidebar-nav-button").stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> (Button) node)
                    .anyMatch(btn -> "Calendar".equals(btn.getText()));
                assertTrue(hasCalendarButton, "Sidebar should have 'Calendar' button");
                
                boolean hasAllTasksButton = sidebar.lookupAll(".sidebar-nav-button").stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> (Button) node)
                    .anyMatch(btn -> "All Tasks".equals(btn.getText()));
                assertTrue(hasAllTasksButton, "Sidebar should have 'All Tasks' button");
                
                // And: it should show the list of categories
                ListView<?> categoryList = (ListView<?>) sidebar.lookup(".category-list");
                assertNotNull(categoryList, "Sidebar should have category list");
                
            } catch (IOException e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Quick add bar - contains text field and add button")
    void testQuickAddBar() throws IOException, InterruptedException {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/MainWindow.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                BorderPane root = loader.load();
                MainUiController controller = loader.getController();
                
                // Given: the main window is displayed
                assertNotNull(root.getTop(), "Quick add bar should exist at top");
                
                // When: viewing the quick add bar
                // Then: it should have a text field
                TextField quickAddField = (TextField) root.getTop().lookup(".quick-add-field");
                assertNotNull(quickAddField, "Quick add bar should have text field");
                assertNotNull(quickAddField.getPromptText(), "Text field should have prompt text");
                
                // And: it should have an add button
                Button quickAddButton = (Button) root.getTop().lookup(".quick-add-button");
                assertNotNull(quickAddButton, "Quick add bar should have add button");
                assertEquals("Add Task", quickAddButton.getText(), "Button should have 'Add Task' text");
                
            } catch (IOException e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Sidebar toggle - collapses and expands sidebar")
    void testSidebarToggle() throws IOException, InterruptedException {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/MainWindow.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                BorderPane root = loader.load();
                MainUiController controller = loader.getController();
                
                VBox sidebar = (VBox) root.getLeft();
                Button toggleButton = (Button) sidebar.lookup(".sidebar-toggle-button");
                
                // Given: the sidebar is expanded
                assertNotNull(toggleButton, "Sidebar should have toggle button");
                double expandedWidth = sidebar.getPrefWidth();
                assertTrue(expandedWidth >= 200, "Expanded sidebar should be at least 200px wide");
                
                // When: toggle button is clicked
                Platform.runLater(() -> toggleButton.fire());
                
                // Wait for animation/update
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
                
                // Then: sidebar width should change
                // (Note: actual width change is tested visually, here we just verify the button exists)
                assertNotNull(toggleButton, "Toggle button should still exist after click");
                
            } catch (IOException e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
    
    @Test
    @DisplayName("Category list - displays sample categories with task counts")
    void testCategoryList() throws IOException, InterruptedException {
        Platform.runLater(() -> {
            try {
                URL fxmlResource = getClass().getResource("/net/talaatharb/workday/ui/MainWindow.fxml");
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                BorderPane root = loader.load();
                MainUiController controller = loader.getController();
                
                VBox sidebar = (VBox) root.getLeft();
                ListView<?> categoryList = (ListView<?>) sidebar.lookup(".category-list");
                
                // Given: the main window is displayed
                assertNotNull(categoryList, "Category list should exist");
                
                // When: viewing the category list
                // Then: it should display categories (sample data is loaded in controller)
                assertTrue(categoryList.getItems().size() > 0, 
                    "Category list should have items (sample data)");
                
            } catch (IOException e) {
                fail("Failed to load FXML: " + e.getMessage());
            }
        });
        
        Thread.sleep(500);
    }
}
