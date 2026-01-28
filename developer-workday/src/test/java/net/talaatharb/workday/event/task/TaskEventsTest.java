package net.talaatharb.workday.event.task;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.EventLogger;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

/**
 * Tests for task-related events following the acceptance criteria.
 */
class TaskEventsTest {
    
    private EventDispatcher dispatcher;
    private EventLogger logger;
    
    @BeforeEach
    void setUp() {
        logger = new EventLogger();
        dispatcher = new EventDispatcher(logger);
    }
    
    @Test
    @DisplayName("TaskCreatedEvent is published when a task is created")
    void testTaskCreatedEvent_Published() throws InterruptedException {
        // Given: a user creates a new task
        CountDownLatch latch = new CountDownLatch(1);
        List<TaskCreatedEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(TaskCreatedEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("New Task")
            .description("Task description")
            .priority(Priority.MEDIUM)
            .status(TaskStatus.TODO)
            .createdAt(LocalDateTime.now())
            .build();
        
        // When: the task is saved successfully
        TaskCreatedEvent event = new TaskCreatedEvent(task);
        dispatcher.publish(event);
        
        // Then: a TaskCreatedEvent should be published
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Event should be published");
        assertEquals(1, receivedEvents.size(), "Should receive one event");
        
        // And: the event should contain the complete task data
        TaskCreatedEvent receivedEvent = receivedEvents.get(0);
        assertNotNull(receivedEvent.getTask(), "Event should contain task");
        assertEquals(task.getId(), receivedEvent.getTask().getId(), "Task ID should match");
        assertEquals(task.getTitle(), receivedEvent.getTask().getTitle(), "Task title should match");
        assertEquals(task.getDescription(), receivedEvent.getTask().getDescription(), "Task description should match");
        assertEquals(task.getPriority(), receivedEvent.getTask().getPriority(), "Task priority should match");
        assertEquals(task.getStatus(), receivedEvent.getTask().getStatus(), "Task status should match");
    }
    
    @Test
    @DisplayName("TaskCompletedEvent is published when a task is marked complete")
    void testTaskCompletedEvent_Published() throws InterruptedException {
        // Given: a user marks a task as complete
        CountDownLatch latch = new CountDownLatch(1);
        List<TaskCompletedEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(TaskCompletedEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        UUID taskId = UUID.randomUUID();
        LocalDateTime completionTime = LocalDateTime.now();
        
        // When: the task status is updated
        TaskCompletedEvent event = new TaskCompletedEvent(taskId, completionTime);
        dispatcher.publish(event);
        
        // Then: a TaskCompletedEvent should be published
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Event should be published");
        assertEquals(1, receivedEvents.size(), "Should receive one event");
        
        // And: the event should contain the task id and completion timestamp
        TaskCompletedEvent receivedEvent = receivedEvents.get(0);
        assertEquals(taskId, receivedEvent.getTaskId(), "Task ID should match");
        assertEquals(completionTime, receivedEvent.getCompletionTimestamp(), "Completion timestamp should match");
    }
    
    @Test
    @DisplayName("TaskUpdatedEvent contains before and after states")
    void testTaskUpdatedEvent_ContainsOldAndNewStates() throws InterruptedException {
        // Given: a user edits a task
        CountDownLatch latch = new CountDownLatch(1);
        List<TaskUpdatedEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(TaskUpdatedEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        Task oldTask = Task.builder()
            .id(UUID.randomUUID())
            .title("Old Title")
            .description("Old description")
            .priority(Priority.LOW)
            .status(TaskStatus.TODO)
            .createdAt(LocalDateTime.now())
            .build();
        
        Task newTask = Task.builder()
            .id(oldTask.getId())
            .title("New Title")
            .description("New description")
            .priority(Priority.HIGH)
            .status(TaskStatus.IN_PROGRESS)
            .createdAt(oldTask.getCreatedAt())
            .updatedAt(LocalDateTime.now())
            .build();
        
        // When: the task is updated
        TaskUpdatedEvent event = new TaskUpdatedEvent(oldTask, newTask);
        dispatcher.publish(event);
        
        // Then: a TaskUpdatedEvent should be published
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Event should be published");
        assertEquals(1, receivedEvents.size(), "Should receive one event");
        
        // And: the event should contain both old and new task states
        TaskUpdatedEvent receivedEvent = receivedEvents.get(0);
        assertNotNull(receivedEvent.getOldTask(), "Event should contain old task");
        assertNotNull(receivedEvent.getNewTask(), "Event should contain new task");
        
        assertEquals("Old Title", receivedEvent.getOldTask().getTitle(), "Old task title should match");
        assertEquals("New Title", receivedEvent.getNewTask().getTitle(), "New task title should match");
        assertEquals(Priority.LOW, receivedEvent.getOldTask().getPriority(), "Old priority should match");
        assertEquals(Priority.HIGH, receivedEvent.getNewTask().getPriority(), "New priority should match");
    }
    
    @Test
    @DisplayName("TaskDeletedEvent is published when a task is deleted")
    void testTaskDeletedEvent_Published() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<TaskDeletedEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(TaskDeletedEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Task to Delete")
            .build();
        
        TaskDeletedEvent event = new TaskDeletedEvent(task);
        dispatcher.publish(event);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, receivedEvents.size());
        assertEquals(task.getId(), receivedEvents.get(0).getTask().getId());
    }
    
    @Test
    @DisplayName("TaskScheduledEvent is published when a task is scheduled")
    void testTaskScheduledEvent_Published() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<TaskScheduledEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(TaskScheduledEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        UUID taskId = UUID.randomUUID();
        LocalDate scheduledDate = LocalDate.now().plusDays(1);
        
        TaskScheduledEvent event = new TaskScheduledEvent(taskId, scheduledDate);
        dispatcher.publish(event);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, receivedEvents.size());
        assertEquals(taskId, receivedEvents.get(0).getTaskId());
        assertEquals(scheduledDate, receivedEvents.get(0).getScheduledDate());
    }
    
    @Test
    @DisplayName("TaskPriorityChangedEvent is published when priority changes")
    void testTaskPriorityChangedEvent_Published() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<TaskPriorityChangedEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(TaskPriorityChangedEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        UUID taskId = UUID.randomUUID();
        Priority oldPriority = Priority.LOW;
        Priority newPriority = Priority.HIGH;
        
        TaskPriorityChangedEvent event = new TaskPriorityChangedEvent(taskId, oldPriority, newPriority);
        dispatcher.publish(event);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, receivedEvents.size());
        TaskPriorityChangedEvent receivedEvent = receivedEvents.get(0);
        assertEquals(taskId, receivedEvent.getTaskId());
        assertEquals(oldPriority, receivedEvent.getOldPriority());
        assertEquals(newPriority, receivedEvent.getNewPriority());
    }
    
    @Test
    @DisplayName("TaskMovedToCategoryEvent is published when task moves to different category")
    void testTaskMovedToCategoryEvent_Published() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<TaskMovedToCategoryEvent> receivedEvents = new ArrayList<>();
        
        dispatcher.subscribe(TaskMovedToCategoryEvent.class, event -> {
            receivedEvents.add(event);
            latch.countDown();
        });
        
        UUID taskId = UUID.randomUUID();
        UUID oldCategoryId = UUID.randomUUID();
        UUID newCategoryId = UUID.randomUUID();
        
        TaskMovedToCategoryEvent event = new TaskMovedToCategoryEvent(taskId, oldCategoryId, newCategoryId);
        dispatcher.publish(event);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, receivedEvents.size());
        TaskMovedToCategoryEvent receivedEvent = receivedEvents.get(0);
        assertEquals(taskId, receivedEvent.getTaskId());
        assertEquals(oldCategoryId, receivedEvent.getOldCategoryId());
        assertEquals(newCategoryId, receivedEvent.getNewCategoryId());
    }
    
    @Test
    @DisplayName("All task events are properly logged")
    void testAllTaskEvents_AreLogged() {
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Test Task")
            .build();
        
        // Publish various events
        dispatcher.publish(new TaskCreatedEvent(task));
        dispatcher.publish(new TaskUpdatedEvent(task, task));
        dispatcher.publish(new TaskDeletedEvent(task));
        dispatcher.publish(new TaskCompletedEvent(task.getId(), LocalDateTime.now()));
        dispatcher.publish(new TaskScheduledEvent(task.getId(), LocalDate.now()));
        dispatcher.publish(new TaskPriorityChangedEvent(task.getId(), Priority.LOW, Priority.HIGH));
        dispatcher.publish(new TaskMovedToCategoryEvent(task.getId(), UUID.randomUUID(), UUID.randomUUID()));
        
        // Verify all events are logged
        assertEquals(7, logger.getEventCount(), "All 7 events should be logged");
    }
}
