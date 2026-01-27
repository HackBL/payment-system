# Payment Transaction Processing System

This project is a simple payment transaction processing service built with Spring Boot.
It supports basic payment operations (Creation, Querying, Cancellation).
The purpose of this project is to demonstrate clean layering, business rule enforcement, and proper HTTP error semantics.

## Current Capabilities

- Create a payment
- Get payment status by ID
- Cancel a payment with status validation

## Payment State Transition
- CREATED -> CANCELED
- CANCELED -> CANCELED (Idempotent)
- otherwise -> 409 conflict

## API Endpoints

- POST /v1/payments

    Create a new payment


- GET /v1/payments/{id}

    Get payment details by ID


- POST /v1/payments/{id}/cancel

    Cancel a payment (only allowed when status is CREATED)

## Error Handling
This service distinguishes business errors from system failures using appropriate HTTP status codes:

- 404 Not Found

    Returned when the payment does not exist.


- 409 Conflict

    Returned when an operation is not allowed for the current payment state
    (e.g., canceling an already canceled payment).


- 500 Internal Service Error
    
    Reserved for unexpected system failures.
 
## Idempotency Design (Key Focus)
This service supports idempotent payment creation using an Idempotency-Key header.

## Why Idempotency?

In real-world systems, clients may retry requests due to:
- Network timeouts
- Client crashes
- Uncertain request outcomes

Without idempotency, retries could result in duplicate payments.


## Idempotency Mechanism
- Each POST /v1/payments request may include an Idempotency-Key
- The request payload is hashed and stored with the key
- Reusing the same key with a different payload results in a conflict


## Idempotency Record States

The system tracks idempotency records with explicit lifecycle states:
- IN_PROGRESS — payment creation started but not fully completed
- COMPLETED — payment successfully created
- EXPIRED — request exceeded TTL without completion

This design helps handle consistency gaps between payment creation and idempotency updates.



## TTL & Recovery

To avoid permanently stuck IN_PROGRESS records:
- Each idempotency record has a TTL
- If a request is retried after TTL expiration:
- The record is marked as EXPIRED
- The client is instructed to retry with a new Idempotency-Key


## Unit Test Coverage

Core idempotency scenarios are covered by unit tests, including:
- Payment creation without idempotency
- First-time idempotent request
- Repeated requests with same key and same payload
- Conflicting payloads with same idempotency key
- In-progress retry handling



## Future Improvements
- Persist idempotency records in a database
- Background cleanup job for expired records
- Event-driven payment lifecycle (PaymentCreated, PaymentCanceled)