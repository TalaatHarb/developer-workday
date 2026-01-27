package net.talaatharb.workday.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.repository.TaskRepository;

/**
 * Service for managing task tags.
 * Provides tag autocomplete, filtering, and management operations.
 */
@Slf4j
@RequiredArgsConstructor
public class TagService {
    
    private final TaskRepository taskRepository;
    
    /**
     * Get all unique tags from all tasks
     */
    public Set<String> getAllTags() {
        List<Task> allTasks = taskRepository.findAll();
        return allTasks.stream()
            .map(Task::getTags)
            .filter(tags -> tags != null)
            .flatMap(Collection::stream)
            .filter(tag -> tag != null && !tag.trim().isEmpty())
            .collect(Collectors.toSet());
    }
    
    /**
     * Get tags matching a prefix (for autocomplete)
     */
    public List<String> getTagsMatchingPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return new ArrayList<>(getAllTags());
        }
        
        String lowerPrefix = prefix.toLowerCase().trim();
        return getAllTags().stream()
            .filter(tag -> tag.toLowerCase().startsWith(lowerPrefix))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get tag usage count
     */
    public Map<String, Long> getTagUsageCount() {
        List<Task> allTasks = taskRepository.findAll();
        return allTasks.stream()
            .map(Task::getTags)
            .filter(tags -> tags != null)
            .flatMap(Collection::stream)
            .filter(tag -> tag != null && !tag.trim().isEmpty())
            .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
    }
    
    /**
     * Rename a tag across all tasks
     */
    public int renameTag(String oldTag, String newTag) {
        if (oldTag == null || newTag == null || oldTag.trim().isEmpty() || newTag.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag names cannot be null or empty");
        }
        
        if (oldTag.equals(newTag)) {
            return 0;
        }
        
        log.info("Renaming tag '{}' to '{}'", oldTag, newTag);
        
        List<Task> allTasks = taskRepository.findAll();
        int updatedCount = 0;
        
        for (Task task : allTasks) {
            if (task.getTags() != null && task.getTags().contains(oldTag)) {
                List<String> updatedTags = new ArrayList<>(task.getTags());
                updatedTags.remove(oldTag);
                if (!updatedTags.contains(newTag)) {
                    updatedTags.add(newTag);
                }
                
                Task updatedTask = Task.builder()
                    .id(task.getId())
                    .title(task.getTitle())
                    .description(task.getDescription())
                    .dueDate(task.getDueDate())
                    .dueTime(task.getDueTime())
                    .scheduledDate(task.getScheduledDate())
                    .priority(task.getPriority())
                    .status(task.getStatus())
                    .categoryId(task.getCategoryId())
                    .tags(updatedTags)
                    .createdAt(task.getCreatedAt())
                    .updatedAt(task.getUpdatedAt())
                    .completedAt(task.getCompletedAt())
                    .recurrence(task.getRecurrence())
                    .reminderMinutesBefore(task.getReminderMinutesBefore())
                    .estimatedDuration(task.getEstimatedDuration())
                    .actualDuration(task.getActualDuration())
                    .build();
                
                taskRepository.save(updatedTask);
                updatedCount++;
            }
        }
        
        log.info("Renamed tag '{}' to '{}' in {} tasks", oldTag, newTag, updatedCount);
        return updatedCount;
    }
    
    /**
     * Delete a tag from all tasks
     */
    public int deleteTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }
        
        log.info("Deleting tag '{}'", tag);
        
        List<Task> allTasks = taskRepository.findAll();
        int updatedCount = 0;
        
        for (Task task : allTasks) {
            if (task.getTags() != null && task.getTags().contains(tag)) {
                List<String> updatedTags = new ArrayList<>(task.getTags());
                updatedTags.remove(tag);
                
                Task updatedTask = Task.builder()
                    .id(task.getId())
                    .title(task.getTitle())
                    .description(task.getDescription())
                    .dueDate(task.getDueDate())
                    .dueTime(task.getDueTime())
                    .scheduledDate(task.getScheduledDate())
                    .priority(task.getPriority())
                    .status(task.getStatus())
                    .categoryId(task.getCategoryId())
                    .tags(updatedTags)
                    .createdAt(task.getCreatedAt())
                    .updatedAt(task.getUpdatedAt())
                    .completedAt(task.getCompletedAt())
                    .recurrence(task.getRecurrence())
                    .reminderMinutesBefore(task.getReminderMinutesBefore())
                    .estimatedDuration(task.getEstimatedDuration())
                    .actualDuration(task.getActualDuration())
                    .build();
                
                taskRepository.save(updatedTask);
                updatedCount++;
            }
        }
        
        log.info("Deleted tag '{}' from {} tasks", tag, updatedCount);
        return updatedCount;
    }
    
    /**
     * Filter tasks by tags (AND logic - tasks must have all specified tags)
     */
    public List<Task> filterByTags(List<Task> tasks, Set<String> requiredTags) {
        if (requiredTags == null || requiredTags.isEmpty()) {
            return tasks;
        }
        
        return tasks.stream()
            .filter(task -> {
                if (task.getTags() == null || task.getTags().isEmpty()) {
                    return false;
                }
                Set<String> taskTags = new HashSet<>(task.getTags());
                return taskTags.containsAll(requiredTags);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Normalize tag name (trim and lowercase for comparison)
     */
    public static String normalizeTag(String tag) {
        return tag == null ? "" : tag.trim().toLowerCase();
    }
}
