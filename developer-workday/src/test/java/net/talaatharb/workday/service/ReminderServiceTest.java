package net.talaatharb.workday.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.EventLogger;
import net.talaatharb.workday.event.reminder.ReminderTriggeredEvent;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

/**
 * Tests for ReminderService following the acceptance criteria.
 */
class ReminderServiceTest {
    
    private EventDispatcher eventDispatcher;
    private EventLogger eventLogger;
    private ReminderService reminderService;
    
    @BeforeEach
    void setUp() {
        eventLogger = new EventLogger();
        eventDispatcher = new EventDispatcher(eventLogger);
        reminderService = new ReminderService(eventDispatcher);
    }
    
    @AfterEach
    void tearDown() {
        if (reminderService != null) {
            reminderService.shutdown();
        }
    }
    
    @Test
    @DisplayName("Schedule a reminder for a task with due date and reminder time")
    void testScheduleReminder() throws InterruptedException {
        // Given: a task with a due date and reminder minutes before set
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime dueTime = LocalTime.of(14, 0);
        
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Important Task")
            .status(TaskStatus.TODO)
            .priority(Priority.HIGH)
            .dueDate(tomorrow)
            .dueTime(dueTime)
            .reminderMinutesBefore(60) // 1 hour before
            .build();
        
        // When: the task is created or updated
        reminderService.scheduleReminder(task);
        
        // Then: a reminder should be scheduled for the specified time before due
        assertTrue(reminderService.hasScheduledReminder(task.getId()), 
            "Reminder should be scheduled");
        assertEquals(1, reminderService.getScheduledReminderCount(), 
            "Should have 1 scheduled reminder");
    }
    
    @Test
    @DisplayName("Trigger reminder notification when time is reached")
    void testTriggerReminder() throws InterruptedException {
        // Given: a scheduled reminder
        CountDownLatch latch = new CountDownLatch(1);
        final ReminderTriggeredEvent[] capturedEvent = new ReminderTriggeredEvent[1];
        
        eventDispatcher.subscribe(ReminderTriggeredEvent.class, event -> {
            capturedEvent[0] = event;
            latch.countDown();
        });
        
        // Create a task with reminder in 2 seconds
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime reminderTime = now.plusSeconds(2).toLocalTime();
        
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Reminder Test Task")
            .status(TaskStatus.TODO)
            .dueDate(today)
            .dueTime(reminderTime.plusSeconds(120)) // Due 2 minutes after reminder
            .reminderMinutesBefore(2) // 2 minutes before (will trigger in ~2 seconds)
            .build();
        
        reminderService.scheduleReminder(task);
        
        // When: the reminder time is reached (wait for trigger)
        boolean triggered = latch.await(5, TimeUnit.SECONDS);
        
        // Then: a ReminderTriggeredEvent should be published
        assertTrue(triggered, "Reminder should be triggered within timeout");
        assertNotNull(capturedEvent[0], "Event should be captured");
        assertEquals(task.getId(), capturedEvent[0].getTaskId());
        assertEquals(task.getTitle(), capturedEvent[0].getTaskTitle());
        
        // And: reminder should no longer be scheduled
        assertFalse(reminderService.hasScheduledReminder(task.getId()), 
            "Reminder should not be scheduled after triggering");
    }
    
    @Test
    @DisplayName("Cancel reminder when task is completed")
    void testCancelReminderOnCompletion() throws InterruptedException {
        // Given: a task with a scheduled reminder
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Task to Complete")
            .status(TaskStatus.TODO)
            .dueDate(tomorrow)
            .dueTime(LocalTime.of(10, 0))
            .reminderMinutesBefore(30)
            .build();
        
        reminderService.scheduleReminder(task);
        assertTrue(reminderService.hasScheduledReminder(task.getId()), 
            "Reminder should be scheduled initially");
        
        // When: the task is completed before the reminder time
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        reminderService.cancelReminder(task.getId());
        
        // Then: the scheduled reminder should be cancelled
        assertFalse(reminderService.hasScheduledReminder(task.getId()), 
            "Reminder should be cancelled");
        assertEquals(0, reminderService.getScheduledReminderCount(), 
            "Should have 0 scheduled reminders");
    }
    
    @Test
    @DisplayName("Update reminder when task is modified")
    void testUpdateReminder() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Task")
            .status(TaskStatus.TODO)
            .dueDate(tomorrow)
            .dueTime(LocalTime.of(15, 0))
            .reminderMinutesBefore(60)
            .build();
        
        reminderService.scheduleReminder(task);
        assertTrue(reminderService.hasScheduledReminder(task.getId()));
        
        // Update task with new reminder time
        task.setReminderMinutesBefore(120);
        reminderService.updateReminder(task);
        
        assertTrue(reminderService.hasScheduledReminder(task.getId()), 
            "Updated reminder should be scheduled");
    }
    
    @Test
    @DisplayName("Do not schedule reminder for task without due date")
    void testNoReminderWithoutDueDate() {
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Task without due date")
            .status(TaskStatus.TODO)
            .reminderMinutesBefore(60)
            .build();
        
        reminderService.scheduleReminder(task);
        
        assertFalse(reminderService.hasScheduledReminder(task.getId()), 
            "Should not schedule reminder without due date");
    }
    
    @Test
    @DisplayName("Do not schedule reminder for task without reminder configuration")
    void testNoReminderWithoutConfiguration() {
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Task without reminder config")
            .status(TaskStatus.TODO)
            .dueDate(LocalDate.now().plusDays(1))
            .dueTime(LocalTime.of(10, 0))
            .build();
        
        reminderService.scheduleReminder(task);
        
        assertFalse(reminderService.hasScheduledReminder(task.getId()), 
            "Should not schedule reminder without reminder configuration");
    }
    
    @Test
    @DisplayName("Do not schedule reminder for past time")
    void testNoReminderForPastTime() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Past task")
            .status(TaskStatus.TODO)
            .dueDate(yesterday)
            .dueTime(LocalTime.of(10, 0))
            .reminderMinutesBefore(60)
            .build();
        
        reminderService.scheduleReminder(task);
        
        assertFalse(reminderService.hasScheduledReminder(task.getId()), 
            "Should not schedule reminder for past time");
    }
    
    @Test
    @DisplayName("Cancel existing reminder when rescheduling")
    void testCancelExistingWhenRescheduling() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Task")
            .status(TaskStatus.TODO)
            .dueDate(tomorrow)
            .dueTime(LocalTime.of(10, 0))
            .reminderMinutesBefore(60)
            .build();
        
        reminderService.scheduleReminder(task);
        assertTrue(reminderService.hasScheduledReminder(task.getId()));
        
        // Schedule again (should cancel old and create new)
        task.setReminderMinutesBefore(30);
        reminderService.scheduleReminder(task);
        
        assertTrue(reminderService.hasScheduledReminder(task.getId()));
        assertEquals(1, reminderService.getScheduledReminderCount(), 
            "Should only have 1 reminder");
    }
    
    @Test
    @DisplayName("updateReminder cancels reminder for completed task")
    void testUpdateReminderForCompletedTask() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Task")
            .status(TaskStatus.TODO)
            .dueDate(tomorrow)
            .dueTime(LocalTime.of(10, 0))
            .reminderMinutesBefore(60)
            .build();
        
        reminderService.scheduleReminder(task);
        assertTrue(reminderService.hasScheduledReminder(task.getId()));
        
        // Mark as completed and update
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        reminderService.updateReminder(task);
        
        assertFalse(reminderService.hasScheduledReminder(task.getId()), 
            "Reminder should be cancelled for completed task");
    }
}
