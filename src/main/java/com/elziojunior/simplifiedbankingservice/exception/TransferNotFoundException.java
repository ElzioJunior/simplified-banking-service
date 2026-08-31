package com.elziojunior.simplifiedbankingservice.exception;

/** Signals that a transfer account does not exist and maps safely to HTTP 404. */
public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException(String message) { super(message); }
}
