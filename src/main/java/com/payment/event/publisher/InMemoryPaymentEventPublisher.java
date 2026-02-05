package com.payment.event.publisher;

import com.payment.event.handler.PaymentEventHandler;
import com.payment.event.model.PaymentEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InMemoryPaymentEventPublisher implements PaymentEventPublisher{
    private final List<PaymentEventHandler> handlers;

    public InMemoryPaymentEventPublisher(List<PaymentEventHandler> handlers) {
        this.handlers = handlers;
    }


    @Override
    public void publish(PaymentEvent paymentEvent) {
        for (PaymentEventHandler handler: handlers) {
            handler.handle(paymentEvent);
        }
    }
}
