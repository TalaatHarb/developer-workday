package net.talaatharb.workday.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;

/**
 * Abstract base class for all domain events in the application.
 * Implements Serializable for event store persistence.
 */
@Getter
public abstract class Event implements Serializable {
    private static final long serialVersionUID = 1L;
    private final UUID eventId;
    private final LocalDateTime timestamp;
    private final String eventType;
    
    protected Event() {
        this.eventId = UUID.randomUUID();
        this.timestamp = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
    }
    
    protected Event(String eventType) {
        this.eventId = UUID.randomUUID();
        this.timestamp = LocalDateTime.now();
        this.eventType = eventType;
    }
    
    public abstract String getEventDetails();
    
    @Override
    public String toString() {
        return String.format("%s[id=%s, timestamp=%s]", eventType, eventId, timestamp);
    }
}
