package net.talaatharb.workday.utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitors application performance including memory usage, operation timings,
 * and provides utilities to detect performance issues.
 */
@Slf4j
public class PerformanceMonitor {
    
    private static PerformanceMonitor instance;
    
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    // Track operation timings
    private final Map<String, OperationStats> operationStats = new ConcurrentHashMap<>();
    
    // Memory usage tracking
    @Getter
    private long initialMemoryUsage = 0;
    
    private PerformanceMonitor() {
        initialMemoryUsage = getCurrentMemoryUsage();
        log.info("PerformanceMonitor initialized. Initial memory: {} MB", initialMemoryUsage / (1024 * 1024));
    }
    
    /**
     * Get singleton instance.
     */
    public static synchronized PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }
    
    /**
     * Start timing an operation.
     * 
     * @param operationName the name of the operation
     * @return start time in nanoseconds
     */
    public long startTiming(String operationName) {
        return System.nanoTime();
    }
    
    /**
     * End timing an operation and record the duration.
     * 
     * @param operationName the name of the operation
     * @param startTime the start time from startTiming()
     * @return duration in milliseconds
     */
    public long endTiming(String operationName, long startTime) {
        long endTime = System.nanoTime();
        long durationNanos = endTime - startTime;
        long durationMillis = durationNanos / 1_000_000;
        
        recordOperationTime(operationName, durationMillis);
        
        if (durationMillis > 200) {
            log.warn("Slow operation detected: {} took {}ms", operationName, durationMillis);
        }
        
        return durationMillis;
    }
    
    /**
     * Record an operation timing.
     * 
     * @param operationName the operation name
     * @param durationMillis duration in milliseconds
     */
    public void recordOperationTime(String operationName, long durationMillis) {
        operationStats.computeIfAbsent(operationName, k -> new OperationStats(operationName))
            .recordTiming(durationMillis);
    }
    
    /**
     * Get statistics for a specific operation.
     * 
     * @param operationName the operation name
     * @return operation statistics or null if not found
     */
    public OperationStats getOperationStats(String operationName) {
        return operationStats.get(operationName);
    }
    
    /**
     * Get all operation statistics.
     * 
     * @return map of operation name to statistics
     */
    public Map<String, OperationStats> getAllOperationStats() {
        return new HashMap<>(operationStats);
    }
    
    /**
     * Get current memory usage in bytes.
     * 
     * @return memory usage in bytes
     */
    public long getCurrentMemoryUsage() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getUsed();
    }
    
    /**
     * Get current memory usage in megabytes.
     * 
     * @return memory usage in MB
     */
    public long getCurrentMemoryUsageMB() {
        return getCurrentMemoryUsage() / (1024 * 1024);
    }
    
    /**
     * Get maximum available memory in bytes.
     * 
     * @return max memory in bytes
     */
    public long getMaxMemory() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getMax();
    }
    
    /**
     * Get memory usage percentage.
     * 
     * @return percentage of memory used (0-100)
     */
    public double getMemoryUsagePercentage() {
        long used = getCurrentMemoryUsage();
        long max = getMaxMemory();
        return max > 0 ? (used * 100.0 / max) : 0.0;
    }
    
    /**
     * Check if memory usage is stable (not growing unexpectedly).
     * 
     * @return true if stable, false if potential memory leak detected
     */
    public boolean isMemoryUsageStable() {
        long currentUsage = getCurrentMemoryUsage();
        double usagePercentage = getMemoryUsagePercentage();
        
        // Consider memory usage unstable if using more than 80% of max
        if (usagePercentage > 80) {
            log.warn("High memory usage detected: {:.2f}%", usagePercentage);
            return false;
        }
        
        return true;
    }
    
    /**
     * Suggest garbage collection (does not force it).
     */
    public void suggestGC() {
        long beforeGC = getCurrentMemoryUsageMB();
        System.gc();
        long afterGC = getCurrentMemoryUsageMB();
        long freed = beforeGC - afterGC;
        
        log.info("GC suggested. Memory before: {}MB, after: {}MB, freed: {}MB", 
            beforeGC, afterGC, freed);
    }
    
    /**
     * Get a snapshot of current performance metrics.
     * 
     * @return map of performance metrics
     */
    public Map<String, Object> getPerformanceSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        
        snapshot.put("memoryUsedMB", getCurrentMemoryUsageMB());
        snapshot.put("memoryMaxMB", getMaxMemory() / (1024 * 1024));
        snapshot.put("memoryUsagePercent", String.format("%.2f", getMemoryUsagePercentage()));
        snapshot.put("memoryStable", isMemoryUsageStable());
        snapshot.put("operationCount", operationStats.size());
        
        return snapshot;
    }
    
    /**
     * Reset all statistics.
     */
    public void reset() {
        operationStats.clear();
        initialMemoryUsage = getCurrentMemoryUsage();
        log.info("PerformanceMonitor reset");
    }
    
    /**
     * Statistics for a specific operation.
     */
    @Getter
    public static class OperationStats {
        private final String operationName;
        private long callCount = 0;
        private long totalTimeMillis = 0;
        private long minTimeMillis = Long.MAX_VALUE;
        private long maxTimeMillis = 0;
        private long lastTimeMillis = 0;
        
        public OperationStats(String operationName) {
            this.operationName = operationName;
        }
        
        public synchronized void recordTiming(long durationMillis) {
            callCount++;
            totalTimeMillis += durationMillis;
            minTimeMillis = Math.min(minTimeMillis, durationMillis);
            maxTimeMillis = Math.max(maxTimeMillis, durationMillis);
            lastTimeMillis = durationMillis;
        }
        
        public double getAverageTimeMillis() {
            return callCount > 0 ? (double) totalTimeMillis / callCount : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("%s: calls=%d, avg=%.2fms, min=%dms, max=%dms, last=%dms",
                operationName, callCount, getAverageTimeMillis(), 
                minTimeMillis == Long.MAX_VALUE ? 0 : minTimeMillis, 
                maxTimeMillis, lastTimeMillis);
        }
    }
}
