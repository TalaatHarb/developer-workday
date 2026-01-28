package net.talaatharb.workday.service;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.UpdateInfo;
import net.talaatharb.workday.model.UserPreferences;

/**
 * Service for checking application updates.
 * This is a stub implementation that simulates update checking.
 * In a real implementation, this would connect to an update server.
 */
@Slf4j
@RequiredArgsConstructor
public class UpdateCheckService {
    
    // Current application version (in real app, this would come from build config/properties)
    private static final String CURRENT_VERSION = "1.0.0";
    
    // Simulated latest version (in real app, fetched from server)
    private static final String SIMULATED_LATEST_VERSION = "1.1.0";
    
    private final PreferencesService preferencesService;
    
    /**
     * Check for updates synchronously
     * @return UpdateInfo containing current and latest version information
     */
    public UpdateInfo checkForUpdates() {
        log.info("Checking for application updates...");
        
        try {
            // Update last check time
            updateLastCheckTime();
            
            // Simulate network call to update server
            // In real implementation, this would make an HTTP request to a version endpoint
            Thread.sleep(500); // Simulate network delay
            
            String latestVersion = fetchLatestVersion();
            boolean updateAvailable = isNewerVersion(CURRENT_VERSION, latestVersion);
            
            UpdateInfo updateInfo = UpdateInfo.builder()
                .currentVersion(CURRENT_VERSION)
                .latestVersion(latestVersion)
                .updateAvailable(updateAvailable)
                .downloadUrl(updateAvailable ? "https://github.com/your-repo/releases/latest" : null)
                .releaseNotes(updateAvailable ? generateReleaseNotes(latestVersion) : null)
                .releaseDate(updateAvailable ? LocalDateTime.now().minusDays(3) : null)
                .critical(false)
                .build();
            
            log.info("Update check completed: current={}, latest={}, updateAvailable={}", 
                CURRENT_VERSION, latestVersion, updateAvailable);
            
            return updateInfo;
        } catch (Exception e) {
            log.error("Failed to check for updates", e);
            
            // Return error info
            return UpdateInfo.builder()
                .currentVersion(CURRENT_VERSION)
                .latestVersion(CURRENT_VERSION)
                .updateAvailable(false)
                .build();
        }
    }
    
    /**
     * Check if updates should be performed (based on user preferences)
     */
    public boolean shouldCheckForUpdates() {
        UserPreferences preferences = preferencesService.getPreferences();
        
        if (!preferences.isAutoCheckForUpdates()) {
            log.debug("Auto-update check is disabled");
            return false;
        }
        
        // Check if enough time has passed since last check (24 hours)
        if (preferences.getLastUpdateCheck() != null) {
            LocalDateTime lastCheck = preferences.getLastUpdateCheck();
            LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
            
            if (lastCheck.isAfter(twentyFourHoursAgo)) {
                log.debug("Update check skipped - last check was less than 24 hours ago");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Simulate fetching latest version from server
     */
    private String fetchLatestVersion() {
        // In real implementation, this would:
        // 1. Make HTTP request to update server/GitHub API
        // 2. Parse JSON response
        // 3. Extract latest version number
        
        // For now, return a simulated version
        return SIMULATED_LATEST_VERSION;
    }
    
    /**
     * Compare version strings to determine if an update is available
     */
    private boolean isNewerVersion(String current, String latest) {
        try {
            // Simple version comparison (assumes semantic versioning: major.minor.patch)
            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");
            
            for (int i = 0; i < Math.min(currentParts.length, latestParts.length); i++) {
                int currentPart = Integer.parseInt(currentParts[i]);
                int latestPart = Integer.parseInt(latestParts[i]);
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            // If all parts are equal, check if latest has more parts
            return latestParts.length > currentParts.length;
        } catch (Exception e) {
            log.warn("Failed to compare versions: {} vs {}", current, latest, e);
            return false;
        }
    }
    
    /**
     * Generate release notes (stub implementation)
     */
    private String generateReleaseNotes(String version) {
        return String.format("""
            What's New in Version %s:
            
            • Improved performance and stability
            • New features and enhancements
            • Bug fixes and improvements
            
            For full release notes, visit our GitHub page.
            """, version);
    }
    
    /**
     * Update the last check time in user preferences
     */
    private void updateLastCheckTime() {
        UserPreferences preferences = preferencesService.getPreferences();
        preferences.setLastUpdateCheck(LocalDateTime.now());
        preferencesService.updatePreferences(preferences);
    }
    
    /**
     * Get the current application version
     */
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }
}
