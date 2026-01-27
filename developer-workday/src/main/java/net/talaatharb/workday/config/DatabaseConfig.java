package net.talaatharb.workday.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration and management of MapDB database.
 * Handles database initialization, lifecycle, and file location.
 */
@Slf4j
public class DatabaseConfig {
    
    private static final String DB_FILENAME = "developer-workday.db";
    private static final String APP_NAME = "developer-workday";
    
    @Getter
    private DB database;
    
    @Getter
    private Path databasePath;
    
    /**
     * Initialize the database with default location
     */
    public void initialize() {
        databasePath = getDefaultDatabasePath();
        initializeDatabase(databasePath);
    }
    
    /**
     * Initialize the database with custom location
     */
    public void initialize(Path customPath) {
        databasePath = customPath;
        initializeDatabase(databasePath);
    }
    
    private void initializeDatabase(Path dbPath) {
        try {
            ensureDirectoryExists(dbPath.getParent());
            
            log.info("Initializing MapDB database at: {}", dbPath);
            
            database = DBMaker
                .fileDB(dbPath.toFile())
                .transactionEnable()
                .closeOnJvmShutdown()
                .make();
            
            log.info("MapDB database initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    /**
     * Get the default database path in user's application data directory
     */
    public static Path getDefaultDatabasePath() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        
        Path appDataDir;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            appDataDir = appData != null ? Paths.get(appData, APP_NAME) : Paths.get(userHome, ".developer-workday");
        } else if (os.contains("mac")) {
            appDataDir = Paths.get(userHome, "Library", "Application Support", APP_NAME);
        } else {
            appDataDir = Paths.get(userHome, ".local", "share", APP_NAME);
        }
        
        return appDataDir.resolve(DB_FILENAME);
    }
    
    private void ensureDirectoryExists(Path directory) {
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                log.debug("Created database directory: {}", directory);
            }
        } catch (Exception e) {
            log.error("Failed to create database directory: {}", directory, e);
            throw new RuntimeException("Failed to create database directory", e);
        }
    }
    
    /**
     * Close the database gracefully
     */
    public void close() {
        if (database != null && !database.isClosed()) {
            log.info("Closing MapDB database");
            try {
                database.close();
                log.info("MapDB database closed successfully");
            } catch (Exception e) {
                log.error("Error closing database", e);
            }
        }
    }
    
    /**
     * Check if database is initialized and open
     */
    public boolean isOpen() {
        return database != null && !database.isClosed();
    }
}
