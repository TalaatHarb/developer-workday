package net.talaatharb.workday.event.category;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import net.talaatharb.workday.event.Event;
import net.talaatharb.workday.model.Category;

/**
 * Event published when a category is deleted.
 * Contains information about affected tasks that need reassignment.
 */
@Getter
public class CategoryDeletedEvent extends Event {
    private final Category category;
    private final List<UUID> affectedTaskIds;
    
    public CategoryDeletedEvent(Category category, List<UUID> affectedTaskIds) {
        super();
        this.category = category;
        this.affectedTaskIds = affectedTaskIds;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Category deleted: %s (ID: %s), affecting %d tasks", 
            category.getName(), category.getId(), affectedTaskIds.size());
    }
}
