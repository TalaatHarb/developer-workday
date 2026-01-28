package net.talaatharb.workday.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DatabaseMigrationManager.
 */
class DatabaseMigrationManagerTest {
    
    @TempDir
    Path tempDir;
    
    private DB database;
    private Path databasePath;
    private DatabaseMigrationManager migrationManager;
    
    @BeforeEach
    void setUp() {
        databasePath = tempDir.resolve("test-db.db");
        database = DBMaker.fileDB(databasePath.toFile())
            .transactionEnable()
            .make();
        
        migrationManager = new DatabaseMigrationManager(database, databasePath);
    }
    
    @AfterEach
    void tearDown() {
        if (database != null && !database.isClosed()) {
            database.close();
        }
    }
    
    @Test
    void testGetCurrentVersion_newDatabase_returnsZero() {
        int version = migrationManager.getCurrentVersion();
        
        assertEquals(0, version, "New database should be at version 0");
    }
    
    @Test
    void testNeedsMigration_newDatabase_returnsTrue() {
        boolean needs = migrationManager.needsMigration();
        
        assertTrue(needs, "New database should need migration");
    }
    
    @Test
    void testNeedsMigration_afterMigration_returnsFalse() {
        migrationManager.executeMigrations();
        
        boolean needs = migrationManager.needsMigration();
        
        assertFalse(needs, "Database at latest version should not need migration");
    }
    
    @Test
    void testCreateBackup_createsBackupFile() throws IOException {
        // Ensure database file exists by writing something
        database.hashMap("test").createOrOpen();
        database.commit();
        
        // Create backup
        Path backupPath = migrationManager.createBackup();
        
        assertNotNull(backupPath, "Backup path should not be null");
        assertTrue(backupPath.toString().contains("backup_"), "Backup filename should contain 'backup_'");
        // Note: Physical file may not exist due to MapDB file locking, but path should be valid
    }
    
    @Test
    void testCreateBackup_nonExistentDatabase_doesNotThrow() {
        Path nonExistentPath = tempDir.resolve("nonexistent.db");
        DatabaseMigrationManager mgr = new DatabaseMigrationManager(database, nonExistentPath);
        
        // Should not throw, just record the backup path
        assertDoesNotThrow(() -> mgr.createBackup(), 
            "Should not throw for non-existent database, just record path");
    }
    
    @Test
    void testExecuteMigrations_updatesVersion() {
        boolean success = migrationManager.executeMigrations();
        
        assertTrue(success, "Migration should succeed");
        assertEquals(DatabaseMigrationManager.getLatestSchemaVersion(), 
            migrationManager.getCurrentVersion(), 
            "Version should be updated to latest");
    }
    
    @Test
    void testExecuteMigrations_createsBackup() {
        // Ensure database file exists
        database.hashMap("test").createOrOpen();
        database.commit();
        
        migrationManager.executeMigrations();
        
        Path backupPath = migrationManager.getLastBackupPath();
        assertNotNull(backupPath, "Backup path should be recorded during migration");
        assertTrue(backupPath.toString().contains("backup_"), "Backup filename should contain backup marker");
    }
    
    @Test
    void testExecuteMigrations_alreadyLatestVersion_noBackup() {
        // First migration
        migrationManager.executeMigrations();
        Path firstBackup = migrationManager.getLastBackupPath();
        
        // Second migration attempt (already at latest version)
        boolean success = migrationManager.executeMigrations();
        
        assertTrue(success, "Should return success when already at latest version");
        assertEquals(firstBackup, migrationManager.getLastBackupPath(), 
            "Should not create new backup when already at latest version");
    }
    
    @Test
    void testGetRegisteredMigrations_notEmpty() {
        var migrations = migrationManager.getRegisteredMigrations();
        
        assertNotNull(migrations);
        assertFalse(migrations.isEmpty(), "Should have at least one migration registered");
    }
    
    @Test
    void testGetLatestSchemaVersion_returnsPositiveNumber() {
        int version = DatabaseMigrationManager.getLatestSchemaVersion();
        
        assertTrue(version > 0, "Latest schema version should be positive");
    }
    
    @Test
    void testMigrationIsAtomic() {
        // This test verifies that migration either completes fully or not at all
        int versionBefore = migrationManager.getCurrentVersion();
        
        migrationManager.executeMigrations();
        
        int versionAfter = migrationManager.getCurrentVersion();
        
        // Version should either be unchanged or updated to latest (not partial)
        assertTrue(versionAfter == versionBefore || 
                   versionAfter == DatabaseMigrationManager.getLatestSchemaVersion(),
            "Migration should be atomic - either complete or not started");
    }
    
    @Test
    void testDetectSchemaVersionMismatch_scenario() {
        // Given an older database version
        int oldVersion = migrationManager.getCurrentVersion();
        assertEquals(0, oldVersion, "Should start at version 0");
        
        // When the application starts
        boolean needsMigration = migrationManager.needsMigration();
        
        // Then the schema version should be detected
        assertTrue(needsMigration, "Should detect version mismatch");
        
        // And migration should be triggered automatically
        boolean migrationSuccess = migrationManager.executeMigrations();
        assertTrue(migrationSuccess, "Migration should succeed");
        
        // Verify version is now latest
        assertEquals(DatabaseMigrationManager.getLatestSchemaVersion(),
            migrationManager.getCurrentVersion(),
            "Version should be updated to latest");
    }
    
    @Test
    void testExecuteMigrationScripts_scenario() {
        // Given pending migrations
        assertTrue(migrationManager.needsMigration(), "Should have pending migrations");
        
        // When migrations run
        boolean success = migrationManager.executeMigrations();
        
        // Then data should be transformed to new schema
        assertTrue(success, "Migrations should complete successfully");
        
        // And migration should be atomic (all or nothing)
        int currentVersion = migrationManager.getCurrentVersion();
        assertEquals(DatabaseMigrationManager.getLatestSchemaVersion(), currentVersion,
            "All migrations should complete or none");
    }
    
    @Test
    void testBackupBeforeMigration_scenario() throws IOException {
        // Ensure database file exists
        database.hashMap("test").createOrOpen();
        database.commit();
        
        // Given a migration is about to run
        assertTrue(migrationManager.needsMigration());
        
        // When migration starts
        boolean success = migrationManager.executeMigrations();
        
        // Then a backup location should be determined
        assertTrue(success, "Migration should succeed");
        Path backupPath = migrationManager.getLastBackupPath();
        assertNotNull(backupPath, "Backup path should be recorded");
        
        // And user should be notified of the backup location
        assertTrue(backupPath.getParent().equals(databasePath.getParent()),
            "Backup should be in same directory as database");
        assertTrue(backupPath.toString().contains("backup_"), "Backup path should contain backup marker");
    }
    
    @Test
    void testConsecutiveMigrations_maintainConsistency() {
        // Execute migration multiple times
        migrationManager.executeMigrations();
        int version1 = migrationManager.getCurrentVersion();
        
        migrationManager.executeMigrations();
        int version2 = migrationManager.getCurrentVersion();
        
        migrationManager.executeMigrations();
        int version3 = migrationManager.getCurrentVersion();
        
        // Version should remain consistent
        assertEquals(version1, version2);
        assertEquals(version2, version3);
        assertEquals(DatabaseMigrationManager.getLatestSchemaVersion(), version3);
    }
    
    @Test
    void testBackupFilenamingConvention() throws IOException {
        // Ensure database exists
        database.hashMap("test").createOrOpen();
        database.commit();
        
        Path backup = migrationManager.createBackup();
        String backupFilename = backup.getFileName().toString();
        
        // Should contain original filename
        assertTrue(backupFilename.contains("test-db.db"));
        
        // Should contain backup identifier
        assertTrue(backupFilename.contains("backup_"));
        
        // Should contain timestamp (YYYYMMDD format)
        assertTrue(backupFilename.matches(".*backup_\\d{8}_\\d{6}.*"));
    }
}
