package com.elziojunior.simplifiedbankingservice.exception;

/** Signals invalid movement-query input that maps safely to HTTP 400. */
public class AccountMovementValidationException extends RuntimeException implements RejectedRequestException {

    public AccountMovementValidationException(String message) {
        super(message);
    }
}
