package net.talaatharb.workday.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import net.talaatharb.workday.event.Event;
import net.talaatharb.workday.event.task.TaskCreatedEvent;
import net.talaatharb.workday.event.task.TaskDeletedEvent;
import net.talaatharb.workday.event.task.TaskUpdatedEvent;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

/**
 * Tests for EventStoreRepository following the acceptance criteria.
 */
class EventStoreRepositoryTest {
    
    private DB database;
    private EventStoreRepository repository;
    private File dbFile;
    
    @BeforeEach
    void setUp() {
        dbFile = new File("test-eventstore-" + UUID.randomUUID() + ".db");
        database = DBMaker.fileDB(dbFile)
            .transactionEnable()
            .make();
        repository = new EventStoreRepository(database);
    }
    
    @AfterEach
    void tearDown() {
        if (database != null && !database.isClosed()) {
            database.close();
        }
        if (dbFile != null && dbFile.exists()) {
            dbFile.delete();
        }
    }
    
    @Test
    @DisplayName("Persist application events with timestamp")
    void testPersistEvents_WithTimestamp() {
        // Given: any application event
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Test Task")
            .priority(Priority.MEDIUM)
            .status(TaskStatus.TODO)
            .build();
        
        TaskCreatedEvent event = new TaskCreatedEvent(task);
        
        // When: the event is published (saved to event store)
        Event savedEvent = repository.save(event);
        
        // Then: it should be saved to the event store with timestamp
        assertNotNull(savedEvent);
        assertNotNull(savedEvent.getTimestamp());
        assertNotNull(savedEvent.getEventId());
        assertEquals("TaskCreatedEvent", savedEvent.getEventType());
        
        // And: it should be retrievable for audit purposes
        assertTrue(repository.existsById(savedEvent.getEventId()));
        Event retrievedEvent = repository.findById(savedEvent.getEventId()).orElse(null);
        assertNotNull(retrievedEvent);
        assertEquals(savedEvent.getEventId(), retrievedEvent.getEventId());
        assertEquals(savedEvent.getTimestamp(), retrievedEvent.getTimestamp());
    }
    
    @Test
    @DisplayName("Query events by type and date range in chronological order")
    void testQueryEventsByTypeAndDateRange_ReturnsInChronologicalOrder() throws InterruptedException {
        // Given: stored events of various types
        Task task1 = Task.builder().id(UUID.randomUUID()).title("Task 1").build();
        Task task2 = Task.builder().id(UUID.randomUUID()).title("Task 2").build();
        Task task3 = Task.builder().id(UUID.randomUUID()).title("Task 3").build();
        
        // Save events with small delays to ensure different timestamps
        LocalDateTime startTime = LocalDateTime.now();
        
        TaskCreatedEvent event1 = new TaskCreatedEvent(task1);
        repository.save(event1);
        Thread.sleep(10);
        
        TaskUpdatedEvent event2 = new TaskUpdatedEvent(task2, task2);
        repository.save(event2);
        Thread.sleep(10);
        
        TaskCreatedEvent event3 = new TaskCreatedEvent(task3);
        repository.save(event3);
        Thread.sleep(10);
        
        TaskDeletedEvent event4 = new TaskDeletedEvent(task1);
        repository.save(event4);
        
        LocalDateTime endTime = LocalDateTime.now();
        
        // When: querying by event type and date range
        List<Event> createdEvents = repository.findByEventTypeAndTimestampBetween(
            "TaskCreatedEvent", startTime.minusSeconds(1), endTime.plusSeconds(1));
        
        // Then: matching events should be returned in chronological order
        assertEquals(2, createdEvents.size(), "Should find 2 TaskCreatedEvent events");
        assertTrue(createdEvents.get(0).getTimestamp().isBefore(createdEvents.get(1).getTimestamp()),
            "Events should be in chronological order");
        assertEquals("TaskCreatedEvent", createdEvents.get(0).getEventType());
        assertEquals("TaskCreatedEvent", createdEvents.get(1).getEventType());
    }
    
    @Test
    @DisplayName("Find all events returns all stored events")
    void testFindAll_ReturnsAllEvents() {
        Task task = Task.builder().id(UUID.randomUUID()).title("Task").build();
        
        repository.save(new TaskCreatedEvent(task));
        repository.save(new TaskUpdatedEvent(task, task));
        repository.save(new TaskDeletedEvent(task));
        
        List<Event> allEvents = repository.findAll();
        assertEquals(3, allEvents.size());
    }
    
    @Test
    @DisplayName("Find events by type returns only matching type")
    void testFindByEventType_ReturnsMatchingType() throws InterruptedException {
        Task task1 = Task.builder().id(UUID.randomUUID()).title("Task 1").build();
        Task task2 = Task.builder().id(UUID.randomUUID()).title("Task 2").build();
        
        repository.save(new TaskCreatedEvent(task1));
        Thread.sleep(10);
        repository.save(new TaskCreatedEvent(task2));
        Thread.sleep(10);
        repository.save(new TaskUpdatedEvent(task1, task1));
        Thread.sleep(10);
        repository.save(new TaskDeletedEvent(task2));
        
        List<Event> createdEvents = repository.findByEventType("TaskCreatedEvent");
        assertEquals(2, createdEvents.size());
        assertTrue(createdEvents.stream().allMatch(e -> e.getEventType().equals("TaskCreatedEvent")));
        
        // Verify chronological order
        assertTrue(createdEvents.get(0).getTimestamp().isBefore(createdEvents.get(1).getTimestamp()));
    }
    
    @Test
    @DisplayName("Find events by timestamp range")
    void testFindByTimestampBetween() throws InterruptedException {
        Task task = Task.builder().id(UUID.randomUUID()).title("Task").build();
        
        LocalDateTime start = LocalDateTime.now();
        Thread.sleep(10);
        
        repository.save(new TaskCreatedEvent(task));
        Thread.sleep(10);
        repository.save(new TaskUpdatedEvent(task, task));
        Thread.sleep(10);
        
        LocalDateTime end = LocalDateTime.now();
        Thread.sleep(10);
        
        repository.save(new TaskDeletedEvent(task));
        
        List<Event> eventsInRange = repository.findByTimestampBetween(start, end);
        assertEquals(2, eventsInRange.size());
    }
    
    @Test
    @DisplayName("Find events after timestamp")
    void testFindAfterTimestamp() throws InterruptedException {
        Task task = Task.builder().id(UUID.randomUUID()).title("Task").build();
        
        repository.save(new TaskCreatedEvent(task));
        Thread.sleep(10);
        
        LocalDateTime midpoint = LocalDateTime.now();
        Thread.sleep(10);
        
        repository.save(new TaskUpdatedEvent(task, task));
        Thread.sleep(10);
        repository.save(new TaskDeletedEvent(task));
        
        List<Event> eventsAfter = repository.findAfterTimestamp(midpoint);
        assertEquals(2, eventsAfter.size());
    }
    
    @Test
    @DisplayName("Find events before timestamp")
    void testFindBeforeTimestamp() throws InterruptedException {
        Task task = Task.builder().id(UUID.randomUUID()).title("Task").build();
        
        repository.save(new TaskCreatedEvent(task));
        Thread.sleep(10);
        repository.save(new TaskUpdatedEvent(task, task));
        Thread.sleep(10);
        
        LocalDateTime midpoint = LocalDateTime.now();
        Thread.sleep(10);
        
        repository.save(new TaskDeletedEvent(task));
        
        List<Event> eventsBefore = repository.findBeforeTimestamp(midpoint);
        assertEquals(2, eventsBefore.size());
    }
    
    @Test
    @DisplayName("Count events by type")
    void testCountByEventType() {
        Task task = Task.builder().id(UUID.randomUUID()).title("Task").build();
        
        repository.save(new TaskCreatedEvent(task));
        repository.save(new TaskCreatedEvent(task));
        repository.save(new TaskUpdatedEvent(task, task));
        
        assertEquals(2, repository.countByEventType("TaskCreatedEvent"));
        assertEquals(1, repository.countByEventType("TaskUpdatedEvent"));
        assertEquals(0, repository.countByEventType("TaskDeletedEvent"));
    }
    
    @Test
    @DisplayName("Count all events")
    void testCount() {
        Task task = Task.builder().id(UUID.randomUUID()).title("Task").build();
        
        assertEquals(0, repository.count());
        
        repository.save(new TaskCreatedEvent(task));
        assertEquals(1, repository.count());
        
        repository.save(new TaskUpdatedEvent(task, task));
        assertEquals(2, repository.count());
    }
    
    @Test
    @DisplayName("Delete all events")
    void testDeleteAll() {
        Task task = Task.builder().id(UUID.randomUUID()).title("Task").build();
        
        repository.save(new TaskCreatedEvent(task));
        repository.save(new TaskUpdatedEvent(task, task));
        assertEquals(2, repository.count());
        
        repository.deleteAll();
        assertEquals(0, repository.count());
    }
}
