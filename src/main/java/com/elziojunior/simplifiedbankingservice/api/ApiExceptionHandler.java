package com.elziojunior.simplifiedbankingservice.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.elziojunior.simplifiedbankingservice.service.AccountCreationValidationException;

/** Translates expected client errors into a stable and safe RFC 9457 response. */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Maps request-bean constraint violations without exposing internal details. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
        return badRequest("The account creation request is invalid.");
    }

    /** Maps malformed or unreadable JSON without echoing parser or input details. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return badRequest("The request body is invalid or unreadable.");
    }

    /** Maps application validation that cannot be expressed safely at the HTTP DTO. */
    @ExceptionHandler(AccountCreationValidationException.class)
    public ProblemDetail handleAccountValidation(AccountCreationValidationException exception) {
        return badRequest(exception.getMessage());
    }

    /** Builds the shared client-safe 400 shape required by the API error contract. */
    private ProblemDetail badRequest(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid account creation request");
        return problem;
    }
}
