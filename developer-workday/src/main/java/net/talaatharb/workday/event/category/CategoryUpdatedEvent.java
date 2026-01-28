package net.talaatharb.workday.event.category;

import lombok.Getter;
import net.talaatharb.workday.event.Event;
import net.talaatharb.workday.model.Category;

/**
 * Event published when a category is updated.
 * Contains both the old and new states of the category.
 */
@Getter
public class CategoryUpdatedEvent extends Event {
    private final Category oldCategory;
    private final Category newCategory;
    
    public CategoryUpdatedEvent(Category oldCategory, Category newCategory) {
        super();
        this.oldCategory = oldCategory;
        this.newCategory = newCategory;
    }
    
    @Override
    public String getEventDetails() {
        return String.format("Category updated: %s (ID: %s)", 
            newCategory.getName(), newCategory.getId());
    }
}
