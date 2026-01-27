package net.talaatharb.workday.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 * Event dispatcher for publishing and subscribing to domain events.
 * Thread-safe implementation using concurrent collections.
 */
@Slf4j
public class EventDispatcher {
    private final Map<Class<? extends Event>, List<EventListener<? extends Event>>> listeners = new ConcurrentHashMap<>();
    private final EventLogger eventLogger;
    
    public EventDispatcher(EventLogger eventLogger) {
        this.eventLogger = eventLogger;
    }
    
    /**
     * Subscribe a listener to a specific event type
     */
    public <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener) {
        log.debug("Subscribing listener to event type: {}", eventType.getSimpleName());
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }
    
    /**
     * Unsubscribe a listener from a specific event type
     */
    public <T extends Event> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        log.debug("Unsubscribing listener from event type: {}", eventType.getSimpleName());
        List<EventListener<? extends Event>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }
    
    /**
     * Publish an event to all registered listeners
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> void publish(T event) {
        log.debug("Publishing event: {}", event);
        
        // Log the event
        if (eventLogger != null) {
            eventLogger.logEvent(event);
        }
        
        // Notify all listeners for this event type
        List<EventListener<? extends Event>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener<? extends Event> listener : eventListeners) {
                try {
                    ((EventListener<T>) listener).onEvent(event);
                } catch (Exception e) {
                    log.error("Error notifying listener for event: {}", event, e);
                }
            }
        }
    }
    
    /**
     * Get the count of listeners for a specific event type
     */
    public int getListenerCount(Class<? extends Event> eventType) {
        List<EventListener<? extends Event>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }
    
    /**
     * Clear all listeners
     */
    public void clearAllListeners() {
        log.debug("Clearing all event listeners");
        listeners.clear();
    }
}
