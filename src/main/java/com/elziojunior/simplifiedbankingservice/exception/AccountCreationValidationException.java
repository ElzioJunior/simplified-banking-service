package com.elziojunior.simplifiedbankingservice.exception;

/**
 * Signals that account-creation data violates an application invariant and can
 * be safely translated to a client validation response.
 */
public class AccountCreationValidationException extends RuntimeException {

    public AccountCreationValidationException(String message) {
        super(message);
    }
}
