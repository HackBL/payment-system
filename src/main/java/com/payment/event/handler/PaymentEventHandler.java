package com.payment.event.handler;

import com.payment.event.model.PaymentEvent;

public interface PaymentEventHandler {
    void handle(PaymentEvent paymentEvent);
}
