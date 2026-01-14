package com.payment.service;
import com.payment.domain.Payment;
import com.payment.domain.PaymentStatus;
import com.payment.dto.CreatePaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.exception.InvalidPaymentStatusException;
import com.payment.exception.PaymentNotFoundException;
import com.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

// Service: contains core business logic and enforces payment status transitions
@Service
public class PaymentService {
    private final PaymentRepository repository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.repository = paymentRepository;
    }

    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Payment payment = new Payment(
                UUID.randomUUID().toString(),
                request.getAmount(),
                request.getCurrency(),
                PaymentStatus.CREATED,
                Instant.now()
        );
        repository.save(payment);

        return toResponse(payment);
    }

    public PaymentResponse cancelPayment(String id) {
        Payment payment = repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment Not Found"));

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new InvalidPaymentStatusException("Only CREATED payments can be canceled");
        }

        payment.setStatus(PaymentStatus.CANCELED);
        repository.save(payment);

        return toResponse(payment);
    }

    public PaymentResponse getPayment(String id) {
        Payment payment =  repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment Not Found"));

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }

}
