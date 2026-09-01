package com.elziojunior.simplifiedbankingservice.exception;

/** Signals invalid transfer input that maps safely to HTTP 400. */
public class TransferValidationException extends RuntimeException {
    public TransferValidationException(String message) { super(message); }
}
