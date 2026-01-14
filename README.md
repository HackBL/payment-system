# Payment Transaction Processing System

This project is a simple payment transaction processing service built with Spring Boot.
It supports basic payment operations (Creation, Querying, Cancellation).
The purpose of this project is to demonstrate clean layering, business rule enforcement, and proper HTTP error semantics.

## Current Capabilities

- Create a payment
- Get payment status by ID
- Cancel a payment with status validation

## Payment State Transition
- Created -> Cancelled

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
 