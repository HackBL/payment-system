package com.payment.event.model;

import java.time.Instant;
import java.util.UUID;

public abstract class PaymentEvent {
    private final UUID eventId; // UUID
    private final PaymentEventType eventType;
    private final AggregateType aggregateType;
    private final String aggregateId; // paymentId
    private final Instant occurredAt;
    private final String idempotencyKey; // nullable

    protected PaymentEvent(PaymentEventType eventType, String aggregateId, Instant occurredAt, String idempotencyKey) {
        this.eventId = UUID.randomUUID();
        this.eventType = eventType;
        this.aggregateType = AggregateType.PAYMENT;
        this.aggregateId = aggregateId;
        this.occurredAt = occurredAt;
        this.idempotencyKey = idempotencyKey;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public PaymentEventType getEventType() {
        return eventType;
    }

    public AggregateType getAggregateType() {
        return aggregateType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
