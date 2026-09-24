package com.store.prices.infrastructure.adapter.in.rest;

import com.store.prices.domain.exception.PriceNotFoundException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into RFC 9457 problem details. The base class keeps Spring MVC's own errors (unknown path,
 * unsupported method, missing parameter…) at their proper status, so only unexpected errors reach the 500 handler.
 * Every response goes through {@link #createResponseEntity}, which is the one place that completes the body.
 */
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    private static final URI ABOUT_BLANK = URI.create("about:blank");

    // The API shows the pattern without the quotes that DateTimeFormatter needs around the T.
    private static final String DISPLAYED_DATE_TIME_PATTERN = PriceController.DATE_TIME_PATTERN.replace("'", "");

    @ExceptionHandler(PriceNotFoundException.class)
    public ResponseEntity<Object> handlePriceNotFound(PriceNotFoundException exception, WebRequest request) {
        return problem(exception, new HttpHeaders(), HttpStatus.NOT_FOUND, exception.getMessage() + ".", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
        log.error("Unexpected error", exception);
        return problem(exception, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.",
                request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String expected = exception.getRequiredType() == LocalDateTime.class
                ? "match " + DISPLAYED_DATE_TIME_PATTERN
                : "be an integer";
        return problem(exception, headers, status,
                "Parameter '%s' must %s.".formatted(exception.getPropertyName(), expected), request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> "Parameter '%s' %s.".formatted(result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .collect(Collectors.joining(" "));
        return problem(exception, headers, status, detail, request);
    }

    /**
     * Spring 7 leaves {@code type} out when it is not set, but the contract requires it, so it is set here for every
     * problem, Spring's own included.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problem && problem.getType() == null) {
            problem.setType(ABOUT_BLANK);
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    private ResponseEntity<Object> problem(Exception exception, HttpHeaders headers, HttpStatusCode status,
            String detail, WebRequest request) {
        return handleExceptionInternal(exception, ProblemDetail.forStatusAndDetail(status, detail), headers, status,
                request);
    }
}
