package net.talaatharb.workday.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.dtos.ExportData;
import net.talaatharb.workday.model.Category;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.UserPreferences;

/**
 * Service for importing application data from various formats.
 */
@Slf4j
@RequiredArgsConstructor
public class DataImportService {
    
    private final TaskService taskService;
    private final CategoryService categoryService;
    private final PreferencesService preferencesService;
    
    /**
     * Import strategy enum
     */
    public enum ImportStrategy {
        MERGE,   // Merge imported data with existing data
        REPLACE  // Replace all existing data with imported data
    }
    
    /**
     * Import data from a JSON file
     * 
     * @param sourceFile the file to import from
     * @param strategy the import strategy (MERGE or REPLACE)
     * @return summary of import operation
     * @throws IOException if import fails
     */
    public ImportResult importFromJson(File sourceFile, ImportStrategy strategy) throws IOException {
        log.info("Importing data from JSON file: {} with strategy: {}", 
            sourceFile.getAbsolutePath(), strategy);
        
        // Parse JSON file
        ObjectMapper mapper = createObjectMapper();
        ExportData exportData = mapper.readValue(sourceFile, ExportData.class);
        
        log.debug("Loaded export data: version={}, tasks={}, categories={}", 
            exportData.getVersion(), 
            exportData.getTasks() != null ? exportData.getTasks().size() : 0,
            exportData.getCategories() != null ? exportData.getCategories().size() : 0);
        
        ImportResult result = new ImportResult();
        
        if (strategy == ImportStrategy.REPLACE) {
            // Delete all existing data
            deleteAllData();
        }
        
        // Import categories first (tasks may reference them)
        if (exportData.getCategories() != null) {
            Map<UUID, UUID> categoryIdMapping = importCategories(exportData.getCategories(), strategy);
            result.categoriesImported = exportData.getCategories().size(); // Count all imported categories
            
            // If we have category ID mappings, update task references
            if (!categoryIdMapping.isEmpty()) {
                updateTaskCategoryReferences(exportData.getTasks(), categoryIdMapping);
            }
        }
        
        // Import tasks
        if (exportData.getTasks() != null) {
            result.tasksImported = importTasks(exportData.getTasks(), strategy);
        }
        
        // Import preferences (only if REPLACE strategy)
        if (strategy == ImportStrategy.REPLACE && exportData.getPreferences() != null) {
            preferencesService.updatePreferences(exportData.getPreferences());
            result.preferencesImported = true;
        }
        
        log.info("Successfully imported {} tasks and {} categories", 
            result.tasksImported, result.categoriesImported);
        
        return result;
    }
    
    /**
     * Delete all existing data
     */
    private void deleteAllData() {
        log.info("Deleting all existing data (REPLACE strategy)");
        
        // Delete all tasks
        List<Task> allTasks = taskService.findAll();
        for (Task task : allTasks) {
            taskService.deleteTask(task.getId());
        }
        
        // Delete all categories
        List<Category> allCategories = categoryService.findAll();
        for (Category category : allCategories) {
            // Use DELETE_TASKS strategy to remove tasks in the category as well
            categoryService.deleteCategory(category.getId(), CategoryService.TaskHandlingStrategy.DELETE_TASKS);
        }
    }
    
    /**
     * Import categories
     * 
     * @return mapping of old IDs to new IDs (for MERGE strategy)
     */
    private Map<UUID, UUID> importCategories(List<Category> categories, ImportStrategy strategy) {
        Map<UUID, UUID> idMapping = new HashMap<>();
        
        for (Category category : categories) {
            UUID oldId = category.getId();
            
            if (strategy == ImportStrategy.MERGE) {
                // Check if category with same name exists
                List<Category> existing = categoryService.findAll();
                boolean exists = existing.stream()
                    .anyMatch(c -> c.getName().equals(category.getName()));
                
                if (exists) {
                    log.debug("Category '{}' already exists, skipping", category.getName());
                    // Find the existing category and map IDs
                    existing.stream()
                        .filter(c -> c.getName().equals(category.getName()))
                        .findFirst()
                        .ifPresent(c -> idMapping.put(oldId, c.getId()));
                    continue;
                }
                
                // Generate new ID to avoid conflicts
                category.setId(UUID.randomUUID());
                idMapping.put(oldId, category.getId());
            }
            
            categoryService.createCategory(category);
            log.debug("Imported category: {}", category.getName());
        }
        
        return idMapping;
    }
    
    /**
     * Import tasks
     */
    private int importTasks(List<Task> tasks, ImportStrategy strategy) {
        int imported = 0;
        
        for (Task task : tasks) {
            if (strategy == ImportStrategy.MERGE) {
                // Generate new ID to avoid conflicts
                task.setId(UUID.randomUUID());
            }
            
            taskService.createTask(task);
            imported++;
            log.debug("Imported task: {}", task.getTitle());
        }
        
        return imported;
    }
    
    /**
     * Update task category references after category ID mapping
     */
    private void updateTaskCategoryReferences(List<Task> tasks, Map<UUID, UUID> categoryIdMapping) {
        if (tasks == null) return;
        
        for (Task task : tasks) {
            if (task.getCategoryId() != null && categoryIdMapping.containsKey(task.getCategoryId())) {
                UUID newCategoryId = categoryIdMapping.get(task.getCategoryId());
                task.setCategoryId(newCategoryId);
                log.debug("Updated task '{}' category reference: {} -> {}", 
                    task.getTitle(), task.getCategoryId(), newCategoryId);
            }
        }
    }
    
    /**
     * Create an ObjectMapper configured for importing data
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
    
    /**
     * Result of import operation
     */
    public static class ImportResult {
        public int tasksImported;
        public int categoriesImported;
        public boolean preferencesImported;
        
        @Override
        public String toString() {
            return String.format("Imported %d tasks, %d categories, preferences: %s", 
                tasksImported, categoriesImported, preferencesImported);
        }
    }
}
