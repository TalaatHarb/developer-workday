package net.talaatharb.workday.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.talaatharb.workday.model.UpdateInfo;
import net.talaatharb.workday.model.UserPreferences;

/**
 * Tests for UpdateCheckService.
 */
class UpdateCheckServiceTest {
    
    @Mock
    private PreferencesService preferencesService;
    
    private UpdateCheckService updateCheckService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        updateCheckService = new UpdateCheckService(preferencesService);
    }
    
    @Test
    @DisplayName("Check for updates returns update info")
    void testCheckForUpdates() {
        // Given: preferences service is configured
        UserPreferences prefs = UserPreferences.builder()
            .autoCheckForUpdates(true)
            .build();
        when(preferencesService.getPreferences()).thenReturn(prefs);
        
        // When: checking for updates
        UpdateInfo updateInfo = updateCheckService.checkForUpdates();
        
        // Then: update info should be returned
        assertNotNull(updateInfo);
        assertNotNull(updateInfo.getCurrentVersion());
        assertNotNull(updateInfo.getLatestVersion());
        
        // And: last check time should be updated
        verify(preferencesService, times(2)).getPreferences(); // Once for check, once for update
        verify(preferencesService).updatePreferences(any(UserPreferences.class));
    }
    
    @Test
    @DisplayName("Should check for updates when auto-check is enabled")
    void testShouldCheckForUpdates_Enabled() {
        // Given: auto-check is enabled and no recent check
        UserPreferences prefs = UserPreferences.builder()
            .autoCheckForUpdates(true)
            .lastUpdateCheck(null)
            .build();
        when(preferencesService.getPreferences()).thenReturn(prefs);
        
        // When: checking if update should be performed
        boolean should = updateCheckService.shouldCheckForUpdates();
        
        // Then: should return true
        assertTrue(should);
    }
    
    @Test
    @DisplayName("Should not check for updates when auto-check is disabled")
    void testShouldCheckForUpdates_Disabled() {
        // Given: auto-check is disabled
        UserPreferences prefs = UserPreferences.builder()
            .autoCheckForUpdates(false)
            .build();
        when(preferencesService.getPreferences()).thenReturn(prefs);
        
        // When: checking if update should be performed
        boolean should = updateCheckService.shouldCheckForUpdates();
        
        // Then: should return false
        assertFalse(should);
    }
    
    @Test
    @DisplayName("Should not check for updates when checked recently (less than 24 hours)")
    void testShouldCheckForUpdates_RecentCheck() {
        // Given: auto-check is enabled but checked 12 hours ago
        UserPreferences prefs = UserPreferences.builder()
            .autoCheckForUpdates(true)
            .lastUpdateCheck(LocalDateTime.now().minusHours(12))
            .build();
        when(preferencesService.getPreferences()).thenReturn(prefs);
        
        // When: checking if update should be performed
        boolean should = updateCheckService.shouldCheckForUpdates();
        
        // Then: should return false
        assertFalse(should);
    }
    
    @Test
    @DisplayName("Should check for updates when last check was more than 24 hours ago")
    void testShouldCheckForUpdates_OldCheck() {
        // Given: auto-check is enabled and last check was 25 hours ago
        UserPreferences prefs = UserPreferences.builder()
            .autoCheckForUpdates(true)
            .lastUpdateCheck(LocalDateTime.now().minusHours(25))
            .build();
        when(preferencesService.getPreferences()).thenReturn(prefs);
        
        // When: checking if update should be performed
        boolean should = updateCheckService.shouldCheckForUpdates();
        
        // Then: should return true
        assertTrue(should);
    }
    
    @Test
    @DisplayName("Get current version returns version string")
    void testGetCurrentVersion() {
        // When: getting current version
        String version = updateCheckService.getCurrentVersion();
        
        // Then: version should not be null or empty
        assertNotNull(version);
        assertFalse(version.isEmpty());
    }
    
    @Test
    @DisplayName("Update check returns correct update available status")
    void testUpdateAvailableDetection() {
        // Given: preferences service is configured
        UserPreferences prefs = UserPreferences.builder()
            .autoCheckForUpdates(true)
            .build();
        when(preferencesService.getPreferences()).thenReturn(prefs);
        
        // When: checking for updates
        UpdateInfo updateInfo = updateCheckService.checkForUpdates();
        
        // Then: update info should contain proper fields when update is available
        if (updateInfo.isUpdateAvailable()) {
            assertNotNull(updateInfo.getDownloadUrl(), "Download URL should be present when update is available");
            assertNotNull(updateInfo.getReleaseNotes(), "Release notes should be present when update is available");
        }
    }
}
