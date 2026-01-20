package com.payment.controller;

import com.payment.dto.CreatePaymentRequest;
import com.payment.dto.PaymentResponse;
import com.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


// Controller: Handles HTTP request and response from clients and delegates payment operation to the service layer
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreatePaymentRequest request) {
        return service.createPayment(request, idempotencyKey);
    }

    @PostMapping("/{id}/cancel")
    public PaymentResponse cancelPayment(@PathVariable String id) {
        return service.cancelPayment(id);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable String id) {
        return service.getPayment(id);
    }
}
