package net.talaatharb.workday.event;

/**
 * Functional interface for event listeners.
 * Listeners can subscribe to specific event types.
 */
@FunctionalInterface
public interface EventListener<T extends Event> {
    void onEvent(T event);
}
