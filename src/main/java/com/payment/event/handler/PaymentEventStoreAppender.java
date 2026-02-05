package com.payment.event.handler;

import com.payment.event.model.PaymentEvent;
import com.payment.event.store.PaymentEventStore;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventStoreAppender implements PaymentEventHandler{
    private final PaymentEventStore eventStore;

    public PaymentEventStoreAppender(PaymentEventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public void handle(PaymentEvent paymentEvent) {
        eventStore.append(paymentEvent);
    }
}
