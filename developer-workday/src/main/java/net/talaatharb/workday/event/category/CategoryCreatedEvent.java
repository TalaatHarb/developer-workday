package net.talaatharb.workday.event.category;

import lombok.Getter;
import net.talaatharb.workday.event.Event;
import net.talaatharb.workday.model.Category;

/**
 * Event published when a new category is created.
 */
@Getter
public class CategoryCreatedEvent extends Event {
    private final Category category;
    
    public CategoryCreatedEvent(Category category) {
        super();
        this.category = category;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Category created: %s (ID: %s)", 
            category.getName(), category.getId());
    }
}
