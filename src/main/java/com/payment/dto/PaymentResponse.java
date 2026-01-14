package com.payment.dto;

import com.payment.domain.PaymentStatus;
import java.time.Instant;

public class PaymentResponse {
    private String id;
    private long amount;
    private String currency;
    private PaymentStatus status;
    private Instant createdAt;


    public PaymentResponse(String id, long amount, String currency,
                         PaymentStatus status, Instant createdAt) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
