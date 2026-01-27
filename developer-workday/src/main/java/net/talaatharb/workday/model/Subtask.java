package net.talaatharb.workday.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subtask entity representing a checklist item within a task.
 * Implements Serializable for MapDB storage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subtask implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private String title;
    private boolean completed;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
