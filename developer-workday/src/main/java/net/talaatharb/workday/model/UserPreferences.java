package net.talaatharb.workday.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User preferences and settings model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    
    // General settings
    @Builder.Default
    private boolean startOnBoot = false;
    
    @Builder.Default
    private boolean minimizeToTray = true;
    
    @Builder.Default
    private String defaultView = "today"; // today, upcoming, calendar, all
    
    @Builder.Default
    private String language = "en"; // en, es, fr, de, etc.
    
    // Appearance settings
    @Builder.Default
    private String theme = "light"; // light, dark
    
    @Builder.Default
    private String accentColor = "#3498db"; // hex color
    
    @Builder.Default
    private int fontSize = 13; // base font size in points
    
    // Notification settings
    @Builder.Default
    private boolean desktopNotificationsEnabled = true;
    
    @Builder.Default
    private boolean soundEnabled = false;
    
    @Builder.Default
    private int reminderLeadTimeMinutes = 15; // minutes before due time
    
    // Keyboard shortcuts (not editable yet, just stored)
    @Builder.Default
    private String quickAddShortcut = "Ctrl+N";
    
    @Builder.Default
    private String settingsShortcut = "Ctrl+,";
    
    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
