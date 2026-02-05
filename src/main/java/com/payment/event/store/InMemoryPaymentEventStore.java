package com.payment.event.store;

import com.payment.event.model.PaymentEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryPaymentEventStore implements  PaymentEventStore{
    private final Map<String, List<PaymentEvent>> store = new HashMap<>();

    @Override
    public void append(PaymentEvent event) {
        store.computeIfAbsent(event.getAggregateId(), k -> new ArrayList<>()).add(event);
    }

    @Override
    public List<PaymentEvent> listByPaymentId(String paymentId) {
        return store.getOrDefault(paymentId, List.of());
    }
}
