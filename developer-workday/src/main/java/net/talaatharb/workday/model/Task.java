package net.talaatharb.workday.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Task entity representing a user task with all associated metadata.
 * Implements Serializable for MapDB storage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private String title;
    private String description;
    
    // Date and time fields
    private LocalDate dueDate;
    private LocalTime dueTime;
    private LocalDate scheduledDate;
    
    // Classification
    private Priority priority;
    private TaskStatus status;
    private UUID categoryId;
    
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    // Subtasks/Checklist
    @Builder.Default
    private List<Subtask> subtasks = new ArrayList<>();
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    
    // Recurrence and reminders
    private RecurrenceRule recurrence;
    private Integer reminderMinutesBefore;
    
    // Snooze
    private LocalDateTime snoozeUntil;
    
    // Duration tracking
    private Duration estimatedDuration;
    private Duration actualDuration;
}
