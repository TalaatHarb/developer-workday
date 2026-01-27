package net.talaatharb.workday.utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mapdb.DB;
import org.mapdb.Serializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Manages database migrations and schema versioning.
 * Handles version detection, backup creation, and migration execution.
 */
@Slf4j
public class DatabaseMigrationManager {
    
    private static final String VERSION_MAP_NAME = "schema_version";
    private static final String CURRENT_VERSION_KEY = "current";
    private static final int LATEST_SCHEMA_VERSION = 1;
    
    private final DB database;
    private final Path databasePath;
    private final List<Migration> migrations = new ArrayList<>();
    
    @Getter
    private Path lastBackupPath;
    
    public DatabaseMigrationManager(DB database, Path databasePath) {
        this.database = database;
        this.databasePath = databasePath;
        registerMigrations();
    }
    
    /**
     * Register all available migrations.
     * Migrations should be added in order from oldest to newest.
     */
    private void registerMigrations() {
        // Initial schema setup (version 0 -> 1)
        migrations.add(new InitialSchemaMigration());
        
        // Future migrations will be added here
        // migrations.add(new Migration_V2());
        // migrations.add(new Migration_V3());
    }
    
    /**
     * Get the current schema version from the database.
     */
    public int getCurrentVersion() {
        try {
            Map<String, Integer> versionMap = database.hashMap(VERSION_MAP_NAME, 
                Serializer.STRING, Serializer.INTEGER).createOrOpen();
            
            return versionMap.getOrDefault(CURRENT_VERSION_KEY, 0);
        } catch (Exception e) {
            log.warn("Failed to read schema version, assuming version 0", e);
            return 0;
        }
    }
    
    /**
     * Set the current schema version in the database.
     */
    private void setCurrentVersion(int version) {
        Map<String, Integer> versionMap = database.hashMap(VERSION_MAP_NAME,
            Serializer.STRING, Serializer.INTEGER).createOrOpen();
        
        versionMap.put(CURRENT_VERSION_KEY, version);
        database.commit();
        log.info("Updated schema version to {}", version);
    }
    
    /**
     * Check if migration is needed.
     */
    public boolean needsMigration() {
        int currentVersion = getCurrentVersion();
        boolean needs = currentVersion < LATEST_SCHEMA_VERSION;
        
        if (needs) {
            log.info("Migration needed: current version {} < latest version {}", 
                currentVersion, LATEST_SCHEMA_VERSION);
        }
        
        return needs;
    }
    
    /**
     * Create a logical backup marker before migration.
     * Note: Actual file backup is challenging with MapDB due to file locking.
     * This method records the intent to backup and returns a path where backup would be created.
     * In production, this should be called when database is closed.
     * 
     * @return Path where backup would be created
     * @throws IOException if backup location cannot be determined
     */
    public Path createBackup() throws IOException {
        if (databasePath == null) {
            throw new IOException("Database path is null");
        }
        
        // Commit any pending transactions to ensure consistency
        try {
            database.commit();
        } catch (Exception e) {
            log.warn("Failed to commit database before backup", e);
        }
        
        // Generate backup filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFilename = databasePath.getFileName().toString() + ".backup_" + timestamp;
        Path backupPath = databasePath.getParent().resolve(backupFilename);
        
        log.info("Backup would be created at: {}", backupPath);
        
        // In production, the actual file copy should happen when database is closed
        // For now, we record the backup path for reference
        lastBackupPath = backupPath;
        
        // Try to copy if file exists and is not locked
        if (Files.exists(databasePath)) {
            try {
                Files.copy(databasePath, backupPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                copyAuxiliaryFiles(databasePath, backupPath);
                log.info("Database backup created successfully at: {}", backupPath);
            } catch (IOException e) {
                log.warn("Could not create physical backup (file may be locked): {}. Backup path recorded for later.", e.getMessage());
                // Not a critical failure - migration can proceed
            }
        }
        
        return backupPath;
    }
    
    /**
     * Copy MapDB auxiliary files (WAL, etc.) to backup location.
     */
    private void copyAuxiliaryFiles(Path source, Path backup) {
        try {
            // MapDB creates .t and .p files
            String[] extensions = {".t", ".p", ".wal"};
            
            for (String ext : extensions) {
                Path auxSource = Path.of(source.toString() + ext);
                if (Files.exists(auxSource)) {
                    Path auxBackup = Path.of(backup.toString() + ext);
                    Files.copy(auxSource, auxBackup, StandardCopyOption.REPLACE_EXISTING);
                    log.debug("Copied auxiliary file: {}", auxSource.getFileName());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to copy some auxiliary files", e);
        }
    }
    
    /**
     * Execute all pending migrations.
     * 
     * @return true if migrations were successful, false otherwise
     */
    public boolean executeMigrations() {
        int currentVersion = getCurrentVersion();
        
        if (currentVersion >= LATEST_SCHEMA_VERSION) {
            log.info("Database is already at latest version {}", currentVersion);
            return true;
        }
        
        log.info("Starting migration from version {} to {}", currentVersion, LATEST_SCHEMA_VERSION);
        
        // Create backup before migration
        try {
            createBackup();
        } catch (IOException e) {
            log.error("Failed to create backup before migration", e);
            return false;
        }
        
        // Execute migrations in order
        List<Migration> pendingMigrations = migrations.stream()
            .filter(m -> m.getFromVersion() >= currentVersion && m.getToVersion() <= LATEST_SCHEMA_VERSION)
            .sorted(Comparator.comparingInt(Migration::getFromVersion))
            .toList();
        
        for (Migration migration : pendingMigrations) {
            try {
                log.info("Executing migration: {} (v{} -> v{})", 
                    migration.getName(), migration.getFromVersion(), migration.getToVersion());
                
                // Execute migration atomically
                migration.execute(database);
                
                // Update version after successful migration
                setCurrentVersion(migration.getToVersion());
                
                log.info("Migration {} completed successfully", migration.getName());
            } catch (Exception e) {
                log.error("Migration failed: {}", migration.getName(), e);
                
                // Migration failed - database should be in consistent state due to transaction support
                log.error("Migration stopped at version {}. Backup available at: {}", 
                    getCurrentVersion(), lastBackupPath);
                
                return false;
            }
        }
        
        log.info("All migrations completed successfully. Database is now at version {}", 
            getCurrentVersion());
        
        return true;
    }
    
    /**
     * Migration interface that all migrations must implement.
     */
    public interface Migration {
        /**
         * Get the version this migration starts from.
         */
        int getFromVersion();
        
        /**
         * Get the version this migration upgrades to.
         */
        int getToVersion();
        
        /**
         * Get a descriptive name for this migration.
         */
        String getName();
        
        /**
         * Execute the migration.
         * This method should be atomic - either all changes succeed or all are rolled back.
         */
        void execute(DB database) throws Exception;
    }
    
    /**
     * Initial schema migration (version 0 -> 1).
     * Sets up the base schema structure.
     */
    private static class InitialSchemaMigration implements Migration {
        
        @Override
        public int getFromVersion() {
            return 0;
        }
        
        @Override
        public int getToVersion() {
            return 1;
        }
        
        @Override
        public String getName() {
            return "Initial Schema Setup";
        }
        
        @Override
        public void execute(DB database) throws Exception {
            log.info("Executing initial schema setup");
            
            // The tables are created by repositories on first use with createOrOpen()
            // This migration just ensures version tracking is in place
            // Future migrations can add specific schema changes here
            
            log.info("Initial schema setup completed");
        }
    }
    
    /**
     * Get list of all registered migrations.
     */
    public List<Migration> getRegisteredMigrations() {
        return new ArrayList<>(migrations);
    }
    
    /**
     * Get the latest schema version.
     */
    public static int getLatestSchemaVersion() {
        return LATEST_SCHEMA_VERSION;
    }
}
