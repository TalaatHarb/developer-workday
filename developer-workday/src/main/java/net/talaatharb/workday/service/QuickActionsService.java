package net.talaatharb.workday.service;

import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.model.QuickAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing quick actions (command palette).
 */
@Slf4j
public class QuickActionsService {
    
    private final List<QuickAction> actions = new ArrayList<>();
    
    /**
     * Register a quick action.
     */
    public void registerAction(QuickAction action) {
        actions.add(action);
        log.debug("Registered quick action: {}", action.getTitle());
    }
    
    /**
     * Get all actions sorted by usage count (most used first), then by title.
     */
    public List<QuickAction> getAllActions() {
        return actions.stream()
            .sorted(Comparator
                .comparing(QuickAction::getUseCount).reversed()
                .thenComparing(QuickAction::getTitle))
            .collect(Collectors.toList());
    }
    
    /**
     * Search actions by query (searches title and description).
     * @param query Search query
     * @return Filtered and sorted list of actions
     */
    public List<QuickAction> searchActions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllActions();
        }
        
        String lowerQuery = query.toLowerCase().trim();
        
        return actions.stream()
            .filter(action -> 
                action.getTitle().toLowerCase().contains(lowerQuery) ||
                (action.getDescription() != null && action.getDescription().toLowerCase().contains(lowerQuery)) ||
                (action.getCategory() != null && action.getCategory().toLowerCase().contains(lowerQuery)))
            .sorted(Comparator
                .comparing(QuickAction::getUseCount).reversed()
                .thenComparing(QuickAction::getTitle))
            .collect(Collectors.toList());
    }
    
    /**
     * Execute an action and increment its use count.
     */
    public void executeAction(QuickAction action) {
        log.info("Executing quick action: {}", action.getTitle());
        action.setUseCount(action.getUseCount() + 1);
        
        try {
            if (action.getAction() != null) {
                action.getAction().run();
            }
        } catch (Exception e) {
            log.error("Failed to execute action: {}", action.getTitle(), e);
        }
    }
    
    /**
     * Get the most recently used actions.
     * @param limit Maximum number of actions to return
     * @return List of most used actions
     */
    public List<QuickAction> getRecentActions(int limit) {
        return actions.stream()
            .filter(action -> action.getUseCount() > 0)
            .sorted(Comparator.comparing(QuickAction::getUseCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
}
