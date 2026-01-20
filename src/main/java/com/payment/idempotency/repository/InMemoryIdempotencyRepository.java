package com.payment.idempotency.repository;

import com.payment.idempotency.model.IdempotencyRecord;
import com.payment.idempotency.model.IdempotencySaveResult;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryIdempotencyRepository implements IdempotencyRepository{
    private final Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

    @Override
    public IdempotencySaveResult save(IdempotencyRecord idempotencyRecord) {
        IdempotencyRecord record = store.putIfAbsent(idempotencyRecord.getIdempotencyKey(), idempotencyRecord);
        return record == null ? IdempotencySaveResult.CREATED : IdempotencySaveResult.EXISTED;
    }

    @Override
    public Optional<IdempotencyRecord> findByIdempotency(String idempotencyKey) {
        return Optional.ofNullable(store.get(idempotencyKey));
    }
}
