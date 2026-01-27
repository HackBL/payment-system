package com.payment.service;
import com.payment.domain.Payment;
import com.payment.domain.PaymentStatus;
import com.payment.dto.CreatePaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.exception.ConflictException;
import com.payment.exception.IdempotencyInProgressException;
import com.payment.exception.InvalidPaymentStatusException;
import com.payment.exception.PaymentNotFoundException;
import com.payment.idempotency.model.IdempotencyRecord;
import com.payment.idempotency.model.IdempotencySaveResult;
import com.payment.idempotency.model.RecordStatus;
import com.payment.idempotency.repository.IdempotencyRepository;
import com.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// Service: contains core business logic and enforces payment status transitions
@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final IdempotencyRepository idempotencyRepository;
    private static final Duration IDEMPOTENCY_TTL = Duration.ofSeconds(30);

    public PaymentService(PaymentRepository paymentRepository, IdempotencyRepository idempotencyRepository) {
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
    }


    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new IllegalArgumentException("Currency required");
        }

        Instant now = Instant.now();

        // 带有Idempotency，生成新的payment request
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {

            String requestHash = computeRequestHash(request.getAmount(), request.getCurrency());
            Optional<IdempotencyRecord> existingOpt = idempotencyRepository.findByIdempotency(idempotencyKey);


            // 通过Idempotency，进行了重复操作
            if (existingOpt.isPresent()) {
                return validateAndReturnExistingPayment(existingOpt.get(), requestHash);
            }

            // 新的Payment 带有Idempotency
            String paymentId = UUID.randomUUID().toString();

            IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, requestHash, paymentId, now, RecordStatus.IN_PROGRESS);
            IdempotencySaveResult saveResult = idempotencyRepository.save(record);

            if (saveResult == IdempotencySaveResult.CREATED) {

                Payment payment = createAndSavePayment(request, paymentId, now);
                try {
                    idempotencyRepository.markCompleted(idempotencyKey);
                } catch (IllegalStateException ex) {
                    System.err.println("[WARN] markCompleted failed. key=" + idempotencyKey + ", error=" + ex.getMessage());
                }

                return toResponse(payment);
            } else if (saveResult == IdempotencySaveResult.EXISTED) {

                IdempotencyRecord existingRecord = idempotencyRepository.findByIdempotency(idempotencyKey)
                        .orElseThrow(() -> new IllegalStateException("Idempotency key exists but record missing"));

                return validateAndReturnExistingPayment(existingRecord, requestHash);
            } else {
                throw new IllegalStateException("Unexpected idempotency saveResult=" + saveResult);
            }
        }

        // 不带有Idempotency，生成新的payment request
        String paymentId = UUID.randomUUID().toString();

        Payment payment = createAndSavePayment(request, paymentId, now);
        return toResponse(payment);
    }



    public PaymentResponse cancelPayment(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment Not Found"));

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            return toResponse(payment);
        }

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new InvalidPaymentStatusException("Payment cannot be canceled from status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.CANCELED);
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);

        return toResponse(payment);
    }

    public PaymentResponse getPayment(String id) {
        Payment payment =  paymentRepository.findById(id)
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

    private PaymentResponse validateAndReturnExistingPayment(IdempotencyRecord record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new ConflictException("Idempotency-Key reused with different payload");
        }

        RecordStatus status = record.getRecordStatus();
        if (status == null) {
            throw new IllegalStateException("Unknown record status");
        }

        if (status == RecordStatus.EXPIRED) {
            throw new ConflictException("Idempotency-Key expired; retry with a NEW key");
        }

        Optional<Payment> paymentOpt = paymentRepository.findById(record.getPaymentId());

        if (status == RecordStatus.IN_PROGRESS) {

            if (paymentOpt.isEmpty()) {
                if (isExpired(record)) {
                    try {
                        idempotencyRepository.markExpired(record.getIdempotencyKey());
                    } catch (IllegalStateException ex) {
                        System.err.println("[WARN] markExpired failed. key=" + record.getIdempotencyKey() + ", error=" + ex.getMessage());
                    }

                    throw new ConflictException("Idempotency-Key expired; retry with a NEW key");
                }

                throw new IdempotencyInProgressException("Request with same Idempotency is still In-Progress");
            }

            try {
                idempotencyRepository.markCompleted(record.getIdempotencyKey());
            } catch (IllegalStateException ex) {
                System.err.println("[WARN] markCompleted failed. key=" + record.getIdempotencyKey() + ", error=" + ex.getMessage());
            }

            return toResponse(paymentOpt.get());
        }

        if (status == RecordStatus.COMPLETED) {
            Payment payment = paymentOpt.orElseThrow(() ->
                    new IllegalStateException("Record is COMPLETED but payment missing. paymentId=" + record.getPaymentId()));
            return toResponse(payment);
        }

        throw new IllegalStateException("Unsupported record status=" + status + " for paymentId=" + record.getPaymentId());
    }

    private Payment createAndSavePayment(CreatePaymentRequest request, String paymentId, Instant now) {
        Payment payment = new Payment(
                paymentId,
                request.getAmount(),
                request.getCurrency(),
                PaymentStatus.CREATED,
                now,
                now
        );

        paymentRepository.save(payment);
        return payment;
    }

    private boolean isExpired(IdempotencyRecord record) {
        return record.getCreatedAt()
                .plus(IDEMPOTENCY_TTL)
                .isBefore(Instant.now());
    }
}
