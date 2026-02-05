package com.payment.domain;

/**
 * ed     Status:
 *          - CREATED
 *          - CANCELED
 *          - AUTHORIZED
 *          - CAPTURED
 *          - VOIDED
 *          - REFUNDED
 *
 *      Terminal:
 *          - CANCELED
 *          - CAPTURED
 *          - VOIDED
 *          - REFUNDED
 *
 *      Transition
 *          - CREATED -> CANCELED
 *          - CREATED -> AUTHORIZED
 *          - AUTHORIZED -> CAPTURED
 *          - AUTHORIZED -> CAPTURED -> REFUNDED
 *          - AUTHORIZED -> VOIDED
 *
 * */

public enum PaymentStatus {
    CREATED,
    CANCELED
}
