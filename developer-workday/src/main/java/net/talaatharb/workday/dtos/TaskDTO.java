package net.talaatharb.workday.dtos;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.RecurrenceRule;
import net.talaatharb.workday.model.TaskStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private UUID id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private LocalDate scheduledDate;
    private Priority priority;
    private TaskStatus status;
    private UUID categoryId;
    private List<String> tags;
    private List<SubtaskDTO> subtasks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private RecurrenceRule recurrence;
    private Integer reminderMinutesBefore;
    private Duration estimatedDuration;
    private Duration actualDuration;
}
