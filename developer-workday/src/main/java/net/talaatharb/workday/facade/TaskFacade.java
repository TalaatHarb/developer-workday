package net.talaatharb.workday.facade;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.service.ReminderService;
import net.talaatharb.workday.service.TaskService;

/**
 * Facade for task-related operations, coordinating between multiple services.
 * Provides a simplified interface for UI controllers.
 */
@Slf4j
@RequiredArgsConstructor
public class TaskFacade {
    
    private final TaskService taskService;
    private final ReminderService reminderService;
    
    /**
     * Get tasks for today's view: overdue tasks first, then today's tasks
     */
    public List<Task> getTasksForToday() {
        LocalDate today = LocalDate.now();
        
        // Get overdue tasks
        List<Task> overdueTasks = taskService.findOverdueTasks();
        
        // Sort overdue tasks by time and priority
        overdueTasks.sort(Comparator
            .comparing((Task t) -> t.getDueTime() != null ? t.getDueTime() : LocalTime.MAX)
            .thenComparing((Task t) -> getPriorityValue(t.getPriority())));
        
        // Get tasks scheduled for today
        List<Task> todaysTasks = taskService.findByScheduledDate(today);
        
        // Sort today's tasks by time and priority
        todaysTasks.sort(Comparator
            .comparing((Task t) -> t.getDueTime() != null ? t.getDueTime() : LocalTime.MAX)
            .thenComparing((Task t) -> getPriorityValue(t.getPriority())));
        
        // Combine with overdue first, then today's tasks
        List<Task> allTasks = new ArrayList<>();
        allTasks.addAll(overdueTasks);
        allTasks.addAll(todaysTasks.stream()
            .filter(t -> !overdueTasks.contains(t)) // Avoid duplicates
            .collect(Collectors.toList()));
        
        return allTasks;
    }
    
    /**
     * Quick add task from a natural language string
     */
    public Task quickAddTask(String quickAddString) {
        log.debug("Quick adding task: {}", quickAddString);
        
        QuickAddResult result = parseQuickAddString(quickAddString);
        
        Task task = Task.builder()
            .title(result.title)
            .scheduledDate(result.date)
            .dueDate(result.date)
            .dueTime(result.time)
            .tags(result.tags)
            .priority(Priority.MEDIUM)
            .status(TaskStatus.TODO)
            .build();
        
        Task createdTask = taskService.createTask(task);
        
        // Schedule reminder if needed
        if (createdTask.getReminderMinutesBefore() != null) {
            reminderService.scheduleReminder(createdTask);
        }
        
        log.info("Quick added task: {}", createdTask.getTitle());
        return createdTask;
    }
    
    /**
     * Parse quick add string
     */
    private QuickAddResult parseQuickAddString(String input) {
        QuickAddResult result = new QuickAddResult();
        String remaining = input;
        
        // Extract tags (words starting with #)
        Pattern tagPattern = Pattern.compile("#(\\w+)");
        Matcher tagMatcher = tagPattern.matcher(input);
        while (tagMatcher.find()) {
            result.tags.add(tagMatcher.group(1));
        }
        remaining = remaining.replaceAll("#\\w+", "").trim();
        
        // Extract time (e.g., "at 5pm", "at 17:00")
        Pattern timePattern = Pattern.compile("at\\s+(\\d{1,2})(:(\\d{2}))?(\\s*([ap]m))?", Pattern.CASE_INSENSITIVE);
        Matcher timeMatcher = timePattern.matcher(remaining);
        if (timeMatcher.find()) {
            result.time = parseTime(timeMatcher.group());
            remaining = remaining.replace(timeMatcher.group(), "").trim();
        }
        
        // Extract date (e.g., "tomorrow", "today", "next monday")
        Pattern datePattern = Pattern.compile("(today|tomorrow|next\\s+\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher dateMatcher = datePattern.matcher(remaining);
        if (dateMatcher.find()) {
            result.date = parseDate(dateMatcher.group());
            remaining = remaining.replace(dateMatcher.group(), "").trim();
        } else {
            result.date = LocalDate.now(); // Default to today
        }
        
        // Rest is the title
        result.title = remaining.trim();
        if (result.title.isEmpty()) {
            result.title = "New Task";
        }
        
        return result;
    }
    
    /**
     * Parse time string
     */
    private LocalTime parseTime(String timeStr) {
        timeStr = timeStr.toLowerCase().replaceAll("at\\s+", "").trim();
        
        try {
            if (timeStr.contains("pm") || timeStr.contains("am")) {
                String cleaned = timeStr.replaceAll("[^0-9:]", "").trim();
                int hour = Integer.parseInt(cleaned.split(":")[0]);
                int minute = cleaned.contains(":") ? Integer.parseInt(cleaned.split(":")[1]) : 0;
                
                if (timeStr.contains("pm") && hour < 12) {
                    hour += 12;
                } else if (timeStr.contains("am") && hour == 12) {
                    hour = 0;
                }
                
                return LocalTime.of(hour, minute);
            } else {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H:mm"));
            }
        } catch (Exception e) {
            log.warn("Failed to parse time: {}", timeStr, e);
            return null;
        }
    }
    
    /**
     * Parse date string
     */
    private LocalDate parseDate(String dateStr) {
        dateStr = dateStr.toLowerCase().trim();
        
        if (dateStr.equals("today")) {
            return LocalDate.now();
        } else if (dateStr.equals("tomorrow")) {
            return LocalDate.now().plusDays(1);
        } else if (dateStr.startsWith("next ")) {
            // Simplified: just add 7 days for "next [day]"
            return LocalDate.now().plusDays(7);
        }
        
        return LocalDate.now(); // Default
    }
    
    /**
     * Get task statistics for a date range
     */
    public TaskStatistics getTaskStatistics(LocalDate startDate, LocalDate endDate) {
        log.debug("Getting task statistics from {} to {}", startDate, endDate);
        
        List<Task> tasks = taskService.findByDueDateBetween(startDate, endDate);
        
        long totalTasks = tasks.size();
        long completedTasks = tasks.stream()
            .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
            .count();
        
        long overdueTasks = tasks.stream()
            .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now()))
            .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED)
            .count();
        
        double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks * 100.0 : 0.0;
        
        return new TaskStatistics(totalTasks, completedTasks, overdueTasks, completionRate);
    }
    
    /**
     * Create a task with all services coordinated
     */
    public Task createTask(Task task) {
        Task createdTask = taskService.createTask(task);
        
        // Schedule reminder if configured
        if (createdTask.getReminderMinutesBefore() != null) {
            reminderService.scheduleReminder(createdTask);
        }
        
        return createdTask;
    }
    
    /**
     * Update a task with all services coordinated
     */
    public Task updateTask(Task task) {
        Task updatedTask = taskService.updateTask(task);
        
        // Update reminder
        reminderService.updateReminder(updatedTask);
        
        return updatedTask;
    }
    
    /**
     * Complete a task with all services coordinated
     */
    public Task completeTask(UUID taskId) {
        Task completedTask = taskService.completeTask(taskId);
        
        // Cancel reminder
        reminderService.cancelReminder(taskId);
        
        return completedTask;
    }
    
    /**
     * Delete a task with all services coordinated
     */
    public void deleteTask(UUID taskId) {
        // Cancel reminder first
        reminderService.cancelReminder(taskId);
        
        // Delete task
        taskService.deleteTask(taskId);
    }
    
    /**
     * Find task by ID
     */
    public Optional<Task> findById(UUID taskId) {
        return taskService.findById(taskId);
    }
    
    /**
     * Find all tasks
     */
    public List<Task> findAll() {
        return taskService.findAll();
    }
    
    /**
     * Find tasks by status
     */
    public List<Task> findByStatus(TaskStatus status) {
        return taskService.findByStatus(status);
    }
    
    /**
     * Find tasks by category
     */
    public List<Task> findByCategoryId(UUID categoryId) {
        return taskService.findByCategoryId(categoryId);
    }
    
    /**
     * Helper to get priority value for sorting
     */
    private int getPriorityValue(Priority priority) {
        if (priority == null) return 99;
        return switch (priority) {
            case URGENT -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }
    
    /**
     * Result of quick add parsing
     */
    private static class QuickAddResult {
        String title;
        LocalDate date;
        LocalTime time;
        List<String> tags = new ArrayList<>();
    }
    
    /**
     * Task statistics data
     */
    public record TaskStatistics(
        long totalTasks,
        long completedTasks,
        long overdueTasks,
        double completionRate
    ) {}
}
