package net.talaatharb.workday.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.talaatharb.workday.dtos.TaskDTO;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

class TaskMapperTest {
    
    private final TaskMapper mapper = TaskMapper.INSTANCE;
    
    @Test
    void testToDTO() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        Task task = Task.builder()
            .id(id)
            .title("Test Task")
            .description("Description")
            .dueDate(LocalDate.of(2024, 12, 31))
            .dueTime(LocalTime.of(17, 30))
            .scheduledDate(LocalDate.of(2024, 12, 30))
            .priority(Priority.HIGH)
            .status(TaskStatus.TODO)
            .categoryId(categoryId)
            .tags(Arrays.asList("work", "urgent"))
            .createdAt(now)
            .updatedAt(now)
            .reminderMinutesBefore(15)
            .estimatedDuration(Duration.ofHours(2))
            .build();
        
        TaskDTO dto = mapper.toDTO(task);
        
        assertNotNull(dto);
        assertEquals(id, dto.getId());
        assertEquals("Test Task", dto.getTitle());
        assertEquals("Description", dto.getDescription());
        assertEquals(LocalDate.of(2024, 12, 31), dto.getDueDate());
        assertEquals(Priority.HIGH, dto.getPriority());
        assertEquals(TaskStatus.TODO, dto.getStatus());
        assertEquals(categoryId, dto.getCategoryId());
        assertEquals(2, dto.getTags().size());
        assertEquals(15, dto.getReminderMinutesBefore());
    }
    
    @Test
    void testToEntity() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        TaskDTO dto = TaskDTO.builder()
            .id(id)
            .title("DTO Task")
            .description("DTO Description")
            .dueDate(LocalDate.of(2025, 1, 15))
            .priority(Priority.MEDIUM)
            .status(TaskStatus.IN_PROGRESS)
            .tags(Arrays.asList("personal"))
            .createdAt(now)
            .build();
        
        Task task = mapper.toEntity(dto);
        
        assertNotNull(task);
        assertEquals(id, task.getId());
        assertEquals("DTO Task", task.getTitle());
        assertEquals("DTO Description", task.getDescription());
        assertEquals(Priority.MEDIUM, task.getPriority());
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
    }
    
    @Test
    void testUpdateEntityFromDTO() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        
        Task existingTask = Task.builder()
            .id(id)
            .title("Original")
            .createdAt(createdAt)
            .build();
        
        TaskDTO dto = TaskDTO.builder()
            .title("Updated")
            .description("New description")
            .priority(Priority.URGENT)
            .build();
        
        mapper.updateEntityFromDTO(dto, existingTask);
        
        assertEquals(id, existingTask.getId());
        assertEquals(createdAt, existingTask.getCreatedAt());
        assertEquals("Updated", existingTask.getTitle());
        assertEquals("New description", existingTask.getDescription());
        assertEquals(Priority.URGENT, existingTask.getPriority());
    }
    
    @Test
    void testNullHandling() {
        assertNull(mapper.toDTO(null));
        assertNull(mapper.toEntity(null));
    }
}
