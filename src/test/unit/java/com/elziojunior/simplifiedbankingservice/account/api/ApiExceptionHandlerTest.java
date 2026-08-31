package com.elziojunior.simplifiedbankingservice.account.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.elziojunior.simplifiedbankingservice.api.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.elziojunior.simplifiedbankingservice.service.AccountCreationValidationException;
import com.elziojunior.simplifiedbankingservice.service.TransferConflictException;
import com.elziojunior.simplifiedbankingservice.service.TransferNotFoundException;
import com.elziojunior.simplifiedbankingservice.service.TransferValidationException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    /** Proves bean-validation failures receive the stable public error message. */
    @Test
    void shouldMapBeanValidationFailure() {
        ProblemDetail problem = handler.handleInvalidRequest(mock(MethodArgumentNotValidException.class));

        assertProblem(problem, "Invalid request", "The request is invalid.");
    }

    /** Proves JSON parsing failures do not expose parser or request details. */
    @Test
    void shouldMapUnreadableRequest() {
        ProblemDetail problem = handler.handleUnreadableRequest(mock(HttpMessageNotReadableException.class));

        assertProblem(problem, "Invalid request", "The request body is invalid or unreadable.");
    }

    /** Proves safe application-validation messages survive translation. */
    @Test
    void shouldMapAccountValidationFailure() {
        ProblemDetail problem = handler.handleAccountValidation(
                new AccountCreationValidationException("Initial balance exceeds the supported monetary range."));

        assertProblem(problem, "Invalid account creation request",
                "Initial balance exceeds the supported monetary range.");
    }

    /** Proves missing, malformed, and application-invalid transfer inputs remain distinct and safe. */
    @Test
    void shouldMapTransferValidationFailures() {
        ProblemDetail missing = handler.handleTransferValidation(mock(MissingRequestHeaderException.class));
        ProblemDetail malformed = handler.handleTransferValidation(mock(MethodArgumentTypeMismatchException.class));
        ProblemDetail invalid = handler.handleTransferValidation(new TransferValidationException("Invalid amount."));

        assertProblem(missing, HttpStatus.BAD_REQUEST, "Invalid transfer request",
                "The Idempotency-Key header is required.");
        assertProblem(malformed, HttpStatus.BAD_REQUEST, "Invalid transfer request",
                "The Idempotency-Key header is invalid.");
        assertProblem(invalid, HttpStatus.BAD_REQUEST, "Invalid transfer request", "Invalid amount.");
    }

    /** Proves missing accounts and business conflicts map to their documented statuses. */
    @Test
    void shouldMapTransferDomainFailures() {
        ProblemDetail missing = handler.handleTransferNotFound(
                new TransferNotFoundException("A transfer account does not exist."));
        ProblemDetail conflict = handler.handleTransferConflict(
                new TransferConflictException("The transfer conflicts with current state."));

        assertProblem(missing, HttpStatus.NOT_FOUND, "Transfer account not found",
                "A transfer account does not exist.");
        assertProblem(conflict, HttpStatus.CONFLICT, "Transfer conflict",
                "The transfer conflicts with current state.");
    }

    /** Proves persistence failures never expose database details. */
    @Test
    void shouldMapDatabaseFailures() {
        ProblemDetail lockFailure = handler.handleLockFailure(
                new CannotAcquireLockException("secret lock detail"));
        ProblemDetail transientFailure = handler.handleTransientDatabaseFailure(
                new TransientDataAccessResourceException("secret database detail"));
        ProblemDetail permanentFailure = handler.handleDatabaseFailure(
                new DataIntegrityViolationException("secret SQL detail"));

        assertProblem(lockFailure, HttpStatus.SERVICE_UNAVAILABLE, "Transfer temporarily unavailable",
                "The transfer could not acquire the required resources.");
        assertProblem(transientFailure, HttpStatus.SERVICE_UNAVAILABLE, "Transfer temporarily unavailable",
                "The transfer could not be completed because persistence is unavailable.");
        assertProblem(permanentFailure, HttpStatus.SERVICE_UNAVAILABLE, "Transfer temporarily unavailable",
                "The transfer could not be completed because persistence is unavailable.");
    }

    private void assertProblem(ProblemDetail problem, String title, String detail) {
        assertProblem(problem, HttpStatus.BAD_REQUEST, title, detail);
    }

    private void assertProblem(ProblemDetail problem, HttpStatus status, String title, String detail) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getTitle()).isEqualTo(title);
        assertThat(problem.getDetail()).isEqualTo(detail);
    }
}
