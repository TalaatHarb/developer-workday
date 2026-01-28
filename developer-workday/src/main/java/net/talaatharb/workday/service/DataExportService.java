package net.talaatharb.workday.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.dtos.ExportData;
import net.talaatharb.workday.model.Category;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.UserPreferences;

/**
 * Service for exporting application data to various formats.
 */
@Slf4j
@RequiredArgsConstructor
public class DataExportService {
    
    private final TaskService taskService;
    private final CategoryService categoryService;
    private final PreferencesService preferencesService;
    
    /**
     * Export all application data to a JSON file
     * 
     * @param targetFile the file to export to
     * @throws IOException if export fails
     */
    public void exportToJson(File targetFile) throws IOException {
        log.info("Exporting data to JSON file: {}", targetFile.getAbsolutePath());
        
        // Gather all data
        List<Task> tasks = taskService.findAll();
        List<Category> categories = categoryService.findAll();
        UserPreferences preferences = preferencesService.getPreferences();
        
        // Create export snapshot
        ExportData exportData = ExportData.createSnapshot(tasks, categories, preferences);
        
        // Write to JSON
        ObjectMapper mapper = createObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(targetFile, exportData);
        
        log.info("Successfully exported {} tasks and {} categories to {}", 
            tasks.size(), categories.size(), targetFile.getAbsolutePath());
    }
    
    /**
     * Export tasks to CSV format
     * 
     * @param targetFile the file to export to
     * @throws IOException if export fails
     */
    public void exportTasksToCSV(File targetFile) throws IOException {
        log.info("Exporting tasks to CSV file: {}", targetFile.getAbsolutePath());
        
        List<Task> tasks = taskService.findAll();
        
        try (FileWriter writer = new FileWriter(targetFile, StandardCharsets.UTF_8)) {
            // Write CSV header
            writer.write("ID,Title,Description,Status,Priority,DueDate,DueTime,ScheduledDate," +
                        "Category,Tags,CreatedAt,UpdatedAt,CompletedAt\n");
            
            // Write each task as a CSV row
            for (Task task : tasks) {
                writer.write(formatTaskAsCSV(task));
                writer.write("\n");
            }
        }
        
        log.info("Successfully exported {} tasks to CSV: {}", tasks.size(), targetFile.getAbsolutePath());
    }
    
    /**
     * Format a task as a CSV row
     */
    private String formatTaskAsCSV(Task task) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        return String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
            task.getId() != null ? task.getId().toString() : "",
            escapeCsv(task.getTitle()),
            escapeCsv(task.getDescription()),
            task.getStatus() != null ? task.getStatus().name() : "",
            task.getPriority() != null ? task.getPriority().name() : "",
            task.getDueDate() != null ? task.getDueDate().format(dateFormatter) : "",
            task.getDueTime() != null ? task.getDueTime().format(timeFormatter) : "",
            task.getScheduledDate() != null ? task.getScheduledDate().format(dateFormatter) : "",
            task.getCategoryId() != null ? task.getCategoryId().toString() : "",
            task.getTags() != null ? String.join(";", task.getTags()) : "",
            task.getCreatedAt() != null ? task.getCreatedAt().format(dateTimeFormatter) : "",
            task.getUpdatedAt() != null ? task.getUpdatedAt().format(dateTimeFormatter) : "",
            task.getCompletedAt() != null ? task.getCompletedAt().format(dateTimeFormatter) : ""
        );
    }
    
    /**
     * Escape special characters for CSV
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Replace double quotes with two double quotes
        return value.replace("\"", "\"\"");
    }
    
    /**
     * Create an ObjectMapper configured for exporting data
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }
}
