package com.elziojunior.simplifiedbankingservice.service;

/**
 * Signals that an account-creation command violates an application invariant
 * and can be safely translated to a client validation response.
 */
public class AccountCreationValidationException extends RuntimeException {

    public AccountCreationValidationException(String message) {
        super(message);
    }
}
