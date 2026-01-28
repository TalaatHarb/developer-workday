package net.talaatharb.workday.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for productivity statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductivityStatistics {
    // Overall stats
    private int totalTasksCompleted;
    private int totalTasksCreated;
    private double completionRate;
    
    // Time period stats
    private int tasksCompletedToday;
    private int tasksCompletedThisWeek;
    private int tasksCompletedThisMonth;
    
    // Streaks
    private int currentStreak;
    private int longestStreak;
    private LocalDate streakStartDate;
    
    // Category breakdown (categoryId -> task count)
    private Map<String, Integer> tasksByCategory;
    private Map<String, Long> timeByCategory;  // in minutes
    
    // Daily completion trend (date -> task count)
    private Map<LocalDate, Integer> dailyCompletions;
}
