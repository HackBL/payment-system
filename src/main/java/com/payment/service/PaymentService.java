package com.payment.service;
import com.payment.domain.Payment;
import com.payment.domain.PaymentStatus;
import com.payment.dto.CreatePaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.exception.ConflictException;
import com.payment.exception.InvalidPaymentStatusException;
import com.payment.exception.PaymentNotFoundException;
import com.payment.idempotency.model.IdempotencyRecord;
import com.payment.idempotency.model.IdempotencySaveResult;
import com.payment.idempotency.repository.IdempotencyRepository;
import com.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// Service: contains core business logic and enforces payment status transitions
@Service
public class PaymentService {
    private final PaymentRepository repository;
    private final IdempotencyRepository idempotencyRepository;

    public PaymentService(PaymentRepository paymentRepository, IdempotencyRepository idempotencyRepository) {
        this.repository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
    }


    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new IllegalArgumentException("Currency required");
        }

        // 带有Idempotency，生成新的payment request
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {

            String requestHash = computeRequestHash(request.getAmount(), request.getCurrency());
            Optional<IdempotencyRecord> existingOpt = idempotencyRepository.findByIdempotency(idempotencyKey);

            // 通过Idempotency，进行了重复操作
            if (existingOpt.isPresent()) {

                IdempotencyRecord record = existingOpt.get();

                if (!record.getRequestHash().equals(requestHash)) {
                    throw new ConflictException("Idempotency-Key reused with different payload");
                }

                // 同一个Idempotency和Payload，重复操作，返回同一个payment结果
                Payment oldPayment = repository.findById(record.getPaymentId())
                        .orElseThrow(() -> new RuntimeException("Idempotency record points to missing payment"));

                return toResponse(oldPayment);
            }

            // 新的Payment 带有Idempotency
            String paymentId = UUID.randomUUID().toString();
            IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, requestHash, paymentId, Instant.now());

            IdempotencySaveResult saveResult = idempotencyRepository.save(record);

            if (saveResult == IdempotencySaveResult.CREATED) {

                Payment payment = createAndSavePayment(request);
                return toResponse(payment);
            } else if (saveResult == IdempotencySaveResult.EXISTED) {

                IdempotencyRecord existingRecord = idempotencyRepository.findByIdempotency(idempotencyKey)
                        .orElseThrow(() -> new RuntimeException("Idempotency key exists but record missing"));

                if (!existingRecord.getRequestHash().equals(requestHash)) {
                    throw new ConflictException("Idempotency-Key reused with different payload");
                }

                Payment oldPayment = repository.findById(existingRecord.getPaymentId())
                        .orElseThrow(() -> new RuntimeException("Idempotency record points to missing payment"));

                return toResponse(oldPayment);
            }
        }

        // 不带有Idempotency，生成新的payment request
        Payment payment = createAndSavePayment(request);
        return toResponse(payment);
    }



    public PaymentResponse cancelPayment(String id) {
        Payment payment = repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment Not Found"));

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            return toResponse(payment);
        }

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new InvalidPaymentStatusException("Payment cannot be canceled from status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.CANCELED);
        payment.setUpdatedAt(Instant.now());
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
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private String computeRequestHash(long amount, String currency) {
        try {
            String canonical = "amount=" + amount + "|currency=" + currency.trim().toUpperCase();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private Payment createAndSavePayment(CreatePaymentRequest request) {
        Instant now = Instant.now();

        Payment payment = new Payment(
                UUID.randomUUID().toString(),
                request.getAmount(),
                request.getCurrency(),
                PaymentStatus.CREATED,
                now,
                now
        );

        repository.save(payment);
        return payment;
    }

}
