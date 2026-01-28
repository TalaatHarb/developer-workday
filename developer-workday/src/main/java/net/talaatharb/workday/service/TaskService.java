package net.talaatharb.workday.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.task.*;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.RecurrenceRule;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.repository.TaskRepository;

/**
 * Service class for task business logic operations.
 * Handles task creation, updates, deletion, completion, and scheduling.
 */
@Slf4j
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final EventDispatcher eventDispatcher;
    
    /**
     * Create a new task
     */
    public Task createTask(Task task) {
        log.debug("Creating new task: {}", task.getTitle());
        
        // Save via repository
        Task savedTask = taskRepository.save(task);
        
        // Publish event
        eventDispatcher.publish(new TaskCreatedEvent(savedTask));
        
        log.info("Created task: {} (ID: {})", savedTask.getTitle(), savedTask.getId());
        return savedTask;
    }
    
    /**
     * Update an existing task
     */
    public Task updateTask(Task updatedTask) {
        log.debug("Updating task: {}", updatedTask.getId());
        
        // Get old task for event
        Optional<Task> oldTaskOpt = taskRepository.findById(updatedTask.getId());
        if (oldTaskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found: " + updatedTask.getId());
        }
        
        Task oldTask = oldTaskOpt.get();
        
        // Save updated task
        Task savedTask = taskRepository.save(updatedTask);
        
        // Publish event with before and after states
        eventDispatcher.publish(new TaskUpdatedEvent(oldTask, savedTask));
        
        // Check for specific changes and publish specialized events
        if (oldTask.getPriority() != savedTask.getPriority()) {
            eventDispatcher.publish(new TaskPriorityChangedEvent(
                savedTask.getId(), oldTask.getPriority(), savedTask.getPriority()));
        }
        
        if (!equals(oldTask.getCategoryId(), savedTask.getCategoryId())) {
            eventDispatcher.publish(new TaskMovedToCategoryEvent(
                savedTask.getId(), oldTask.getCategoryId(), savedTask.getCategoryId()));
        }
        
        if (!equals(oldTask.getScheduledDate(), savedTask.getScheduledDate()) 
            && savedTask.getScheduledDate() != null) {
            eventDispatcher.publish(new TaskScheduledEvent(
                savedTask.getId(), savedTask.getScheduledDate()));
        }
        
        log.info("Updated task: {}", savedTask.getId());
        return savedTask;
    }
    
    /**
     * Delete a task
     */
    public void deleteTask(UUID taskId) {
        log.debug("Deleting task: {}", taskId);
        
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        
        Task task = taskOpt.get();
        taskRepository.deleteById(taskId);
        
        // Publish event
        eventDispatcher.publish(new TaskDeletedEvent(task));
        
        log.info("Deleted task: {}", taskId);
    }
    
    /**
     * Complete a task
     */
    public Task completeTask(UUID taskId) {
        log.debug("Completing task: {}", taskId);
        
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        
        Task task = taskOpt.get();
        
        // Check if task is recurring
        if (task.getRecurrence() != null) {
            return completeRecurringTask(task);
        }
        
        // Set completion status and timestamp
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        
        Task completedTask = taskRepository.save(task);
        
        // Publish events
        eventDispatcher.publish(new TaskCompletedEvent(taskId, completedTask.getCompletedAt()));
        eventDispatcher.publish(new TaskUpdatedEvent(task, completedTask));
        
        log.info("Completed task: {}", taskId);
        return completedTask;
    }
    
    /**
     * Complete a recurring task and create next occurrence
     */
    private Task completeRecurringTask(Task task) {
        log.debug("Completing recurring task: {}", task.getId());
        
        // Mark original task as complete
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        Task completedTask = taskRepository.save(task);
        
        // Publish completion events
        eventDispatcher.publish(new TaskCompletedEvent(task.getId(), completedTask.getCompletedAt()));
        
        // Create new task for next occurrence
        Task nextTask = createNextRecurringTask(task);
        if (nextTask != null) {
            log.info("Created next occurrence for recurring task: {}", nextTask.getId());
        }
        
        log.info("Completed recurring task: {}", task.getId());
        return completedTask;
    }
    
    /**
     * Create next occurrence of a recurring task
     */
    private Task createNextRecurringTask(Task completedTask) {
        RecurrenceRule recurrence = completedTask.getRecurrence();
        
        // Calculate next date based on recurrence rule
        LocalDate nextDate = calculateNextDate(completedTask.getScheduledDate(), recurrence);
        
        // Check if we should create another occurrence
        if (shouldCreateNextOccurrence(recurrence, nextDate)) {
            Task nextTask = Task.builder()
                .title(completedTask.getTitle())
                .description(completedTask.getDescription())
                .priority(completedTask.getPriority())
                .status(TaskStatus.TODO)
                .categoryId(completedTask.getCategoryId())
                .tags(completedTask.getTags())
                .scheduledDate(nextDate)
                .dueDate(completedTask.getDueDate() != null ? 
                    nextDate.plusDays(completedTask.getDueDate().toEpochDay() - 
                        completedTask.getScheduledDate().toEpochDay()) : null)
                .dueTime(completedTask.getDueTime())
                .recurrence(recurrence)
                .reminderMinutesBefore(completedTask.getReminderMinutesBefore())
                .estimatedDuration(completedTask.getEstimatedDuration())
                .build();
            
            return createTask(nextTask);
        }
        
        return null;
    }
    
    /**
     * Calculate next date based on recurrence rule
     */
    private LocalDate calculateNextDate(LocalDate currentDate, RecurrenceRule recurrence) {
        if (currentDate == null) {
            currentDate = LocalDate.now();
        }
        
        int interval = recurrence.getInterval() != null ? recurrence.getInterval() : 1;
        
        return switch (recurrence.getType()) {
            case DAILY -> currentDate.plusDays(interval);
            case WEEKLY -> currentDate.plusWeeks(interval);
            case MONTHLY -> currentDate.plusMonths(interval);
            case YEARLY -> currentDate.plusYears(interval);
        };
    }
    
    /**
     * Check if next occurrence should be created
     */
    private boolean shouldCreateNextOccurrence(RecurrenceRule recurrence, LocalDate nextDate) {
        // Check end date
        if (recurrence.getEndDate() != null && nextDate.isAfter(recurrence.getEndDate())) {
            return false;
        }
        
        // Check max occurrences (would need to track count - simplified for now)
        // In a full implementation, we'd track the occurrence count
        
        return true;
    }
    
    /**
     * Schedule a task for a specific date
     */
    public Task scheduleTask(UUID taskId, LocalDate scheduledDate) {
        log.debug("Scheduling task {} for {}", taskId, scheduledDate);
        
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        
        Task task = taskOpt.get();
        Task oldTask = cloneTask(task);
        
        task.setScheduledDate(scheduledDate);
        Task savedTask = taskRepository.save(task);
        
        // Publish events
        eventDispatcher.publish(new TaskScheduledEvent(taskId, scheduledDate));
        eventDispatcher.publish(new TaskUpdatedEvent(oldTask, savedTask));
        
        log.info("Scheduled task {} for {}", taskId, scheduledDate);
        return savedTask;
    }
    
    /**
     * Snooze a task until the specified date/time
     */
    public Task snoozeTask(UUID taskId, LocalDateTime snoozeUntil) {
        Task task = findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        
        task.setSnoozeUntil(snoozeUntil);
        task.setUpdatedAt(LocalDateTime.now());
        
        Task savedTask = taskRepository.save(task);
        
        // Publish event
        TaskSnoozedEvent event = TaskSnoozedEvent.builder()
            .taskId(taskId)
            .snoozeUntil(snoozeUntil)
            .build();
        eventDispatcher.publish(event);
        
        log.info("Snoozed task {} until {}", taskId, snoozeUntil);
        return savedTask;
    }
    
    /**
     * Unsnooze a task (remove snooze)
     */
    public Task unsnoozeTask(UUID taskId) {
        Task task = findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        
        task.setSnoozeUntil(null);
        task.setUpdatedAt(LocalDateTime.now());
        
        Task savedTask = taskRepository.save(task);
        
        log.info("Unsnoozed task {}", taskId);
        return savedTask;
    }
    
    /**
     * Find task by ID
     */
    public Optional<Task> findById(UUID taskId) {
        return taskRepository.findById(taskId);
    }
    
    /**
     * Find all tasks
     */
    public List<Task> findAll() {
        return taskRepository.findAll();
    }
    
    /**
     * Find tasks by status
     */
    public List<Task> findByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }
    
    /**
     * Find tasks by category
     */
    public List<Task> findByCategoryId(UUID categoryId) {
        return taskRepository.findByCategoryId(categoryId);
    }
    
    /**
     * Find tasks by scheduled date
     */
    public List<Task> findByScheduledDate(LocalDate date) {
        return taskRepository.findByScheduledDate(date);
    }
    
    /**
     * Find overdue tasks
     */
    public List<Task> findOverdueTasks() {
        return taskRepository.findOverdueTasks();
    }
    
    /**
     * Find tasks by due date range
     */
    public List<Task> findByDueDateBetween(LocalDate startDate, LocalDate endDate) {
        return taskRepository.findByDueDateBetween(startDate, endDate);
    }
    
    /**
     * Search tasks by keyword in title, description, and tags
     */
    public List<Task> searchTasks(String keyword) {
        log.debug("Searching tasks with keyword: {}", keyword);
        return taskRepository.searchTasks(keyword);
    }
    
    /**
     * Find inbox tasks (tasks without scheduled date or category)
     */
    public List<Task> findInboxTasks() {
        log.debug("Finding inbox tasks");
        return taskRepository.findInboxTasks();
    }
    
    /**
     * Helper method to compare objects safely
     */
    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
    
    /**
     * Helper method to clone a task (shallow copy of key fields)
     */
    private Task cloneTask(Task task) {
        return Task.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .dueDate(task.getDueDate())
            .dueTime(task.getDueTime())
            .scheduledDate(task.getScheduledDate())
            .priority(task.getPriority())
            .status(task.getStatus())
            .categoryId(task.getCategoryId())
            .tags(task.getTags())
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .completedAt(task.getCompletedAt())
            .recurrence(task.getRecurrence())
            .reminderMinutesBefore(task.getReminderMinutesBefore())
            .estimatedDuration(task.getEstimatedDuration())
            .actualDuration(task.getActualDuration())
            .build();
    }
    
    /**
     * Update task notes
     */
    public Task updateNotes(UUID taskId, String notes) {
        log.debug("Updating notes for task: {}", taskId);
        Task task = findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        
        task.setNotes(notes);
        task.setUpdatedAt(LocalDateTime.now());
        
        Task savedTask = taskRepository.save(task);
        eventDispatcher.publish(new TaskUpdatedEvent(task, savedTask));
        
        log.info("Updated notes for task: {}", taskId);
        return savedTask;
    }
    
    /**
     * Add attachment to task
     */
    public Task addAttachment(UUID taskId, net.talaatharb.workday.model.Attachment attachment) {
        log.debug("Adding attachment to task: {}", taskId);
        Task task = findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        
        if (task.getAttachments() == null) {
            task.setAttachments(new java.util.ArrayList<>());
        }
        task.getAttachments().add(attachment);
        task.setUpdatedAt(LocalDateTime.now());
        
        Task savedTask = taskRepository.save(task);
        eventDispatcher.publish(new TaskUpdatedEvent(task, savedTask));
        
        log.info("Added attachment {} to task: {}", attachment.getFileName(), taskId);
        return savedTask;
    }
    
    /**
     * Remove attachment from task
     */
    public Task removeAttachment(UUID taskId, String fileName) {
        log.debug("Removing attachment from task: {}", taskId);
        Task task = findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        
        if (task.getAttachments() != null) {
            task.getAttachments().removeIf(att -> att.getFileName().equals(fileName));
            task.setUpdatedAt(LocalDateTime.now());
            
            Task savedTask = taskRepository.save(task);
            eventDispatcher.publish(new TaskUpdatedEvent(task, savedTask));
            
            log.info("Removed attachment {} from task: {}", fileName, taskId);
            return savedTask;
        }
        
        return task;
    }
}
