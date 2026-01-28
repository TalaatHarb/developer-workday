package net.talaatharb.workday.facade;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import net.talaatharb.workday.event.EventDispatcher;
import net.talaatharb.workday.event.EventLogger;
import net.talaatharb.workday.model.UpdateInfo;
import net.talaatharb.workday.model.UserPreferences;
import net.talaatharb.workday.repository.PreferencesRepository;
import net.talaatharb.workday.service.PreferencesService;
import net.talaatharb.workday.service.UpdateCheckService;

/**
 * Integration tests for UpdateCheckFacade.
 */
class UpdateCheckFacadeTest {
    
    private DB database;
    private PreferencesRepository preferencesRepository;
    private EventDispatcher eventDispatcher;
    private EventLogger eventLogger;
    private PreferencesService preferencesService;
    private UpdateCheckService updateCheckService;
    private UpdateCheckFacade updateCheckFacade;
    private File dbFile;
    
    @BeforeEach
    void setUp() {
        dbFile = new File("test-updatecheck-" + UUID.randomUUID() + ".db");
        database = DBMaker.fileDB(dbFile)
            .transactionEnable()
            .make();
        preferencesRepository = new PreferencesRepository(database);
        eventLogger = new EventLogger();
        eventDispatcher = new EventDispatcher(eventLogger);
        preferencesService = new PreferencesService(preferencesRepository, eventDispatcher);
        updateCheckService = new UpdateCheckService(preferencesService);
        updateCheckFacade = new UpdateCheckFacade(updateCheckService);
    }
    
    @AfterEach
    void tearDown() {
        if (database != null && !database.isClosed()) {
            database.close();
        }
        if (dbFile != null && dbFile.exists()) {
            dbFile.delete();
        }
    }
    
    @Test
    @DisplayName("Manual update check returns update info")
    void testCheckForUpdatesManually() {
        // When: manually checking for updates
        UpdateInfo updateInfo = updateCheckFacade.checkForUpdatesManually();
        
        // Then: update info should be returned
        assertNotNull(updateInfo);
        assertNotNull(updateInfo.getCurrentVersion());
        assertNotNull(updateInfo.getLatestVersion());
    }
    
    @Test
    @DisplayName("Auto update check when enabled and no recent check")
    void testCheckForUpdatesIfEnabled_FirstTime() {
        // Given: auto-check is enabled (default)
        UserPreferences prefs = preferencesService.getPreferences();
        assertTrue(prefs.isAutoCheckForUpdates());
        
        // When: checking for updates if enabled
        UpdateInfo updateInfo = updateCheckFacade.checkForUpdatesIfEnabled();
        
        // Then: update check should be performed
        assertNotNull(updateInfo);
    }
    
    @Test
    @DisplayName("Auto update check skipped when disabled")
    void testCheckForUpdatesIfEnabled_Disabled() {
        // Given: auto-check is disabled
        UserPreferences prefs = preferencesService.getPreferences();
        prefs.setAutoCheckForUpdates(false);
        preferencesService.updatePreferences(prefs);
        
        // When: checking for updates if enabled
        UpdateInfo updateInfo = updateCheckFacade.checkForUpdatesIfEnabled();
        
        // Then: update check should be skipped
        assertNull(updateInfo);
    }
    
    @Test
    @DisplayName("Auto update check skipped when checked recently")
    void testCheckForUpdatesIfEnabled_RecentCheck() {
        // Given: auto-check is enabled and checked 1 hour ago
        UserPreferences prefs = preferencesService.getPreferences();
        prefs.setLastUpdateCheck(LocalDateTime.now().minusHours(1));
        preferencesService.updatePreferences(prefs);
        
        // When: checking for updates if enabled
        UpdateInfo updateInfo = updateCheckFacade.checkForUpdatesIfEnabled();
        
        // Then: update check should be skipped
        assertNull(updateInfo);
    }
    
    @Test
    @DisplayName("Auto update check performed when last check was over 24 hours ago")
    void testCheckForUpdatesIfEnabled_OldCheck() {
        // Given: auto-check is enabled and last check was 25 hours ago
        UserPreferences prefs = preferencesService.getPreferences();
        prefs.setLastUpdateCheck(LocalDateTime.now().minusHours(25));
        preferencesService.updatePreferences(prefs);
        
        // When: checking for updates if enabled
        UpdateInfo updateInfo = updateCheckFacade.checkForUpdatesIfEnabled();
        
        // Then: update check should be performed
        assertNotNull(updateInfo);
    }
    
    @Test
    @DisplayName("Get current version returns version string")
    void testGetCurrentVersion() {
        // When: getting current version
        String version = updateCheckFacade.getCurrentVersion();
        
        // Then: version should not be null or empty
        assertNotNull(version);
        assertFalse(version.isEmpty());
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"), "Version should match semantic versioning pattern");
    }
    
    @Test
    @DisplayName("Update info contains required fields when update is available")
    void testUpdateInfo_Fields() {
        // When: manually checking for updates
        UpdateInfo updateInfo = updateCheckFacade.checkForUpdatesManually();
        
        // Then: required fields should be present
        assertNotNull(updateInfo.getCurrentVersion());
        assertNotNull(updateInfo.getLatestVersion());
        
        // And: if update is available, additional fields should be present
        if (updateInfo.isUpdateAvailable()) {
            assertNotNull(updateInfo.getDownloadUrl(), "Download URL should be present");
            assertNotNull(updateInfo.getReleaseNotes(), "Release notes should be present");
        }
    }
}
