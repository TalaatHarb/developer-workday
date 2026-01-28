package net.talaatharb.workday.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.talaatharb.workday.model.Category;
import net.talaatharb.workday.model.Task;
import net.talaatharb.workday.model.UserPreferences;

/**
 * DTO for exporting all application data to JSON.
 * Contains tasks, categories, and user preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportData {
    
    private String version;
    private LocalDateTime exportedAt;
    private List<Task> tasks;
    private List<Category> categories;
    private UserPreferences preferences;
    
    /**
     * Create an export data snapshot with the current version
     */
    public static ExportData createSnapshot(List<Task> tasks, List<Category> categories, UserPreferences preferences) {
        return ExportData.builder()
            .version("1.0")
            .exportedAt(LocalDateTime.now())
            .tasks(tasks)
            .categories(categories)
            .preferences(preferences)
            .build();
    }
}
