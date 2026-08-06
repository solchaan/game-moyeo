package com.gamemoyeo.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_BASE_URL = "https://api.gamemoyeo.com/problems/";

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException exception, HttpServletRequest request) {
        ProblemDetail problem = createProblem(
            exception.status(), exception.code(), exception.getMessage(), request.getRequestURI());
        return ResponseEntity.status(exception.status()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleInvalidArgument(
        MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = createProblem(
            HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.", request.getRequestURI());
        List<FieldErrorDetail> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
            .toList();
        problem.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(
        ConstraintViolationException exception, HttpServletRequest request) {
        ProblemDetail problem = createProblem(
            HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION", "Request constraint was violated.",
            request.getRequestURI());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        log.error("Unhandled API exception", exception);
        ProblemDetail problem = createProblem(
            HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred.",
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ProblemDetail createProblem(HttpStatus status, String code, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create(PROBLEM_BASE_URL + code.toLowerCase().replace('_', '-')));
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", code);
        problem.setProperty("traceId", MDC.get("traceId"));
        return problem;
    }

    record FieldErrorDetail(String field, String reason) {
    }
}
