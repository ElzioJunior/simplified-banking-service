package com.elziojunior.simplifiedbankingservice.account.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.elziojunior.simplifiedbankingservice.api.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.elziojunior.simplifiedbankingservice.service.AccountCreationValidationException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    /** Proves bean-validation failures receive the stable public error message. */
    @Test
    void shouldMapBeanValidationFailure() {
        ProblemDetail problem = handler.handleInvalidRequest(mock(MethodArgumentNotValidException.class));

        assertProblem(problem, "The account creation request is invalid.");
    }

    /** Proves JSON parsing failures do not expose parser or request details. */
    @Test
    void shouldMapUnreadableRequest() {
        ProblemDetail problem = handler.handleUnreadableRequest(mock(HttpMessageNotReadableException.class));

        assertProblem(problem, "The request body is invalid or unreadable.");
    }

    /** Proves safe application-validation messages survive translation. */
    @Test
    void shouldMapAccountValidationFailure() {
        ProblemDetail problem = handler.handleAccountValidation(
                new AccountCreationValidationException("Initial balance exceeds the supported monetary range."));

        assertProblem(problem, "Initial balance exceeds the supported monetary range.");
    }

    private void assertProblem(ProblemDetail problem, String detail) {
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid account creation request");
        assertThat(problem.getDetail()).isEqualTo(detail);
    }
}
