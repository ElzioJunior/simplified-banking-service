package com.elziojunior.simplifiedbankingservice.exception;

/** Signals that the account requested for movement history does not exist. */
public class AccountMovementNotFoundException extends RuntimeException implements RejectedRequestException {

    public AccountMovementNotFoundException(String message) {
        super(message);
    }
}
