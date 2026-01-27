package net.talaatharb.workday.utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.Task;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages search indexing for fast task searches in large databases.
 * Uses inverted index pattern for O(1) lookups by words.
 */
@Slf4j
public class SearchIndexManager {
    
    private static SearchIndexManager instance;
    
    // Inverted index: word -> set of task IDs containing that word
    private final Map<String, Set<UUID>> wordIndex = new ConcurrentHashMap<>();
    
    // Task ID -> task mapping for quick retrieval
    private final Map<UUID, Task> taskCache = new ConcurrentHashMap<>();
    
    // Track indexing statistics
    @Getter
    private long indexedTaskCount = 0;
    
    @Getter
    private long lastIndexTime = 0;
    
    private SearchIndexManager() {
        log.info("SearchIndexManager initialized");
    }
    
    /**
     * Get singleton instance.
     */
    public static synchronized SearchIndexManager getInstance() {
        if (instance == null) {
            instance = new SearchIndexManager();
        }
        return instance;
    }
    
    /**
     * Index a single task for searching.
     * 
     * @param task the task to index
     */
    public void indexTask(Task task) {
        if (task == null || task.getId() == null) {
            return;
        }
        
        // Remove old indexing for this task if it exists
        removeFromIndex(task.getId());
        
        // Add to cache
        taskCache.put(task.getId(), task);
        
        // Extract and index words from title and description
        Set<String> words = extractWords(task);
        for (String word : words) {
            wordIndex.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(task.getId());
        }
        
        indexedTaskCount++;
        log.debug("Indexed task: {} with {} words", task.getId(), words.size());
    }
    
    /**
     * Index multiple tasks at once.
     * 
     * @param tasks the tasks to index
     */
    public void indexTasks(List<Task> tasks) {
        long startTime = System.currentTimeMillis();
        
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        
        for (Task task : tasks) {
            indexTask(task);
        }
        
        lastIndexTime = System.currentTimeMillis() - startTime;
        log.info("Indexed {} tasks in {}ms", tasks.size(), lastIndexTime);
    }
    
    /**
     * Remove a task from the index.
     * 
     * @param taskId the task ID to remove
     */
    public void removeFromIndex(UUID taskId) {
        if (taskId == null) {
            return;
        }
        
        // Remove from cache
        Task removed = taskCache.remove(taskId);
        
        if (removed != null) {
            // Remove from word index
            Set<String> words = extractWords(removed);
            for (String word : words) {
                Set<UUID> taskIds = wordIndex.get(word);
                if (taskIds != null) {
                    taskIds.remove(taskId);
                    if (taskIds.isEmpty()) {
                        wordIndex.remove(word);
                    }
                }
            }
            indexedTaskCount--;
        }
    }
    
    /**
     * Search for tasks matching the query string.
     * Uses indexed search for O(1) word lookups.
     * 
     * @param query the search query
     * @return list of matching tasks
     */
    public List<Task> search(String query) {
        long startTime = System.currentTimeMillis();
        
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(taskCache.values());
        }
        
        // Extract search terms
        Set<String> searchTerms = normalizeWords(query);
        
        if (searchTerms.isEmpty()) {
            return new ArrayList<>(taskCache.values());
        }
        
        // Find tasks containing any of the search terms
        Set<UUID> matchingTaskIds = new HashSet<>();
        for (String term : searchTerms) {
            Set<UUID> taskIds = wordIndex.get(term);
            if (taskIds != null) {
                matchingTaskIds.addAll(taskIds);
            }
        }
        
        // Retrieve matching tasks from cache
        List<Task> results = matchingTaskIds.stream()
            .map(taskCache::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        long searchTime = System.currentTimeMillis() - startTime;
        log.debug("Search for '{}' completed in {}ms, found {} results", query, searchTime, results.size());
        
        return results;
    }
    
    /**
     * Rebuild the entire index from scratch.
     * 
     * @param tasks all tasks to index
     */
    public void rebuildIndex(List<Task> tasks) {
        long startTime = System.currentTimeMillis();
        
        clearIndex();
        indexTasks(tasks);
        
        long rebuildTime = System.currentTimeMillis() - startTime;
        log.info("Rebuilt search index with {} tasks in {}ms", tasks.size(), rebuildTime);
    }
    
    /**
     * Clear the entire index.
     */
    public void clearIndex() {
        wordIndex.clear();
        taskCache.clear();
        indexedTaskCount = 0;
        log.info("Search index cleared");
    }
    
    /**
     * Extract searchable words from a task.
     */
    private Set<String> extractWords(Task task) {
        Set<String> words = new HashSet<>();
        
        // Add words from title
        if (task.getTitle() != null) {
            words.addAll(normalizeWords(task.getTitle()));
        }
        
        // Add words from description
        if (task.getDescription() != null) {
            words.addAll(normalizeWords(task.getDescription()));
        }
        
        // Add tags as-is (case-insensitive)
        if (task.getTags() != null) {
            task.getTags().stream()
                .map(String::toLowerCase)
                .forEach(words::add);
        }
        
        return words;
    }
    
    /**
     * Normalize and tokenize text into searchable words.
     */
    private Set<String> normalizeWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }
        
        // Convert to lowercase, split by non-word characters, and filter short words
        return Arrays.stream(text.toLowerCase().split("[\\W_]+"))
            .filter(word -> word.length() >= 2) // Ignore single characters
            .collect(Collectors.toSet());
    }
    
    /**
     * Get index statistics.
     * 
     * @return map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("indexedTaskCount", indexedTaskCount);
        stats.put("uniqueWords", wordIndex.size());
        stats.put("cachedTasks", taskCache.size());
        stats.put("lastIndexTimeMs", lastIndexTime);
        return stats;
    }
    
    /**
     * Check if a task is indexed.
     * 
     * @param taskId the task ID to check
     * @return true if indexed, false otherwise
     */
    public boolean isIndexed(UUID taskId) {
        return taskCache.containsKey(taskId);
    }
}
