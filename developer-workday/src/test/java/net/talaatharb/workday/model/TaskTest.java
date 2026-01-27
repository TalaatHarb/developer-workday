package net.talaatharb.workday.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TaskTest {
    
    @Test
    void testTaskHasAllRequiredFields() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        Task task = Task.builder()
            .id(id)
            .title("Test Task")
            .description("Test Description")
            .dueDate(LocalDate.of(2024, 12, 31))
            .dueTime(LocalTime.of(17, 30))
            .scheduledDate(LocalDate.of(2024, 12, 30))
            .priority(Priority.HIGH)
            .status(TaskStatus.TODO)
            .categoryId(categoryId)
            .tags(Arrays.asList("work", "important"))
            .createdAt(now)
            .updatedAt(now)
            .completedAt(null)
            .recurrence(null)
            .reminderMinutesBefore(15)
            .estimatedDuration(Duration.ofHours(2))
            .actualDuration(Duration.ofMinutes(90))
            .build();
        
        assertEquals(id, task.getId());
        assertEquals(categoryId, task.getCategoryId());
        assertEquals("Test Task", task.getTitle());
        assertEquals("Test Description", task.getDescription());
        assertEquals(LocalDate.of(2024, 12, 31), task.getDueDate());
        assertEquals(LocalTime.of(17, 30), task.getDueTime());
        assertEquals(LocalDate.of(2024, 12, 30), task.getScheduledDate());
        assertEquals(Priority.HIGH, task.getPriority());
        assertEquals(TaskStatus.TODO, task.getStatus());
        assertEquals(2, task.getTags().size());
        assertTrue(task.getTags().contains("work"));
        assertTrue(task.getTags().contains("important"));
        assertEquals(now, task.getCreatedAt());
        assertEquals(now, task.getUpdatedAt());
        assertNull(task.getCompletedAt());
        assertNull(task.getRecurrence());
        assertEquals(15, task.getReminderMinutesBefore());
        assertEquals(Duration.ofHours(2), task.getEstimatedDuration());
        assertEquals(Duration.ofMinutes(90), task.getActualDuration());
    }
    
    @Test
    void testTaskIsSerializable() throws Exception {
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Serializable Task")
            .description("Testing serialization")
            .priority(Priority.MEDIUM)
            .status(TaskStatus.IN_PROGRESS)
            .tags(Arrays.asList("test"))
            .createdAt(LocalDateTime.now())
            .build();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(task);
        oos.close();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Task deserializedTask = (Task) ois.readObject();
        ois.close();
        
        assertEquals(task.getId(), deserializedTask.getId());
        assertEquals(task.getTitle(), deserializedTask.getTitle());
        assertEquals(task.getDescription(), deserializedTask.getDescription());
        assertEquals(task.getPriority(), deserializedTask.getPriority());
        assertEquals(task.getStatus(), deserializedTask.getStatus());
    }
    
    @Test
    void testPriorityEnum() {
        assertEquals(4, Priority.values().length);
        assertNotNull(Priority.valueOf("LOW"));
        assertNotNull(Priority.valueOf("MEDIUM"));
        assertNotNull(Priority.valueOf("HIGH"));
        assertNotNull(Priority.valueOf("URGENT"));
    }
    
    @Test
    void testTaskStatusEnum() {
        assertEquals(4, TaskStatus.values().length);
        assertNotNull(TaskStatus.valueOf("TODO"));
        assertNotNull(TaskStatus.valueOf("IN_PROGRESS"));
        assertNotNull(TaskStatus.valueOf("COMPLETED"));
        assertNotNull(TaskStatus.valueOf("CANCELLED"));
    }
    
    @Test
    void testTaskWithRecurrence() {
        RecurrenceRule recurrence = RecurrenceRule.builder()
            .type(RecurrenceRule.RecurrenceType.DAILY)
            .interval(1)
            .endDate(LocalDate.of(2025, 12, 31))
            .build();
        
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Recurring Task")
            .recurrence(recurrence)
            .build();
        
        assertNotNull(task.getRecurrence());
        assertEquals(RecurrenceRule.RecurrenceType.DAILY, task.getRecurrence().getType());
        assertEquals(1, task.getRecurrence().getInterval());
    }
    
    @Test
    void testTaskBuilder() {
        Task task = Task.builder()
            .id(UUID.randomUUID())
            .title("Builder Test")
            .build();
        
        assertNotNull(task);
        assertNotNull(task.getId());
        assertEquals("Builder Test", task.getTitle());
        assertNotNull(task.getTags());
        assertTrue(task.getTags().isEmpty());
    }
    
    @Test
    void testTaskDefaultValues() {
        Task task = new Task();
        assertNull(task.getId());
        assertNull(task.getTitle());
        assertNotNull(task.getTags());
    }
}
