package net.talaatharb.workday.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Simple in-memory event logger for audit purposes.
 * In a production system, this would write to a persistent store.
 */
@Slf4j
public class EventLogger {
    private final List<Event> eventLog = Collections.synchronizedList(new ArrayList<>());
    private final int maxEvents;
    
    public EventLogger() {
        this(1000); // Default max 1000 events
    }
    
    public EventLogger(int maxEvents) {
        this.maxEvents = maxEvents;
    }
    
    /**
     * Log an event to the persistent store
     */
    public void logEvent(Event event) {
        log.info("Logging event: {} - {}", event.getEventType(), event.getEventDetails());
        
        synchronized (eventLog) {
            eventLog.add(event);
            
            // Keep only the most recent events
            if (eventLog.size() > maxEvents) {
                eventLog.remove(0);
            }
        }
    }
    
    /**
     * Get all logged events
     */
    public List<Event> getAllEvents() {
        synchronized (eventLog) {
            return new ArrayList<>(eventLog);
        }
    }
    
    /**
     * Get events of a specific type
     */
    public <T extends Event> List<T> getEventsByType(Class<T> eventType) {
        List<T> result = new ArrayList<>();
        synchronized (eventLog) {
            for (Event event : eventLog) {
                if (eventType.isInstance(event)) {
                    result.add(eventType.cast(event));
                }
            }
        }
        return result;
    }
    
    /**
     * Clear all logged events
     */
    public void clearLog() {
        synchronized (eventLog) {
            eventLog.clear();
        }
    }
    
    /**
     * Get the number of logged events
     */
    public int getEventCount() {
        synchronized (eventLog) {
            return eventLog.size();
        }
    }
}
