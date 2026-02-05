package com.payment.event.publisher;

import com.payment.event.model.PaymentEvent;

public interface PaymentEventPublisher {
    void publish(PaymentEvent paymentEvent);

}
