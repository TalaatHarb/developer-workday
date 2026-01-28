package net.talaatharb.workday.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for weekly statistics data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyStatistics {
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private int totalTasksCompleted;
    private int totalTasksPlanned;
    private double completionRate;
    private int highPriorityCompleted;
    private int mediumPriorityCompleted;
    private int lowPriorityCompleted;
    private int overdueTasks;
}
