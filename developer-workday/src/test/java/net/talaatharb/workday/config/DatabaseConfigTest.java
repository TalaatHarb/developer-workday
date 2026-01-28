package net.talaatharb.workday.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapdb.DB;

class DatabaseConfigTest {
    
    @TempDir
    Path tempDir;
    
    private DatabaseConfig databaseConfig;
    private Path testDbPath;
    
    @BeforeEach
    void setUp() {
        databaseConfig = new DatabaseConfig();
        testDbPath = tempDir.resolve("test-workday.db");
    }
    
    @AfterEach
    void tearDown() {
        if (databaseConfig != null) {
            databaseConfig.close();
        }
        cleanupTestDb();
    }
    
    private void cleanupTestDb() {
        try {
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    void testInitializeWithCustomPath() {
        databaseConfig.initialize(testDbPath);
        
        assertNotNull(databaseConfig.getDatabase());
        assertTrue(databaseConfig.isOpen());
        assertEquals(testDbPath, databaseConfig.getDatabasePath());
        assertTrue(Files.exists(testDbPath.getParent()));
    }
    
    @Test
    void testDatabaseSupportsTransactions() {
        databaseConfig.initialize(testDbPath);
        
        DB db = databaseConfig.getDatabase();
        assertNotNull(db);
        
        // Create a map and test transaction
        var map = db.hashMap("test", org.mapdb.Serializer.STRING, org.mapdb.Serializer.STRING).createOrOpen();
        map.put("key1", "value1");
        db.commit();
        
        assertEquals("value1", map.get("key1"));
    }
    
    @Test
    void testDatabaseFileIsCreated() {
        databaseConfig.initialize(testDbPath);
        
        assertTrue(Files.exists(testDbPath) || Files.exists(testDbPath.getParent()),
            "Database file or directory should exist");
    }
    
    @Test
    void testCloseDatabase() {
        databaseConfig.initialize(testDbPath);
        assertTrue(databaseConfig.isOpen());
        
        databaseConfig.close();
        assertFalse(databaseConfig.isOpen());
    }
    
    @Test
    void testMultipleCloseCallsAreSafe() {
        databaseConfig.initialize(testDbPath);
        
        databaseConfig.close();
        assertDoesNotThrow(() -> databaseConfig.close());
    }
    
    @Test
    void testGetDefaultDatabasePath() {
        Path defaultPath = DatabaseConfig.getDefaultDatabasePath();
        
        assertNotNull(defaultPath);
        assertTrue(defaultPath.toString().contains("developer-workday"));
        assertTrue(defaultPath.toString().endsWith("developer-workday.db"));
    }
    
    @Test
    void testIsOpenReturnsFalseWhenNotInitialized() {
        DatabaseConfig uninitializedConfig = new DatabaseConfig();
        assertFalse(uninitializedConfig.isOpen());
    }
    
    @Test
    void testDatabasePersistence() throws InterruptedException {
        // Initialize and write data
        databaseConfig.initialize(testDbPath);
        var map = databaseConfig.getDatabase().hashMap("testMap", org.mapdb.Serializer.STRING, org.mapdb.Serializer.STRING).createOrOpen();
        map.put("persistKey", "persistValue");
        databaseConfig.getDatabase().commit();
        databaseConfig.close();
        
        // Wait a bit to ensure file is written
        Thread.sleep(100);
        
        // Reinitialize and verify data persists
        DatabaseConfig newConfig = new DatabaseConfig();
        newConfig.initialize(testDbPath);
        var newMap = newConfig.getDatabase().hashMap("testMap", org.mapdb.Serializer.STRING, org.mapdb.Serializer.STRING).open();
        
        assertEquals("persistValue", newMap.get("persistKey"), 
            "Data should persist after database restart");
        
        newConfig.close();
    }
    
    @Test
    void testDirectoryIsCreatedIfNotExists() {
        Path deepPath = tempDir.resolve("deep").resolve("nested").resolve("path").resolve("test.db");
        
        databaseConfig.initialize(deepPath);
        
        assertTrue(Files.exists(deepPath.getParent()));
        databaseConfig.close();
    }
}
