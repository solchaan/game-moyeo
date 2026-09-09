package com.gamemoyeo.common.exception;

import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.util.stream.Collectors;
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
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
@Order(-1)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_BASE_URL = "https://api.gamemoyeo.com/problems/";

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException exception, HttpServletRequest request) {
        ProblemDetail problem = createProblem(
            exception.status(), exception.code(), exception.getMessage(), request.getRequestURI());
        if (!exception.fieldErrors().isEmpty()) {
            problem.setProperty("fieldErrors", exception.fieldErrors().entrySet().stream()
                .map(entry -> new FieldErrorDetail(entry.getKey(), entry.getValue())).toList());
        }
        return ResponseEntity.status(exception.status()).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception, HttpHeaders headers,
        HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = createProblem(
            HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "입력한 항목을 확인해 주세요.", ((ServletWebRequest) request).getRequest().getRequestURI());
        List<FieldErrorDetail> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
            .toList();
        problem.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException exception,
        HttpHeaders headers, HttpStatusCode status,
        WebRequest request) {
        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
            "입력 형식이 올바르지 않습니다. 날짜와 숫자 항목을 확인해 주세요.",
            ((ServletWebRequest) request).getRequest().getRequestURI());
        if (exception.getCause() instanceof JsonMappingException mapping) {
            String field = mapping.getPath().stream()
                .map(reference -> reference.getFieldName() == null ? "[" + reference.getIndex() + "]" : reference.getFieldName())
                .collect(Collectors.joining(".")).replace(".[", "[");
            if (!field.isBlank()) {
                problem.setProperty("fieldErrors", List.of(new FieldErrorDetail(field, "입력 형식을 확인해 주세요.")));
            }
        }
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
        DataIntegrityViolationException exception,
        HttpServletRequest request
    ) {
        log.warn("Database constraint violation: {}", exception.getMostSpecificCause().getMessage());
        ProblemDetail problem = createProblem(
            HttpStatus.CONFLICT, "RESOURCE_CONFLICT", "The resource conflicts with existing data.",
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<ProblemDetail> handleAuthorizationDenied(
        AuthorizationDeniedException exception,
        HttpServletRequest request
    ) {
        ProblemDetail problem = createProblem(
            HttpStatus.FORBIDDEN,
            "FORBIDDEN",
            "You do not have permission to perform this action.",
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
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
