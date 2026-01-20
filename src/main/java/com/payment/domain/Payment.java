package com.payment.domain;
import java.time.Instant;

// Payment: Domain Model that represents the internal state of a payment
public class Payment {
    private final String id;
    private final long amount;
    private final String currency;
    private PaymentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Payment(String id, long amount, String currency, PaymentStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() {return updatedAt;}

    public void setStatus(PaymentStatus status) {this.status = status;}
    public void setUpdatedAt(Instant updatedAt) {this.updatedAt = updatedAt;}
}
