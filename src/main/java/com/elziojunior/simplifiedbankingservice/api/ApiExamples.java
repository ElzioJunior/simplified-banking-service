package com.elziojunior.simplifiedbankingservice.api;

/** Fictional OpenAPI examples shared by the public HTTP operations. */
final class ApiExamples {

    static final String ACCOUNT_CREATE = """
            {
              "name": "Ada Lovelace",
              "initialBalance": 1000.00
            }
            """;
    static final String ACCOUNT_CREATE_ZERO_BALANCE = """
            {
              "name": "Grace Hopper",
              "initialBalance": 0.00
            }
            """;
    static final String ACCOUNT_CREATE_BLANK_NAME = """
            {
              "name": "   ",
              "initialBalance": 100.00
            }
            """;
    static final String ACCOUNT_CREATE_MISSING_BALANCE = """
            {
              "name": "Ada Lovelace"
            }
            """;
    static final String ACCOUNT_CREATE_NEGATIVE_BALANCE = """
            {
              "name": "Ada Lovelace",
              "initialBalance": -0.01
            }
            """;
    static final String ACCOUNT_CREATE_UNSUPPORTED_BALANCE = """
            {
              "name": "Ada Lovelace",
              "initialBalance": 100000000000000000.00
            }
            """;
    static final String ACCOUNT_RESPONSE = """
            {
              "id": 41,
              "name": "Ada Lovelace",
              "balance": 1000.00,
              "createdAt": "2026-09-01T12:00:00Z"
            }
            """;

    static final String MOVEMENT_PAGE = """
            {
              "content": [
                {
                  "id": 84,
                  "operationId": "f6608b62-b6ba-4da2-864d-b8d49c48fb85",
                  "type": "CREDIT",
                  "amount": 100.00,
                  "createdAt": "2026-09-01T12:00:00Z"
                }
              ],
              "page": 0,
              "size": 10,
              "totalElements": 1,
              "totalPages": 1
            }
            """;
    static final String EMPTY_MOVEMENT_PAGE = """
            {
              "content": [],
              "page": 0,
              "size": 10,
              "totalElements": 0,
              "totalPages": 0
            }
            """;

    static final String TRANSFER_TOKEN_RESPONSE = """
            {
              "token": "4e80db4d-ce8c-40a6-b839-b45fd45b1461",
              "expiresAt": "2026-09-01T12:10:00Z"
            }
            """;
    static final String TRANSFER_CREATE = """
            {
              "sourceAccountId": 41,
              "destinationAccountId": 42,
              "amount": 100.00
            }
            """;
    static final String TRANSFER_REPLAY = """
            {
              "sourceAccountId": 41,
              "destinationAccountId": 42,
              "amount": 100.00
            }
            """;
    static final String TRANSFER_TOKEN_PAYLOAD_MISMATCH = """
            {
              "sourceAccountId": 41,
              "destinationAccountId": 42,
              "amount": 101.00
            }
            """;
    static final String TRANSFER_MISSING_ACCOUNT = """
            {
              "destinationAccountId": 42,
              "amount": 100.00
            }
            """;
    static final String TRANSFER_ZERO_AMOUNT = """
            {
              "sourceAccountId": 41,
              "destinationAccountId": 42,
              "amount": 0.00
            }
            """;
    static final String TRANSFER_UNSUPPORTED_AMOUNT = """
            {
              "sourceAccountId": 41,
              "destinationAccountId": 42,
              "amount": 100000000000000000.00
            }
            """;
    static final String TRANSFER_SAME_ACCOUNT = """
            {
              "sourceAccountId": 41,
              "destinationAccountId": 41,
              "amount": 100.00
            }
            """;
    static final String TRANSFER_INSUFFICIENT_FUNDS = """
            {
              "sourceAccountId": 41,
              "destinationAccountId": 42,
              "amount": 1000000.00
            }
            """;
    static final String TRANSFER_UNKNOWN_ACCOUNT = """
            {
              "sourceAccountId": 999999,
              "destinationAccountId": 42,
              "amount": 100.00
            }
            """;
    static final String TRANSFER_RESPONSE = """
            {
              "transferId": "f6608b62-b6ba-4da2-864d-b8d49c48fb85",
              "status": "COMPLETED",
              "sourceAccountId": 41,
              "destinationAccountId": 42,
              "amount": 100.00
            }
            """;

    static final String INVALID_REQUEST = """
            {
              "type": "about:blank",
              "title": "Invalid request",
              "status": 400,
              "detail": "The request is invalid."
            }
            """;
    static final String INVALID_BODY = """
            {
              "type": "about:blank",
              "title": "Invalid request",
              "status": 400,
              "detail": "The request body is invalid or unreadable."
            }
            """;
    static final String UNSUPPORTED_ACCOUNT_BALANCE = """
            {
              "type": "about:blank",
              "title": "Invalid account creation request",
              "status": 400,
              "detail": "Initial balance exceeds the supported monetary range."
            }
            """;
    static final String UNSUPPORTED_TRANSFER_AMOUNT = """
            {
              "type": "about:blank",
              "title": "Invalid transfer request",
              "status": 400,
              "detail": "Transfer amount is outside the supported monetary range."
            }
            """;
    static final String INVALID_MOVEMENT_RANGE = """
            {
              "type": "about:blank",
              "title": "Invalid movement query",
              "status": 400,
              "detail": "Start must be before end."
            }
            """;
    static final String ACCOUNT_NOT_FOUND = """
            {
              "type": "about:blank",
              "title": "Account not found",
              "status": 404,
              "detail": "The requested account does not exist."
            }
            """;
    static final String MOVEMENT_UNAVAILABLE = """
            {
              "type": "about:blank",
              "title": "Movement query temporarily unavailable",
              "status": 503,
              "detail": "The movements could not be retrieved because persistence is unavailable."
            }
            """;
    static final String MISSING_IDEMPOTENCY_KEY = """
            {
              "type": "about:blank",
              "title": "Invalid transfer request",
              "status": 400,
              "detail": "The Idempotency-Key header is required."
            }
            """;
    static final String MALFORMED_IDEMPOTENCY_KEY = """
            {
              "type": "about:blank",
              "title": "Invalid transfer request",
              "status": 400,
              "detail": "The Idempotency-Key header is invalid."
            }
            """;
    static final String TRANSFER_ACCOUNT_NOT_FOUND = """
            {
              "type": "about:blank",
              "title": "Transfer account not found",
              "status": 404,
              "detail": "A transfer account was not found."
            }
            """;
    static final String SAME_ACCOUNT_CONFLICT = """
            {
              "type": "about:blank",
              "title": "Transfer conflict",
              "status": 409,
              "detail": "Source and destination accounts must be different."
            }
            """;
    static final String INSUFFICIENT_FUNDS_CONFLICT = """
            {
              "type": "about:blank",
              "title": "Transfer conflict",
              "status": 409,
              "detail": "The source account has insufficient funds."
            }
            """;
    static final String INVALID_TOKEN_CONFLICT = """
            {
              "type": "about:blank",
              "title": "Transfer conflict",
              "status": 409,
              "detail": "The idempotency token is invalid."
            }
            """;
    static final String EXPIRED_TOKEN_CONFLICT = """
            {
              "type": "about:blank",
              "title": "Transfer conflict",
              "status": 409,
              "detail": "The idempotency token has expired."
            }
            """;
    static final String TOKEN_PAYLOAD_MISMATCH_CONFLICT = """
            {
              "type": "about:blank",
              "title": "Transfer conflict",
              "status": 409,
              "detail": "The idempotency token is associated with another transfer."
            }
            """;
    static final String TRANSFER_UNAVAILABLE = """
            {
              "type": "about:blank",
              "title": "Transfer temporarily unavailable",
              "status": 503,
              "detail": "The transfer could not acquire the required resources."
            }
            """;
    static final String TRANSFER_PERSISTENCE_UNAVAILABLE = """
            {
              "type": "about:blank",
              "title": "Transfer temporarily unavailable",
              "status": 503,
              "detail": "The transfer could not be completed because persistence is unavailable."
            }
            """;

    private ApiExamples() {
    }
}
