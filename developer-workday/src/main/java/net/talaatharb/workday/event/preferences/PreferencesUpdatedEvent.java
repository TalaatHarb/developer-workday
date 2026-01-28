package net.talaatharb.workday.event.preferences;

import lombok.Getter;
import net.talaatharb.workday.event.Event;
import net.talaatharb.workday.model.UserPreferences;

/**
 * Event published when user preferences are updated.
 */
@Getter
public class PreferencesUpdatedEvent extends Event {
    private final UserPreferences oldPreferences;
    private final UserPreferences newPreferences;
    
    public PreferencesUpdatedEvent(UserPreferences oldPreferences, UserPreferences newPreferences) {
        super();
        this.oldPreferences = oldPreferences;
        this.newPreferences = newPreferences;
    }
    
    @Override
    public String getEventDetails() {
        return "User preferences updated";
    }
}
