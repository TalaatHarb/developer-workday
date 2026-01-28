package net.talaatharb.workday.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.dtos.ProductivityStatistics;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating productivity statistics.
 */
@Slf4j
@RequiredArgsConstructor
public class StatisticsService {
    
    private final TaskRepository taskRepository;
    
    /**
     * Calculate overall productivity statistics.
     */
    public ProductivityStatistics calculateStatistics() {
        log.debug("Calculating productivity statistics");
        
        List<Task> allTasks = taskRepository.findAll();
        List<Task> completedTasks = allTasks.stream()
            .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
            .collect(Collectors.toList());
        
        LocalDate today = LocalDate.now();
        
        return ProductivityStatistics.builder()
            .totalTasksCompleted(completedTasks.size())
            .totalTasksCreated(allTasks.size())
            .completionRate(calculateCompletionRate(allTasks.size(), completedTasks.size()))
            .tasksCompletedToday(countTasksCompletedOnDate(completedTasks, today))
            .tasksCompletedThisWeek(countTasksCompletedInWeek(completedTasks, today))
            .tasksCompletedThisMonth(countTasksCompletedInMonth(completedTasks, today))
            .currentStreak(calculateCurrentStreak(completedTasks))
            .longestStreak(calculateLongestStreak(completedTasks))
            .tasksByCategory(groupTasksByCategory(completedTasks))
            .dailyCompletions(calculateDailyCompletions(completedTasks, 30))
            .build();
    }
    
    /**
     * Calculate completion rate.
     */
    private double calculateCompletionRate(int total, int completed) {
        if (total == 0) {
            return 0.0;
        }
        return (double) completed / total * 100.0;
    }
    
    /**
     * Count tasks completed on a specific date.
     */
    private int countTasksCompletedOnDate(List<Task> completedTasks, LocalDate date) {
        return (int) completedTasks.stream()
            .filter(t -> t.getCompletedAt() != null)
            .filter(t -> t.getCompletedAt().toLocalDate().equals(date))
            .count();
    }
    
    /**
     * Count tasks completed in the current week.
     */
    private int countTasksCompletedInWeek(List<Task> completedTasks, LocalDate today) {
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        return (int) completedTasks.stream()
            .filter(t -> t.getCompletedAt() != null)
            .filter(t -> {
                LocalDate completedDate = t.getCompletedAt().toLocalDate();
                return !completedDate.isBefore(weekStart) && !completedDate.isAfter(today);
            })
            .count();
    }
    
    /**
     * Count tasks completed in the current month.
     */
    private int countTasksCompletedInMonth(List<Task> completedTasks, LocalDate today) {
        LocalDate monthStart = today.withDayOfMonth(1);
        return (int) completedTasks.stream()
            .filter(t -> t.getCompletedAt() != null)
            .filter(t -> {
                LocalDate completedDate = t.getCompletedAt().toLocalDate();
                return !completedDate.isBefore(monthStart) && !completedDate.isAfter(today);
            })
            .count();
    }
    
    /**
     * Calculate current streak of consecutive days with completed tasks.
     */
    private int calculateCurrentStreak(List<Task> completedTasks) {
        Set<LocalDate> completionDates = completedTasks.stream()
            .filter(t -> t.getCompletedAt() != null)
            .map(t -> t.getCompletedAt().toLocalDate())
            .collect(Collectors.toSet());
        
        if (completionDates.isEmpty()) {
            return 0;
        }
        
        LocalDate date = LocalDate.now();
        int streak = 0;
        
        // Check if there's completion today or yesterday (to allow for end of day)
        if (!completionDates.contains(date) && !completionDates.contains(date.minusDays(1))) {
            return 0;
        }
        
        // Count backwards from today
        while (completionDates.contains(date)) {
            streak++;
            date = date.minusDays(1);
        }
        
        return streak;
    }
    
    /**
     * Calculate longest streak.
     */
    private int calculateLongestStreak(List<Task> completedTasks) {
        Set<LocalDate> completionDates = completedTasks.stream()
            .filter(t -> t.getCompletedAt() != null)
            .map(t -> t.getCompletedAt().toLocalDate())
            .collect(Collectors.toSet());
        
        if (completionDates.isEmpty()) {
            return 0;
        }
        
        List<LocalDate> sortedDates = completionDates.stream()
            .sorted()
            .collect(Collectors.toList());
        
        int longestStreak = 1;
        int currentStreak = 1;
        
        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate prevDate = sortedDates.get(i - 1);
            LocalDate currentDate = sortedDates.get(i);
            
            if (ChronoUnit.DAYS.between(prevDate, currentDate) == 1) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        
        return longestStreak;
    }
    
    /**
     * Group completed tasks by category.
     */
    private Map<String, Integer> groupTasksByCategory(List<Task> completedTasks) {
        Map<String, Integer> categoryMap = new HashMap<>();
        
        completedTasks.stream()
            .filter(t -> t.getCategoryId() != null)
            .forEach(t -> {
                String categoryId = t.getCategoryId().toString();
                categoryMap.put(categoryId, categoryMap.getOrDefault(categoryId, 0) + 1);
            });
        
        return categoryMap;
    }
    
    /**
     * Calculate daily completions for the last N days.
     */
    private Map<LocalDate, Integer> calculateDailyCompletions(List<Task> completedTasks, int days) {
        Map<LocalDate, Integer> dailyMap = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        
        // Initialize map with last N days
        for (int i = days - 1; i >= 0; i--) {
            dailyMap.put(today.minusDays(i), 0);
        }
        
        // Count completions per day
        completedTasks.stream()
            .filter(t -> t.getCompletedAt() != null)
            .map(t -> t.getCompletedAt().toLocalDate())
            .filter(date -> !date.isBefore(today.minusDays(days - 1)))
            .forEach(date -> dailyMap.put(date, dailyMap.getOrDefault(date, 0) + 1));
        
        return dailyMap;
    }
}
