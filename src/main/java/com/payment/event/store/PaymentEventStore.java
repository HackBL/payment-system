package com.payment.event.store;

import com.payment.event.model.PaymentEvent;

import java.util.List;

public interface PaymentEventStore {
    void append(PaymentEvent event);
    List<PaymentEvent> listByPaymentId(String paymentId);
}
