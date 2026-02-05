package com.payment.event.model;

import java.time.Instant;

public class PaymentCanceledEvent extends PaymentEvent{
    private final String reason; // nullable

    public PaymentCanceledEvent(String aggregateId, Instant occurredAt, String idempotencyKey, String reason) {
        super(PaymentEventType.PAYMENT_CANCELED, aggregateId, occurredAt, idempotencyKey);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
