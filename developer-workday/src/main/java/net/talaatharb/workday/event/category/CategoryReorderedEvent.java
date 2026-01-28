package net.talaatharb.workday.event.category;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import net.talaatharb.workday.event.Event;

/**
 * Event published when categories are reordered.
 * Contains the new order of category IDs.
 */
@Getter
public class CategoryReorderedEvent extends Event {
    private final List<UUID> categoryIds;
    
    public CategoryReorderedEvent(List<UUID> categoryIds) {
        super();
        this.categoryIds = categoryIds;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Categories reordered: %d categories", categoryIds.size());
    }
}
