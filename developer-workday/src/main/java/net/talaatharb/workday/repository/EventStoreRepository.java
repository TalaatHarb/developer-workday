package net.talaatharb.workday.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.mapdb.DB;
import org.mapdb.Serializer;

import lombok.extern.slf4j.Slf4j;
import net.talaatharb.workday.event.Event;

/**
 * Repository for persisting all application events for audit trail and event replay functionality.
 * Uses MapDB for persistence.
 */
@Slf4j
public class EventStoreRepository {
    private static final String EVENTS_MAP = "events";
    private final DB database;
    private final Map<UUID, Event> eventsMap;
    
    public EventStoreRepository(DB database) {
        this.database = database;
        this.eventsMap = database.hashMap(EVENTS_MAP, Serializer.UUID, Serializer.JAVA).createOrOpen();
        log.info("EventStoreRepository initialized with {} events", eventsMap.size());
    }
    
    /**
     * Save an event to the event store
     */
    public Event save(Event event) {
        eventsMap.put(event.getEventId(), event);
        database.commit();
        log.debug("Saved event: {} - {}", event.getEventType(), event.getEventId());
        return event;
    }
    
    /**
     * Find event by ID
     */
    public Optional<Event> findById(UUID eventId) {
        return Optional.ofNullable(eventsMap.get(eventId));
    }
    
    /**
     * Find all events
     */
    public List<Event> findAll() {
        return new ArrayList<>(eventsMap.values());
    }
    
    /**
     * Find events by type
     */
    public List<Event> findByEventType(String eventType) {
        return eventsMap.values().stream()
            .filter(event -> eventType.equals(event.getEventType()))
            .sorted(Comparator.comparing(Event::getTimestamp))
            .collect(Collectors.toList());
    }
    
    /**
     * Find events by date range in chronological order
     */
    public List<Event> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return eventsMap.values().stream()
            .filter(event -> !event.getTimestamp().isBefore(startTime) 
                          && !event.getTimestamp().isAfter(endTime))
            .sorted(Comparator.comparing(Event::getTimestamp))
            .collect(Collectors.toList());
    }
    
    /**
     * Find events by type and date range in chronological order
     */
    public List<Event> findByEventTypeAndTimestampBetween(String eventType, 
                                                          LocalDateTime startTime, 
                                                          LocalDateTime endTime) {
        return eventsMap.values().stream()
            .filter(event -> eventType.equals(event.getEventType()))
            .filter(event -> !event.getTimestamp().isBefore(startTime) 
                          && !event.getTimestamp().isAfter(endTime))
            .sorted(Comparator.comparing(Event::getTimestamp))
            .collect(Collectors.toList());
    }
    
    /**
     * Find events after a specific timestamp
     */
    public List<Event> findAfterTimestamp(LocalDateTime timestamp) {
        return eventsMap.values().stream()
            .filter(event -> event.getTimestamp().isAfter(timestamp))
            .sorted(Comparator.comparing(Event::getTimestamp))
            .collect(Collectors.toList());
    }
    
    /**
     * Find events before a specific timestamp
     */
    public List<Event> findBeforeTimestamp(LocalDateTime timestamp) {
        return eventsMap.values().stream()
            .filter(event -> event.getTimestamp().isBefore(timestamp))
            .sorted(Comparator.comparing(Event::getTimestamp))
            .collect(Collectors.toList());
    }
    
    /**
     * Count all events
     */
    public long count() {
        return eventsMap.size();
    }
    
    /**
     * Count events by type
     */
    public long countByEventType(String eventType) {
        return eventsMap.values().stream()
            .filter(event -> eventType.equals(event.getEventType()))
            .count();
    }
    
    /**
     * Delete all events (use with caution)
     */
    public void deleteAll() {
        eventsMap.clear();
        database.commit();
        log.warn("Deleted all events from event store");
    }
    
    /**
     * Check if event exists by ID
     */
    public boolean existsById(UUID eventId) {
        return eventsMap.containsKey(eventId);
    }
}
