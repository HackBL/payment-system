package com.payment.repository;

import com.payment.domain.Payment;
import java.util.Optional;

// Repository: abstract persistence of Payment entities
public interface PaymentRepository {
    void save(Payment payment);
    Optional<Payment> findById(String id);
}
