package net.talaatharb.workday.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.mapdb.DB;
import org.mapdb.Serializer;

import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;

/**
 * Repository for Task entity using MapDB for persistence.
 */
@Slf4j
public class TaskRepository {
    private static final String TASKS_MAP = "tasks";
    private final DB database;
    private final Map<UUID, Task> tasksMap;
    
    public TaskRepository(DB database) {
        this.database = database;
        this.tasksMap = database.hashMap(TASKS_MAP, Serializer.UUID, Serializer.JAVA).createOrOpen();
        log.info("TaskRepository initialized with {} tasks", tasksMap.size());
    }
    
    /**
     * Save or update a task
     */
    public Task save(Task task) {
        if (task.getId() == null) {
            task.setId(UUID.randomUUID());
        }
        
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(LocalDateTime.now());
        }
        task.setUpdatedAt(LocalDateTime.now());
        
        tasksMap.put(task.getId(), task);
        database.commit();
        log.debug("Saved task: {}", task.getId());
        return task;
    }
    
    /**
     * Find task by ID
     */
    public Optional<Task> findById(UUID id) {
        return Optional.ofNullable(tasksMap.get(id));
    }
    
    /**
     * Find all tasks
     */
    public List<Task> findAll() {
        return new ArrayList<>(tasksMap.values());
    }
    
    /**
     * Find tasks by category ID
     */
    public List<Task> findByCategoryId(UUID categoryId) {
        return tasksMap.values().stream()
            .filter(task -> categoryId.equals(task.getCategoryId()))
            .collect(Collectors.toList());
    }
    
    /**
     * Find tasks by status
     */
    public List<Task> findByStatus(TaskStatus status) {
        return tasksMap.values().stream()
            .filter(task -> status == task.getStatus())
            .collect(Collectors.toList());
    }
    
    /**
     * Find tasks by due date range
     */
    public List<Task> findByDueDateBetween(LocalDate startDate, LocalDate endDate) {
        return tasksMap.values().stream()
            .filter(task -> task.getDueDate() != null)
            .filter(task -> !task.getDueDate().isBefore(startDate) && !task.getDueDate().isAfter(endDate))
            .collect(Collectors.toList());
    }
    
    /**
     * Find overdue tasks (past due date, not completed)
     */
    public List<Task> findOverdueTasks() {
        LocalDate today = LocalDate.now();
        return tasksMap.values().stream()
            .filter(task -> task.getDueDate() != null)
            .filter(task -> task.getDueDate().isBefore(today))
            .filter(task -> task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CANCELLED)
            .collect(Collectors.toList());
    }
    
    /**
     * Find tasks scheduled for a specific date
     */
    public List<Task> findByScheduledDate(LocalDate date) {
        return tasksMap.values().stream()
            .filter(task -> date.equals(task.getScheduledDate()))
            .collect(Collectors.toList());
    }
    
    /**
     * Delete a task by ID
     */
    public boolean deleteById(UUID id) {
        Task removed = tasksMap.remove(id);
        if (removed != null) {
            database.commit();
            log.debug("Deleted task: {}", id);
            return true;
        }
        return false;
    }
    
    /**
     * Delete all tasks
     */
    public void deleteAll() {
        tasksMap.clear();
        database.commit();
        log.debug("Deleted all tasks");
    }
    
    /**
     * Count all tasks
     */
    public long count() {
        return tasksMap.size();
    }
    
    /**
     * Check if task exists by ID
     */
    public boolean existsById(UUID id) {
        return tasksMap.containsKey(id);
    }
}
