package net.talaatharb.workday.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import net.talaatharb.workday.dtos.ExportData;
import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.EventLogger;
import net.talaatharb.workday.model.Category;
import net.talaatharb.workday.model.Priority;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.TaskStatus;
import net.talaatharb.workday.model.UserPreferences;
import net.talaatharb.workday.repository.CategoryRepository;
import net.talaatharb.workday.repository.PreferencesRepository;
import net.talaatharb.workday.repository.TaskRepository;
import net.talaatharb.workday.service.DataImportService.ImportStrategy;

/**
 * Tests for DataExportService and DataImportService following Task 48 acceptance criteria.
 * 
 * Feature: Data Export Import
 *   Scenario: Export all data
 *     Given the application has tasks and categories
 *     When exporting data
 *     Then a JSON file should be generated with all tasks, categories, and settings
 *     And the file should be downloadable to a chosen location
 *
 *   Scenario: Import data
 *     Given a previously exported data file
 *     When importing data
 *     Then the user should choose merge or replace strategy
 *     And data should be imported according to the chosen strategy
 *
 *   Scenario: Export to CSV
 *     Given tasks in the system
 *     When exporting to CSV
 *     Then a CSV file with task data should be generated
 *     And it should be compatible with spreadsheet applications
 */
class DataExportImportServiceTest {
    
    private DB database;
    private TaskRepository taskRepository;
    private CategoryRepository categoryRepository;
    private PreferencesRepository preferencesRepository;
    private EventDispatcher eventDispatcher;
    private EventLogger eventLogger;
    private TaskService taskService;
    private CategoryService categoryService;
    private PreferencesService preferencesService;
    private DataExportService exportService;
    private DataImportService importService;
    private File dbFile;
    private File exportFile;
    
    @BeforeEach
    void setUp() {
        // Setup database
        dbFile = new File("test-exportimport-" + UUID.randomUUID() + ".db");
        database = DBMaker.fileDB(dbFile)
            .transactionEnable()
            .make();
        
        // Setup repositories
        taskRepository = new TaskRepository(database);
        categoryRepository = new CategoryRepository(database);
        preferencesRepository = new PreferencesRepository(database);
        
        // Setup services
        eventLogger = new EventLogger();
        eventDispatcher = new EventDispatcher(eventLogger);
        taskService = new TaskService(taskRepository, eventDispatcher);
        categoryService = new CategoryService(categoryRepository, taskRepository, eventDispatcher);
        preferencesService = new PreferencesService(preferencesRepository, eventDispatcher);
        
        // Setup export/import services
        exportService = new DataExportService(taskService, categoryService, preferencesService);
        importService = new DataImportService(taskService, categoryService, preferencesService);
    }
    
    @AfterEach
    void tearDown() {
        if (database != null && !database.isClosed()) {
            database.close();
        }
        if (dbFile != null && dbFile.exists()) {
            dbFile.delete();
        }
        if (exportFile != null && exportFile.exists()) {
            exportFile.delete();
        }
    }
    
    @Test
    @DisplayName("Export all data - JSON file generated with tasks, categories, and settings")
    void testExportAllData() throws IOException {
        // Given: the application has tasks and categories
        Category category = categoryService.createCategory(Category.builder()
            .name("Work")
            .color("#3498db")
            .build());
        
        taskService.createTask(Task.builder()
            .title("Task 1")
            .description("Description 1")
            .status(TaskStatus.TODO)
            .priority(Priority.HIGH)
            .categoryId(category.getId())
            .dueDate(LocalDate.now().plusDays(1))
            .dueTime(LocalTime.of(10, 0))
            .tags(List.of("urgent"))
            .build());
        
        taskService.createTask(Task.builder()
            .title("Task 2")
            .description("Description 2")
            .status(TaskStatus.IN_PROGRESS)
            .priority(Priority.MEDIUM)
            .build());
        
        // When: exporting data
        exportFile = new File("test-export-" + UUID.randomUUID() + ".json");
        exportService.exportToJson(exportFile);
        
        // Then: a JSON file should be generated
        assertTrue(exportFile.exists(), "Export file should be created");
        assertTrue(exportFile.length() > 0, "Export file should not be empty");
        
        // And: the file should contain all tasks, categories, and settings
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        ExportData exportData = mapper.readValue(exportFile, ExportData.class);
        
        assertNotNull(exportData, "Export data should be readable");
        assertEquals("1.0", exportData.getVersion(), "Should have version");
        assertNotNull(exportData.getExportedAt(), "Should have export timestamp");
        assertEquals(2, exportData.getTasks().size(), "Should export all tasks");
        assertEquals(1, exportData.getCategories().size(), "Should export all categories");
        assertNotNull(exportData.getPreferences(), "Should export preferences");
    }
    
    @Test
    @DisplayName("Import data with MERGE strategy - merges with existing data")
    void testImportDataMergeStrategy() throws IOException {
        // Given: existing data in the system
        Category existingCategory = categoryService.createCategory(Category.builder()
            .name("Existing Category")
            .color("#ff0000")
            .build());
        
        taskService.createTask(Task.builder()
            .title("Existing Task")
            .categoryId(existingCategory.getId())
            .build());
        
        // And: a previously exported data file with different data
        ExportData exportData = ExportData.builder()
            .version("1.0")
            .exportedAt(LocalDateTime.now())
            .tasks(List.of(
                Task.builder()
                    .id(UUID.randomUUID())
                    .title("Imported Task")
                    .status(TaskStatus.TODO)
                    .build()
            ))
            .categories(List.of(
                Category.builder()
                    .id(UUID.randomUUID())
                    .name("Imported Category")
                    .color("#00ff00")
                    .build()
            ))
            .build();
        
        exportFile = new File("test-import-" + UUID.randomUUID() + ".json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.writeValue(exportFile, exportData);
        
        // When: importing data with MERGE strategy
        DataImportService.ImportResult result = importService.importFromJson(exportFile, ImportStrategy.MERGE);
        
        // Then: data should be imported and merged
        assertEquals(1, result.tasksImported, "Should import 1 task");
        assertEquals(1, result.categoriesImported, "Should import 1 category");
        
        // And: existing data should still be present
        List<Task> allTasks = taskService.findAll();
        assertEquals(2, allTasks.size(), "Should have both existing and imported tasks");
        
        List<Category> allCategories = categoryService.findAll();
        assertEquals(2, allCategories.size(), "Should have both existing and imported categories");
    }
    
    @Test
    @DisplayName("Import data with REPLACE strategy - replaces all existing data")
    void testImportDataReplaceStrategy() throws IOException {
        // Given: existing data in the system
        categoryService.createCategory(Category.builder()
            .name("Existing Category")
            .color("#ff0000")
            .build());
        
        taskService.createTask(Task.builder()
            .title("Existing Task")
            .build());
        
        // And: a previously exported data file
        ExportData exportData = ExportData.builder()
            .version("1.0")
            .exportedAt(LocalDateTime.now())
            .tasks(List.of(
                Task.builder()
                    .id(UUID.randomUUID())
                    .title("Imported Task")
                    .status(TaskStatus.TODO)
                    .build()
            ))
            .categories(List.of(
                Category.builder()
                    .id(UUID.randomUUID())
                    .name("Imported Category")
                    .color("#00ff00")
                    .build()
            ))
            .preferences(UserPreferences.builder()
                .theme("dark")
                .build())
            .build();
        
        exportFile = new File("test-import-replace-" + UUID.randomUUID() + ".json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.writeValue(exportFile, exportData);
        
        // When: importing data with REPLACE strategy
        DataImportService.ImportResult result = importService.importFromJson(exportFile, ImportStrategy.REPLACE);
        
        // Then: data should be imported and replace existing
        assertEquals(1, result.tasksImported, "Should import 1 task");
        assertEquals(1, result.categoriesImported, "Should import 1 category");
        assertTrue(result.preferencesImported, "Should import preferences");
        
        // And: existing data should be replaced
        List<Task> allTasks = taskService.findAll();
        assertEquals(1, allTasks.size(), "Should have only imported task");
        assertEquals("Imported Task", allTasks.get(0).getTitle());
        
        List<Category> allCategories = categoryService.findAll();
        assertEquals(1, allCategories.size(), "Should have only imported category");
        assertEquals("Imported Category", allCategories.get(0).getName());
        
        // And: preferences should be imported
        UserPreferences prefs = preferencesService.getPreferences();
        assertEquals("dark", prefs.getTheme());
    }
    
    @Test
    @DisplayName("Export to CSV - generates CSV file compatible with spreadsheets")
    void testExportToCSV() throws IOException {
        // Given: tasks in the system
        taskService.createTask(Task.builder()
            .title("Task 1")
            .description("Description 1")
            .status(TaskStatus.TODO)
            .priority(Priority.HIGH)
            .dueDate(LocalDate.of(2026, 1, 28))
            .dueTime(LocalTime.of(10, 0))
            .tags(List.of("urgent", "work"))
            .build());
        
        taskService.createTask(Task.builder()
            .title("Task 2")
            .description("Description with \"quotes\"")
            .status(TaskStatus.COMPLETED)
            .priority(Priority.LOW)
            .build());
        
        // When: exporting to CSV
        exportFile = new File("test-export-" + UUID.randomUUID() + ".csv");
        exportService.exportTasksToCSV(exportFile);
        
        // Then: a CSV file should be generated
        assertTrue(exportFile.exists(), "CSV file should be created");
        assertTrue(exportFile.length() > 0, "CSV file should not be empty");
        
        // And: it should be compatible with spreadsheet applications
        String csvContent = Files.readString(exportFile.toPath());
        
        // Check header
        assertTrue(csvContent.startsWith("ID,Title,Description,Status,Priority"), 
            "CSV should have proper header");
        
        // Check content
        String[] lines = csvContent.split("\n");
        assertEquals(3, lines.length, "CSV should have header + 2 data rows");
        
        // Check first task (titles are in quotes in CSV)
        assertTrue(lines[1].contains("\"Task 1\""), "CSV should contain Task 1");
        assertTrue(lines[1].contains("TODO"), "CSV should contain status");
        assertTrue(lines[1].contains("HIGH"), "CSV should contain priority");
        
        // Check second task with quotes
        assertTrue(lines[2].contains("\"Task 2\""), "CSV should contain Task 2");
        assertTrue(lines[2].contains("COMPLETED"), "CSV should contain status");
        assertTrue(lines[2].contains("\"\"quotes\"\""), "CSV should properly escape quotes");
    }
    
    @Test
    @DisplayName("Export and re-import maintains data integrity")
    void testExportImportRoundTrip() throws IOException {
        // Given: tasks and categories in the system
        Category category = categoryService.createCategory(Category.builder()
            .name("Work")
            .color("#3498db")
            .build());
        
        Task task = taskService.createTask(Task.builder()
            .title("Important Task")
            .description("Task description")
            .status(TaskStatus.TODO)
            .priority(Priority.URGENT)
            .categoryId(category.getId())
            .dueDate(LocalDate.now().plusDays(1))
            .dueTime(LocalTime.of(14, 30))
            .tags(List.of("work", "urgent"))
            .build());
        
        // When: exporting data
        exportFile = new File("test-roundtrip-" + UUID.randomUUID() + ".json");
        exportService.exportToJson(exportFile);
        
        // And: clearing existing data
        categoryService.deleteCategory(category.getId(), CategoryService.TaskHandlingStrategy.DELETE_TASKS);
        
        // And: re-importing data
        DataImportService.ImportResult result = importService.importFromJson(exportFile, ImportStrategy.REPLACE);
        
        // Then: data should be restored
        assertEquals(1, result.tasksImported);
        assertEquals(1, result.categoriesImported);
        
        List<Task> tasks = taskService.findAll();
        assertEquals(1, tasks.size());
        Task restoredTask = tasks.get(0);
        
        assertEquals("Important Task", restoredTask.getTitle());
        assertEquals("Task description", restoredTask.getDescription());
        assertEquals(TaskStatus.TODO, restoredTask.getStatus());
        assertEquals(Priority.URGENT, restoredTask.getPriority());
        assertEquals(2, restoredTask.getTags().size());
        assertTrue(restoredTask.getTags().contains("work"));
        assertTrue(restoredTask.getTags().contains("urgent"));
    }
    
    @Test
    @DisplayName("Import handles empty export data")
    void testImportEmptyData() throws IOException {
        // Given: an empty export file
        ExportData exportData = ExportData.builder()
            .version("1.0")
            .exportedAt(LocalDateTime.now())
            .tasks(List.of())
            .categories(List.of())
            .build();
        
        exportFile = new File("test-import-empty-" + UUID.randomUUID() + ".json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.writeValue(exportFile, exportData);
        
        // When: importing data
        DataImportService.ImportResult result = importService.importFromJson(exportFile, ImportStrategy.MERGE);
        
        // Then: should handle empty data gracefully
        assertEquals(0, result.tasksImported);
        assertEquals(0, result.categoriesImported);
    }
}
