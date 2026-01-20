package com.payment.idempotency.model;

import java.time.Instant;

public class IdempotencyRecord {
    private final String idempotencyKey;
    private final String requestHash;
    private final String paymentId;
    private final Instant createdAt;

    public IdempotencyRecord(String idempotencyKey, String requestHash, String paymentId, Instant createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.paymentId = paymentId;
        this.createdAt = createdAt;
    }


    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
