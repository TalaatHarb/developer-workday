package net.talaatharb.workday.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for viewing application logs.
 */
@Slf4j
public class LogViewer {
    
    private static final String LOG_DIR = System.getProperty("user.home") + "/.developer-workday/logs";
    private static final String APPLICATION_LOG = "application.log";
    private static final String ERROR_LOG = "error.log";
    
    /**
     * Get the path to the log directory
     */
    public static Path getLogDirectory() {
        return Paths.get(LOG_DIR);
    }
    
    /**
     * Get the path to the main application log file
     */
    public static Path getApplicationLogFile() {
        return Paths.get(LOG_DIR, APPLICATION_LOG);
    }
    
    /**
     * Get the path to the error log file
     */
    public static Path getErrorLogFile() {
        return Paths.get(LOG_DIR, ERROR_LOG);
    }
    
    /**
     * Read the last N lines from the application log
     * 
     * @param lineCount number of lines to read
     * @return list of log lines (most recent first)
     */
    public static List<String> readRecentLogs(int lineCount) {
        return readLastLines(getApplicationLogFile(), lineCount);
    }
    
    /**
     * Read the last N lines from the error log
     * 
     * @param lineCount number of lines to read
     * @return list of error log lines (most recent first)
     */
    public static List<String> readRecentErrors(int lineCount) {
        return readLastLines(getErrorLogFile(), lineCount);
    }
    
    /**
     * Read all lines from the application log
     * 
     * @return list of all log lines
     */
    public static List<String> readAllLogs() {
        try {
            Path logFile = getApplicationLogFile();
            if (!Files.exists(logFile)) {
                log.debug("Log file does not exist: {}", logFile);
                return Collections.emptyList();
            }
            
            return Files.readAllLines(logFile);
        } catch (IOException e) {
            log.error("Failed to read application log", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Read the last N lines from a log file
     */
    private static List<String> readLastLines(Path logFile, int lineCount) {
        try {
            if (!Files.exists(logFile)) {
                log.debug("Log file does not exist: {}", logFile);
                return Collections.emptyList();
            }
            
            List<String> allLines = Files.readAllLines(logFile);
            int startIndex = Math.max(0, allLines.size() - lineCount);
            List<String> lastLines = new ArrayList<>(allLines.subList(startIndex, allLines.size()));
            Collections.reverse(lastLines); // Most recent first
            return lastLines;
        } catch (IOException e) {
            log.error("Failed to read log file: {}", logFile, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Search logs for a specific keyword
     * 
     * @param keyword the keyword to search for
     * @return list of matching log lines
     */
    public static List<String> searchLogs(String keyword) {
        try {
            Path logFile = getApplicationLogFile();
            if (!Files.exists(logFile)) {
                return Collections.emptyList();
            }
            
            return Files.readAllLines(logFile).stream()
                .filter(line -> line.toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to search logs for keyword: {}", keyword, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Open the log directory in the system file explorer
     */
    public static void openLogDirectory() {
        try {
            File logDir = getLogDirectory().toFile();
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(logDir);
            } else {
                log.warn("Desktop operations not supported on this system");
            }
        } catch (IOException e) {
            log.error("Failed to open log directory", e);
        }
    }
    
    /**
     * Clear old log files (older than specified days)
     * 
     * @param daysToKeep number of days of logs to keep
     */
    public static void clearOldLogs(int daysToKeep) {
        try {
            Path logDir = getLogDirectory();
            if (!Files.exists(logDir)) {
                return;
            }
            
            long cutoffMillis = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000);
            
            Files.list(logDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffMillis;
                    } catch (IOException e) {
                        log.error("Error checking file modification time: {}", path, e);
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        log.info("Deleted old log file: {}", path);
                    } catch (IOException e) {
                        log.error("Failed to delete old log file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to clear old logs", e);
        }
    }
}
