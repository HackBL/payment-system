package com.payment.event.model;

import java.time.Instant;

public class PaymentCreatedEvent extends PaymentEvent{
    private final long amount;
    private final String currency;

    public PaymentCreatedEvent(String aggregateId, Instant occurredAt, String idempotencyKey, long amount, String currency) {
        super(PaymentEventType.PAYMENT_CREATED, aggregateId, occurredAt, idempotencyKey);

        this.amount = amount;
        this.currency = currency;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
