package com.payment.service;

import com.payment.domain.Payment;
import com.payment.domain.PaymentStatus;
import com.payment.dto.CreatePaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.exception.ConflictException;
import com.payment.exception.IdempotencyInProgressException;
import com.payment.idempotency.model.IdempotencyRecord;
import com.payment.idempotency.model.RecordStatus;
import com.payment.idempotency.repository.IdempotencyRepository;
import com.payment.idempotency.repository.InMemoryIdempotencyRepository;
import com.payment.repository.InMemoryPaymentRepository;
import com.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {
    private PaymentRepository paymentRepository;
    private IdempotencyRepository idempotencyRepository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = new InMemoryPaymentRepository();
        idempotencyRepository = new InMemoryIdempotencyRepository();
        paymentService = new PaymentService(paymentRepository, idempotencyRepository, null);
    }

    //  1) 无 idempotencyKey → 创建新 payment
    @Test
    void createPayment_WithoutIdempotency_ShouldCreateNewPayment() {
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(100);
        paymentRequest.setCurrency("USD");
        PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, null);

        assertEquals(PaymentStatus.CREATED, paymentResponse.getStatus());
        assertNotNull(paymentResponse.getId());

        assertEquals(100, paymentResponse.getAmount());
        assertEquals("USD", paymentResponse.getCurrency());

        Optional<Payment> payment = paymentRepository.findById(paymentResponse.getId());
        assertTrue(payment.isPresent());
        assertEquals(100, payment.get().getAmount());
        assertEquals("USD", payment.get().getCurrency());
    }

    //  2) 有 idempotencyKey 第一次请求（key 新）→ 创建新 payment，status=CREATED
    @Test
    void createPayment_withIdempotency_firstTime_shouldCreateNewPayment() {
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setAmount(100);
        paymentRequest.setCurrency("USD");
        String idempotencyKey = "idem-key";
        PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, idempotencyKey);

        assertEquals(PaymentStatus.CREATED, paymentResponse.getStatus());
        assertNotNull(paymentResponse.getId());

        assertEquals(100, paymentResponse.getAmount());
        assertEquals("USD", paymentResponse.getCurrency());

        Optional<Payment> payment = paymentRepository.findById(paymentResponse.getId());
        assertTrue(payment.isPresent());
        assertEquals(100, payment.get().getAmount());
        assertEquals("USD", payment.get().getCurrency());

        Optional<IdempotencyRecord> idempotencyRecord = idempotencyRepository.findByIdempotency(idempotencyKey);
        assertTrue(idempotencyRecord.isPresent());
        assertEquals(RecordStatus.COMPLETED, idempotencyRecord.get().getRecordStatus());
        assertEquals(paymentResponse.getId(), idempotencyRecord.get().getPaymentId());
        assertNotNull(idempotencyRecord.get().getRequestHash());
    }

    // 3) 同 key + 同 payload + record=COMPLETED → 返回同一个 payment（id 不变）
    @Test
    void createPayment_withIdempotency_paymentWithSameIdempotencyKeyAndSamePayload() {
        String idempotencyKey = "idem-key";

        CreatePaymentRequest paymentRequest1 = new CreatePaymentRequest();
        paymentRequest1.setAmount(100);
        paymentRequest1.setCurrency("USD");
        PaymentResponse paymentResponse1 = paymentService.createPayment(paymentRequest1, idempotencyKey);

        assertEquals(PaymentStatus.CREATED, paymentResponse1.getStatus());

        CreatePaymentRequest paymentRequest2 = new CreatePaymentRequest();
        paymentRequest2.setAmount(100);
        paymentRequest2.setCurrency("USD");
        PaymentResponse paymentResponse2 = paymentService.createPayment(paymentRequest2, idempotencyKey);

        assertEquals(PaymentStatus.CREATED, paymentResponse2.getStatus());

        assertEquals(paymentResponse1.getId(), paymentResponse2.getId());
        Optional<IdempotencyRecord> record =
                idempotencyRepository.findByIdempotency(idempotencyKey);

        assertTrue(record.isPresent());
        assertEquals(RecordStatus.COMPLETED, record.get().getRecordStatus());
        assertEquals(paymentResponse1.getId(), record.get().getPaymentId());
    }

    // 4) 同 key + 不同 payload → 抛 ConflictException（409)
    @Test
    void createPayment_withIdempotency_paymentWithSameIdempotencyAndDifferentPayload() {
        String idempotencyKey = "idem-key";

        CreatePaymentRequest paymentRequest1 = new CreatePaymentRequest();
        paymentRequest1.setAmount(100);
        paymentRequest1.setCurrency("USD");
        PaymentResponse paymentResponse1 = paymentService.createPayment(paymentRequest1, idempotencyKey);

        assertEquals(PaymentStatus.CREATED, paymentResponse1.getStatus());

        CreatePaymentRequest paymentRequest2 = new CreatePaymentRequest();
        paymentRequest2.setAmount(200);
        paymentRequest2.setCurrency("RMB");
        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> paymentService.createPayment(paymentRequest2, idempotencyKey)
        );

        assertEquals("Idempotency-Key reused with different payload", ex.getMessage());

        Optional<IdempotencyRecord> record = idempotencyRepository.findByIdempotency(idempotencyKey);
        assertTrue(record.isPresent());
        assertEquals(paymentResponse1.getId(), record.get().getPaymentId());
        assertEquals(RecordStatus.COMPLETED, record.get().getRecordStatus());
    }

    // 5) 同 key + record=IN_PROGRESS + payment 不存在 + 未过期 → 抛 IdempotencyInProgressException（202）
    @Test
    void createPayment_withIdempotency_firstPaymentIsInProgress_SecondPaymentWithSameIdempotencyAndSamePayload_NoPaymentFoundInPaymentRepository() {
        String idempotencyKey = "idem-key";
        String paymentId = UUID.randomUUID().toString();

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setCurrency("USD");
        request.setAmount(100);

        String requestHash = computeRequestHash(request.getAmount(), request.getCurrency());

        IdempotencyRecord inProgressRecord = new IdempotencyRecord(
                idempotencyKey,
                requestHash,
                paymentId,
                Instant.now(),
                RecordStatus.IN_PROGRESS
        );
        idempotencyRepository.save(inProgressRecord);
        assertTrue(idempotencyRepository.findByIdempotency(idempotencyKey).isPresent());
        assertEquals(RecordStatus.IN_PROGRESS, idempotencyRepository.findByIdempotency(idempotencyKey).get().getRecordStatus());

        IdempotencyInProgressException ex = assertThrows(
                IdempotencyInProgressException.class,
                () -> paymentService.createPayment(request, idempotencyKey)
        );

        assertEquals("Request with same Idempotency is still In-Progress", ex.getMessage());
        assertTrue(idempotencyRepository.findByIdempotency(idempotencyKey).isPresent());
        assertEquals(RecordStatus.IN_PROGRESS, idempotencyRepository.findByIdempotency(idempotencyKey).get().getRecordStatus());
    }

    // 6) 同 key + record=IN_PROGRESS + payment 存在 → 走“自愈”：返回 payment，并把 record 标记 COMPLETED
    @Test
    void createPayment_withIdempotency_firstPaymentIsInProgress_secondPaymentWithSameIdempotencyAndSamePayload_PaymentFoundInPaymentRepository() {
//        String idempotencyKey = "idem-key";
//        String paymentId = UUID.randomUUID().toString();
//
//        CreatePaymentRequest request = new CreatePaymentRequest();
//        request.setAmount(100);
//        request.setCurrency("USD");
//
//        String requestHash = computeRequestHash(request.getAmount(), request.getCurrency());
//        IdempotencyRecord inProgressRecord = new IdempotencyRecord(
//                idempotencyKey,
//                requestHash,
//                paymentId,
//                Instant.now(),
//                RecordStatus.IN_PROGRESS
//        );
//        idempotencyRepository.save(inProgressRecord);
//        assertTrue(idempotencyRepository.findByIdempotency(idempotencyKey).isPresent());
//        assertEquals(RecordStatus.IN_PROGRESS, idempotencyRepository.findByIdempotency(idempotencyKey).get().getRecordStatus());


    }
    // 7) 同 key + record=IN_PROGRESS + payment 不存在 + 已过期 → 标记 EXPIRED，然后抛 ConflictException

    // 8) recordStatus=EXPIRED → 直接 ConflictException

    // 9) saveResult 出现未知值/为 null → IllegalStateException

    // 10) currency 小写/带空格：请求里 " usd " → response currency 仍是原样还是被 normalize？

    // 11) CREATED → cancel 成功，status=CANCELED，updatedAt 变化

    // 12) 已是 CANCELED → 再 cancel，幂等返回（仍 CANCELED）

    // 13) 非 CREATED（如果你以后加 AUTH/CAPTURE）→ cancel 抛 InvalidPaymentStatusException

    // 14) payment 不存在 → PaymentNotFoundException（404）

    // 15) 存在 → 返回正确数据

    // 16) 不存在 → PaymentNotFoundException（404）

    // 17) 两个线程同 key 同 payload 并发 createPayment：最终只创建 1 个 payment，另一个返回同一个结果 或 202（取决于时序，你需要把断言写成“允许两种结果”）

    // 18) 两个线程同 key 不同 payload 并发：一个成功/一个 conflict（同样断言允许时序差异）

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
}