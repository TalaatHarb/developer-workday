package net.talaatharb.workday.service;

import net.talaatharb.workday.model.QuickAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for QuickActionsService.
 */
class QuickActionsServiceTest {
    
    private QuickActionsService quickActionsService;
    
    @BeforeEach
    void setUp() {
        quickActionsService = new QuickActionsService();
    }
    
    @Test
    void testRegisterAction() {
        // Given
        QuickAction action = QuickAction.builder()
            .id("test-1")
            .title("Test Action")
            .description("This is a test action")
            .category("Test")
            .build();
        
        // When
        quickActionsService.registerAction(action);
        List<QuickAction> allActions = quickActionsService.getAllActions();
        
        // Then
        assertEquals(1, allActions.size());
        assertEquals("Test Action", allActions.get(0).getTitle());
    }
    
    @Test
    void testGetAllActions_SortedByUsageAndTitle() {
        // Given
        QuickAction action1 = QuickAction.builder()
            .id("test-1")
            .title("Z Action")
            .useCount(0)
            .build();
        
        QuickAction action2 = QuickAction.builder()
            .id("test-2")
            .title("A Action")
            .useCount(5)
            .build();
        
        QuickAction action3 = QuickAction.builder()
            .id("test-3")
            .title("B Action")
            .useCount(3)
            .build();
        
        quickActionsService.registerAction(action1);
        quickActionsService.registerAction(action2);
        quickActionsService.registerAction(action3);
        
        // When
        List<QuickAction> allActions = quickActionsService.getAllActions();
        
        // Then
        assertEquals(3, allActions.size());
        // Should be sorted by useCount descending, then by title
        assertEquals("A Action", allActions.get(0).getTitle());  // useCount=5
        assertEquals("B Action", allActions.get(1).getTitle());  // useCount=3
        assertEquals("Z Action", allActions.get(2).getTitle());  // useCount=0
    }
    
    @Test
    void testSearchActions_ByTitle() {
        // Given
        QuickAction action1 = QuickAction.builder()
            .id("test-1")
            .title("Create Task")
            .category("Tasks")
            .build();
        
        QuickAction action2 = QuickAction.builder()
            .id("test-2")
            .title("Open Settings")
            .category("Settings")
            .build();
        
        quickActionsService.registerAction(action1);
        quickActionsService.registerAction(action2);
        
        // When
        List<QuickAction> results = quickActionsService.searchActions("create");
        
        // Then
        assertEquals(1, results.size());
        assertEquals("Create Task", results.get(0).getTitle());
    }
    
    @Test
    void testSearchActions_ByCategory() {
        // Given
        QuickAction action1 = QuickAction.builder()
            .id("test-1")
            .title("Create Task")
            .category("Tasks")
            .build();
        
        QuickAction action2 = QuickAction.builder()
            .id("test-2")
            .title("Delete Task")
            .category("Tasks")
            .build();
        
        QuickAction action3 = QuickAction.builder()
            .id("test-3")
            .title("Open Settings")
            .category("Settings")
            .build();
        
        quickActionsService.registerAction(action1);
        quickActionsService.registerAction(action2);
        quickActionsService.registerAction(action3);
        
        // When
        List<QuickAction> results = quickActionsService.searchActions("tasks");
        
        // Then
        assertEquals(2, results.size());
    }
    
    @Test
    void testSearchActions_EmptyQuery() {
        // Given
        QuickAction action1 = QuickAction.builder()
            .id("test-1")
            .title("Action 1")
            .build();
        
        quickActionsService.registerAction(action1);
        
        // When
        List<QuickAction> results = quickActionsService.searchActions("");
        
        // Then
        assertEquals(1, results.size());
    }
    
    @Test
    void testExecuteAction_IncrementsUseCount() {
        // Given
        AtomicBoolean executed = new AtomicBoolean(false);
        QuickAction action = QuickAction.builder()
            .id("test-1")
            .title("Test Action")
            .useCount(0)
            .action(() -> executed.set(true))
            .build();
        
        quickActionsService.registerAction(action);
        
        // When
        quickActionsService.executeAction(action);
        
        // Then
        assertTrue(executed.get());
        assertEquals(1, action.getUseCount());
    }
    
    @Test
    void testGetRecentActions() {
        // Given
        QuickAction action1 = QuickAction.builder()
            .id("test-1")
            .title("Action 1")
            .useCount(5)
            .build();
        
        QuickAction action2 = QuickAction.builder()
            .id("test-2")
            .title("Action 2")
            .useCount(10)
            .build();
        
        QuickAction action3 = QuickAction.builder()
            .id("test-3")
            .title("Action 3")
            .useCount(0)  // Never used
            .build();
        
        quickActionsService.registerAction(action1);
        quickActionsService.registerAction(action2);
        quickActionsService.registerAction(action3);
        
        // When
        List<QuickAction> recentActions = quickActionsService.getRecentActions(2);
        
        // Then
        assertEquals(2, recentActions.size());
        assertEquals("Action 2", recentActions.get(0).getTitle());  // useCount=10
        assertEquals("Action 1", recentActions.get(1).getTitle());  // useCount=5
    }
}
