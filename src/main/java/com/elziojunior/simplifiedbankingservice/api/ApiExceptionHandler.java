package com.elziojunior.simplifiedbankingservice.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.elziojunior.simplifiedbankingservice.service.AccountCreationValidationException;
import com.elziojunior.simplifiedbankingservice.service.TransferConflictException;
import com.elziojunior.simplifiedbankingservice.service.TransferNotFoundException;
import com.elziojunior.simplifiedbankingservice.service.TransferValidationException;

/** Translates expected client errors into a stable and safe RFC 9457 response. */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Maps request-bean constraint violations without exposing internal details. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "The request is invalid.");
    }

    /** Maps malformed or unreadable JSON without echoing parser or input details. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "The request body is invalid or unreadable.");
    }

    /** Maps application validation that cannot be expressed safely at the HTTP DTO. */
    @ExceptionHandler(AccountCreationValidationException.class)
    public ProblemDetail handleAccountValidation(AccountCreationValidationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid account creation request", exception.getMessage());
    }

    /** Maps missing token headers and transfer validation without exposing rejected payloads. */
    @ExceptionHandler({MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class,
            TransferValidationException.class})
    public ProblemDetail handleTransferValidation(Exception exception) {
        String detail;
        if (exception instanceof TransferValidationException) {
            detail = exception.getMessage();
        } else if (exception instanceof MissingRequestHeaderException) {
            detail = "The Idempotency-Key header is required.";
        } else {
            detail = "The Idempotency-Key header is invalid.";
        }
        return problem(HttpStatus.BAD_REQUEST, "Invalid transfer request", detail);
    }

    /** Maps absent accounts to the documented safe not-found contract. */
    @ExceptionHandler(TransferNotFoundException.class)
    public ProblemDetail handleTransferNotFound(TransferNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Transfer account not found", exception.getMessage());
    }

    /** Maps business and token conflicts without returning account state. */
    @ExceptionHandler(TransferConflictException.class)
    public ProblemDetail handleTransferConflict(TransferConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Transfer conflict", exception.getMessage());
    }

    /** Maps transient contention/database failure to a retryable safe response. */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ProblemDetail handleLockFailure(PessimisticLockingFailureException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Transfer temporarily unavailable",
                "The transfer could not acquire the required resources.");
    }

    /** Maps other transient database failures without misclassifying them as lock contention. */
    @ExceptionHandler(TransientDataAccessException.class)
    public ProblemDetail handleTransientDatabaseFailure(TransientDataAccessException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Transfer temporarily unavailable",
                "The transfer could not be completed because persistence is unavailable.");
    }

    /** Maps remaining database failures without leaking SQL or persistence details. */
    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDatabaseFailure(DataAccessException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Transfer temporarily unavailable",
                "The transfer could not be completed because persistence is unavailable.");
    }

    /** Builds a shared client-safe RFC 9457 shape for expected API failures. */
    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
