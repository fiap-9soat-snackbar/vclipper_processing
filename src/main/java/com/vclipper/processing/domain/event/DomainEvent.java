package com.vclipper.processing.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events
 */
public abstract class DomainEvent {
    private final String eventId;
    private final LocalDateTime occurredAt;
    private final String aggregateId;
    
    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.aggregateId = aggregateId;
    }
    
    public String getEventId() { return eventId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getAggregateId() { return aggregateId; }
    
    public abstract String getEventType();
}
