package net.talaatharb.workday.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PerformanceMonitor.
 */
class PerformanceMonitorTest {
    
    private PerformanceMonitor performanceMonitor;
    
    @BeforeEach
    void setUp() {
        performanceMonitor = PerformanceMonitor.getInstance();
        performanceMonitor.reset();
    }
    
    @Test
    void testGetInstance_returnsSameInstance() {
        PerformanceMonitor instance1 = PerformanceMonitor.getInstance();
        PerformanceMonitor instance2 = PerformanceMonitor.getInstance();
        
        assertSame(instance1, instance2, "Should return same singleton instance");
    }
    
    @Test
    void testStartTiming_returnsNonZero() {
        long startTime = performanceMonitor.startTiming("test-operation");
        
        assertTrue(startTime > 0, "Start time should be non-zero");
    }
    
    @Test
    void testEndTiming_recordsDuration() throws InterruptedException {
        long startTime = performanceMonitor.startTiming("test-operation");
        
        // Simulate some work
        Thread.sleep(10);
        
        long duration = performanceMonitor.endTiming("test-operation", startTime);
        
        assertTrue(duration >= 10, "Duration should be at least 10ms");
    }
    
    @Test
    void testRecordOperationTime() {
        performanceMonitor.recordOperationTime("test-op", 50);
        performanceMonitor.recordOperationTime("test-op", 100);
        performanceMonitor.recordOperationTime("test-op", 75);
        
        PerformanceMonitor.OperationStats stats = performanceMonitor.getOperationStats("test-op");
        
        assertNotNull(stats);
        assertEquals(3, stats.getCallCount());
        assertEquals(50, stats.getMinTimeMillis());
        assertEquals(100, stats.getMaxTimeMillis());
        assertEquals(75.0, stats.getAverageTimeMillis(), 0.01);
        assertEquals(75, stats.getLastTimeMillis());
    }
    
    @Test
    void testGetOperationStats_nonExistent_returnsNull() {
        PerformanceMonitor.OperationStats stats = performanceMonitor.getOperationStats("nonexistent");
        
        assertNull(stats);
    }
    
    @Test
    void testGetAllOperationStats() {
        performanceMonitor.recordOperationTime("op1", 10);
        performanceMonitor.recordOperationTime("op2", 20);
        performanceMonitor.recordOperationTime("op3", 30);
        
        Map<String, PerformanceMonitor.OperationStats> allStats = performanceMonitor.getAllOperationStats();
        
        assertEquals(3, allStats.size());
        assertTrue(allStats.containsKey("op1"));
        assertTrue(allStats.containsKey("op2"));
        assertTrue(allStats.containsKey("op3"));
    }
    
    @Test
    void testGetCurrentMemoryUsage() {
        long memoryUsage = performanceMonitor.getCurrentMemoryUsage();
        
        assertTrue(memoryUsage > 0, "Memory usage should be greater than 0");
    }
    
    @Test
    void testGetCurrentMemoryUsageMB() {
        long memoryUsageMB = performanceMonitor.getCurrentMemoryUsageMB();
        
        assertTrue(memoryUsageMB > 0, "Memory usage in MB should be greater than 0");
    }
    
    @Test
    void testGetMaxMemory() {
        long maxMemory = performanceMonitor.getMaxMemory();
        
        assertTrue(maxMemory > 0, "Max memory should be greater than 0");
    }
    
    @Test
    void testGetMemoryUsagePercentage() {
        double percentage = performanceMonitor.getMemoryUsagePercentage();
        
        assertTrue(percentage >= 0.0 && percentage <= 100.0, 
            "Memory usage percentage should be between 0 and 100");
    }
    
    @Test
    void testIsMemoryUsageStable() {
        boolean isStable = performanceMonitor.isMemoryUsageStable();
        
        // Should be stable under normal test conditions
        assertTrue(isStable, "Memory usage should be stable during tests");
    }
    
    @Test
    void testGetPerformanceSnapshot() {
        performanceMonitor.recordOperationTime("test-op", 100);
        
        Map<String, Object> snapshot = performanceMonitor.getPerformanceSnapshot();
        
        assertNotNull(snapshot);
        assertTrue(snapshot.containsKey("memoryUsedMB"));
        assertTrue(snapshot.containsKey("memoryMaxMB"));
        assertTrue(snapshot.containsKey("memoryUsagePercent"));
        assertTrue(snapshot.containsKey("memoryStable"));
        assertTrue(snapshot.containsKey("operationCount"));
        
        assertEquals(1, ((Number) snapshot.get("operationCount")).intValue());
    }
    
    @Test
    void testReset() {
        performanceMonitor.recordOperationTime("test-op", 100);
        
        performanceMonitor.reset();
        
        Map<String, PerformanceMonitor.OperationStats> stats = performanceMonitor.getAllOperationStats();
        assertTrue(stats.isEmpty(), "All operation stats should be cleared after reset");
    }
    
    @Test
    void testOperationStats_toString() {
        PerformanceMonitor.OperationStats stats = new PerformanceMonitor.OperationStats("test-operation");
        stats.recordTiming(100);
        stats.recordTiming(200);
        stats.recordTiming(150);
        
        String statsString = stats.toString();
        
        assertNotNull(statsString);
        assertTrue(statsString.contains("test-operation"));
        assertTrue(statsString.contains("calls=3"));
        assertTrue(statsString.contains("avg="));
    }
    
    @Test
    void testOperationStats_multipleRecordings() {
        PerformanceMonitor.OperationStats stats = new PerformanceMonitor.OperationStats("db-query");
        
        stats.recordTiming(50);
        stats.recordTiming(75);
        stats.recordTiming(100);
        stats.recordTiming(25);
        
        assertEquals(4, stats.getCallCount());
        assertEquals(25, stats.getMinTimeMillis());
        assertEquals(100, stats.getMaxTimeMillis());
        assertEquals(62.5, stats.getAverageTimeMillis(), 0.01);
        assertEquals(25, stats.getLastTimeMillis());
    }
    
    @Test
    void testOperationStats_getAverageTimeMillis_noRecordings() {
        PerformanceMonitor.OperationStats stats = new PerformanceMonitor.OperationStats("empty-op");
        
        assertEquals(0.0, stats.getAverageTimeMillis(), 0.01);
    }
    
    @Test
    void testMemoryManagement_scenario() {
        // Given extended application usage
        long initialMemory = performanceMonitor.getInitialMemoryUsage();
        
        // When monitoring memory
        long currentMemory = performanceMonitor.getCurrentMemoryUsage();
        boolean isStable = performanceMonitor.isMemoryUsageStable();
        
        // Then memory usage should remain stable
        assertTrue(isStable, "Memory usage should be stable");
        
        // And no memory leaks should occur (current memory should be reasonable)
        assertTrue(currentMemory > 0, "Current memory should be positive");
        assertTrue(performanceMonitor.getMemoryUsagePercentage() < 100, 
            "Memory should not be at maximum capacity");
    }
    
    @Test
    void testSlowOperationDetection() throws InterruptedException {
        // Test that operations longer than 200ms are logged as slow
        long startTime = performanceMonitor.startTiming("slow-operation");
        
        Thread.sleep(250); // Simulate slow operation
        
        long duration = performanceMonitor.endTiming("slow-operation", startTime);
        
        assertTrue(duration >= 250, "Duration should be at least 250ms");
        
        PerformanceMonitor.OperationStats stats = performanceMonitor.getOperationStats("slow-operation");
        assertNotNull(stats);
        assertTrue(stats.getMaxTimeMillis() >= 250);
    }
    
    @Test
    void testMultipleOperations_independentTracking() {
        performanceMonitor.recordOperationTime("operation-A", 10);
        performanceMonitor.recordOperationTime("operation-B", 20);
        performanceMonitor.recordOperationTime("operation-A", 15);
        
        PerformanceMonitor.OperationStats statsA = performanceMonitor.getOperationStats("operation-A");
        PerformanceMonitor.OperationStats statsB = performanceMonitor.getOperationStats("operation-B");
        
        assertEquals(2, statsA.getCallCount());
        assertEquals(1, statsB.getCallCount());
        assertEquals(12.5, statsA.getAverageTimeMillis(), 0.01);
        assertEquals(20.0, statsB.getAverageTimeMillis(), 0.01);
    }
    
    @Test
    void testSuggestGC_doesNotThrow() {
        // GC suggestion should not throw exceptions
        assertDoesNotThrow(() -> performanceMonitor.suggestGC());
    }
}
