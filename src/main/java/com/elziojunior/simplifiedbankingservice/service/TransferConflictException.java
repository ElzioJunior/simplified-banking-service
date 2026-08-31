package com.elziojunior.simplifiedbankingservice.service;

/** Signals a business or idempotency conflict that maps safely to HTTP 409. */
public class TransferConflictException extends RuntimeException {
    public TransferConflictException(String message) { super(message); }
}
