package net.talaatharb.workday.event.category;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.EventLogger;
import net.talaatharb.workday.model.Category;

/**
 * Tests for category-related events following the acceptance criteria.
 */
class CategoryEventsTest {
    
    private EventDispatcher dispatcher;
    private EventLogger logger;
    
    @BeforeEach
    void setUp() {
        logger = new EventLogger();
        dispatcher = new EventDispatcher(logger);
    }
    
    @Test
    @DisplayName("CategoryCreatedEvent is published when a category is created")
    void testCategoryCreatedEvent_Published() throws InterruptedException {
        // Given: a user creates a new category
        CountDownLatch latch = new CountDownLatch(1);
        List<CategoryCreatedEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(CategoryCreatedEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .name("Work")
            .description("Work-related tasks")
            .color("#FF5733")
            .icon("briefcase")
            .isDefault(false)
            .createdAt(LocalDateTime.now())
            .build();
        
        // When: the category is saved successfully
        CategoryCreatedEvent event = new CategoryCreatedEvent(category);
        dispatcher.publish(event);
        
        // Then: a CategoryCreatedEvent should be published
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Event should be published");
        assertEquals(1, receivedEvents.size(), "Should receive one event");
        
        // And: the event should contain the complete category data
        CategoryCreatedEvent receivedEvent = receivedEvents.get(0);
        assertNotNull(receivedEvent.getCategory(), "Event should contain category");
        assertEquals(category.getId(), receivedEvent.getCategory().getId(), "Category ID should match");
        assertEquals(category.getName(), receivedEvent.getCategory().getName(), "Category name should match");
        assertEquals(category.getDescription(), receivedEvent.getCategory().getDescription(), "Category description should match");
        assertEquals(category.getColor(), receivedEvent.getCategory().getColor(), "Category color should match");
        assertEquals(category.getIcon(), receivedEvent.getCategory().getIcon(), "Category icon should match");
    }
    
    @Test
    @DisplayName("CategoryDeletedEvent handles task reassignment")
    void testCategoryDeletedEvent_HandlesTaskReassignment() throws InterruptedException {
        // Given: a category with tasks is deleted
        CountDownLatch latch = new CountDownLatch(1);
        List<CategoryDeletedEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(CategoryDeletedEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .name("Old Category")
            .build();
        
        List<UUID> affectedTaskIds = Arrays.asList(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        // When: the CategoryDeletedEvent is published
        CategoryDeletedEvent event = new CategoryDeletedEvent(category, affectedTaskIds);
        dispatcher.publish(event);
        
        // Then: it should contain information about affected tasks
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Event should be published");
        assertEquals(1, receivedEvents.size(), "Should receive one event");
        
        CategoryDeletedEvent receivedEvent = receivedEvents.get(0);
        assertNotNull(receivedEvent.getCategory(), "Event should contain category");
        assertNotNull(receivedEvent.getAffectedTaskIds(), "Event should contain affected task IDs");
        assertEquals(3, receivedEvent.getAffectedTaskIds().size(), "Should have 3 affected tasks");
        assertEquals(affectedTaskIds, receivedEvent.getAffectedTaskIds(), "Affected task IDs should match");
        
        // And: listeners should handle task reassignment to default category
        // (This would be implemented in the actual service layer)
        assertEquals(category.getId(), receivedEvent.getCategory().getId(), "Category ID should match");
    }
    
    @Test
    @DisplayName("CategoryUpdatedEvent contains before and after states")
    void testCategoryUpdatedEvent_ContainsOldAndNewStates() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<CategoryUpdatedEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(CategoryUpdatedEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        Category oldCategory = Category.builder()
            .id(UUID.randomUUID())
            .name("Old Name")
            .description("Old description")
            .color("#FF0000")
            .build();
        
        Category newCategory = Category.builder()
            .id(oldCategory.getId())
            .name("New Name")
            .description("New description")
            .color("#00FF00")
            .updatedAt(LocalDateTime.now())
            .build();
        
        CategoryUpdatedEvent event = new CategoryUpdatedEvent(oldCategory, newCategory);
        dispatcher.publish(event);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, receivedEvents.size());
        
        CategoryUpdatedEvent receivedEvent = receivedEvents.get(0);
        assertNotNull(receivedEvent.getOldCategory());
        assertNotNull(receivedEvent.getNewCategory());
        assertEquals("Old Name", receivedEvent.getOldCategory().getName());
        assertEquals("New Name", receivedEvent.getNewCategory().getName());
    }
    
    @Test
    @DisplayName("CategoryReorderedEvent is published when categories are reordered")
    void testCategoryReorderedEvent_Published() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<CategoryReorderedEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(CategoryReorderedEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        List<UUID> categoryIds = Arrays.asList(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        CategoryReorderedEvent event = new CategoryReorderedEvent(categoryIds);
        dispatcher.publish(event);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, receivedEvents.size());
        
        CategoryReorderedEvent receivedEvent = receivedEvents.get(0);
        assertEquals(3, receivedEvent.getCategoryIds().size());
        assertEquals(categoryIds, receivedEvent.getCategoryIds());
    }
    
    @Test
    @DisplayName("All category events are properly logged")
    void testAllCategoryEvents_AreLogged() {
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .name("Test Category")
            .build();
        
        List<UUID> taskIds = Arrays.asList(UUID.randomUUID());
        List<UUID> categoryIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        
        // Publish various events
        dispatcher.publish(new CategoryCreatedEvent(category));
        dispatcher.publish(new CategoryUpdatedEvent(category, category));
        dispatcher.publish(new CategoryDeletedEvent(category, taskIds));
        dispatcher.publish(new CategoryReorderedEvent(categoryIds));
        
        // Verify all events are logged
        assertEquals(4, logger.getEventCount(), "All 4 events should be logged");
    }
    
    @Test
    @DisplayName("CategoryDeletedEvent with empty task list")
    void testCategoryDeletedEvent_EmptyTaskList() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<CategoryDeletedEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(CategoryDeletedEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .name("Empty Category")
            .build();
        
        CategoryDeletedEvent event = new CategoryDeletedEvent(category, new ArrayList<>());
        dispatcher.publish(event);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, receivedEvents.size());
        
        CategoryDeletedEvent receivedEvent = receivedEvents.get(0);
        assertEquals(0, receivedEvent.getAffectedTaskIds().size());
    }
}
