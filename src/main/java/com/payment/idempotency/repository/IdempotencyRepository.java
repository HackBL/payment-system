package com.payment.idempotency.repository;

import com.payment.idempotency.model.IdempotencyRecord;
import com.payment.idempotency.model.IdempotencySaveResult;

import java.util.Optional;

public interface IdempotencyRepository {
    IdempotencySaveResult save(IdempotencyRecord idempotencyRecord);
    Optional<IdempotencyRecord> findByIdempotency(String idempotencyKey);
}
