package com.payment.idempotency.model;

import java.time.Instant;

public class IdempotencyRecord {
    private final String idempotencyKey;
    private final String requestHash;
    private final String paymentId;
    private final Instant createdAt;
    private RecordStatus recordStatus;

    public IdempotencyRecord(String idempotencyKey, String requestHash, String paymentId, Instant createdAt, RecordStatus recordStatus) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.paymentId = paymentId;
        this.createdAt = createdAt;
        this.recordStatus = recordStatus;
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

    public RecordStatus getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(RecordStatus recordStatus) {
        this.recordStatus = recordStatus;
    }
}
