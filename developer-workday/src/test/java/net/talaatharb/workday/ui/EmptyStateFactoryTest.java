package net.talaatharb.workday.ui;

import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EmptyStateFactory.
 */
class EmptyStateFactoryTest extends ApplicationTest {

    @Test
    void testCreateEmptyState() {
        // Given
        boolean[] actionCalled = {false};
        
        // When
        VBox emptyState = EmptyStateFactory.createEmptyState(
            "Test Title",
            "Test Message",
            "📝",
            "Test Action",
            () -> actionCalled[0] = true
        );
        
        // Then
        assertNotNull(emptyState);
        assertTrue(emptyState.getChildren().size() > 0);
    }

    @Test
    void testCreateEmptyTaskList() {
        // Given
        boolean[] actionCalled = {false};
        
        // When
        VBox emptyState = EmptyStateFactory.createEmptyTaskList(() -> actionCalled[0] = true);
        
        // Then
        assertNotNull(emptyState);
        assertTrue(emptyState.getChildren().size() > 0);
    }

    @Test
    void testCreateEmptyCategoryState() {
        // Given
        String categoryName = "Work";
        boolean[] actionCalled = {false};
        
        // When
        VBox emptyState = EmptyStateFactory.createEmptyCategoryState(
            categoryName,
            () -> actionCalled[0] = true
        );
        
        // Then
        assertNotNull(emptyState);
        assertTrue(emptyState.getChildren().size() > 0);
    }

    @Test
    void testCreateEmptySearchResults() {
        // When
        VBox emptyState = EmptyStateFactory.createEmptySearchResults("test query");
        
        // Then
        assertNotNull(emptyState);
        assertTrue(emptyState.getChildren().size() > 0);
    }

    @Test
    void testCreateEmptyTodayView() {
        // Given
        boolean[] actionCalled = {false};
        
        // When
        VBox emptyState = EmptyStateFactory.createEmptyTodayView(() -> actionCalled[0] = true);
        
        // Then
        assertNotNull(emptyState);
        assertTrue(emptyState.getChildren().size() > 0);
    }

    @Test
    void testCreateEmptyUpcomingView() {
        // Given
        boolean[] actionCalled = {false};
        
        // When
        VBox emptyState = EmptyStateFactory.createEmptyUpcomingView(() -> actionCalled[0] = true);
        
        // Then
        assertNotNull(emptyState);
        assertTrue(emptyState.getChildren().size() > 0);
    }

    @Test
    void testCreateEmptyCompletedView() {
        // When
        VBox emptyState = EmptyStateFactory.createEmptyCompletedView();
        
        // Then
        assertNotNull(emptyState);
        assertTrue(emptyState.getChildren().size() > 0);
    }

    @Test
    void testCreateEmptyCategoryList() {
        // Given
        boolean[] actionCalled = {false};
        
        // When
        VBox emptyState = EmptyStateFactory.createEmptyCategoryList(() -> actionCalled[0] = true);
        
        // Then
        assertNotNull(emptyState);
        assertTrue(emptyState.getChildren().size() > 0);
    }
}
