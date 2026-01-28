package net.talaatharb.workday.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a quick action that can be executed from the command palette.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickAction {
    private String id;
    private String title;
    private String description;
    private String category;  // e.g., "Tasks", "Views", "Settings"
    private Runnable action;  // The action to execute
    private int useCount;     // Track usage for ranking
}
