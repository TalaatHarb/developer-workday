package net.talaatharb.workday.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskDTO {
    private UUID id;
    private String title;
    private boolean completed;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
