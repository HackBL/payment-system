package com.payment.controller;

import com.payment.dto.CancelPaymentRequest;
import com.payment.dto.CreatePaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.event.model.PaymentEvent;
import com.payment.event.store.PaymentEventStore;
import com.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


// Controller: Handles HTTP request and response from clients and delegates payment operation to the service layer
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentService service;
    private final PaymentEventStore eventStore;

    public PaymentController(PaymentService service, PaymentEventStore eventStore) {
        this.service = service;
        this.eventStore = eventStore;
    }

    @PostMapping
    public PaymentResponse createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreatePaymentRequest request) {
        return service.createPayment(request, idempotencyKey);
    }

    @PostMapping("/{id}/cancel")
    public PaymentResponse cancelPayment (
            @PathVariable String id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) CancelPaymentRequest request) {
        return service.cancelPayment(id, idempotencyKey, request);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable String id) {
        return service.getPayment(id);
    }

    @GetMapping("/{id}/events")
    public List<PaymentEvent> getPaymentEvents(@PathVariable String id) {
        return eventStore.listByPaymentId(id);
    }
}
