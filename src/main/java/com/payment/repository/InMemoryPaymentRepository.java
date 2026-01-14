package com.payment.repository;

import com.payment.domain.Payment;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository{
    private final Map<String, Payment> store = new ConcurrentHashMap<>();

    @Override
    public void save(Payment payment) {
        store.put(payment.getId(), payment);
    }

    @Override
    public Optional<Payment> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
