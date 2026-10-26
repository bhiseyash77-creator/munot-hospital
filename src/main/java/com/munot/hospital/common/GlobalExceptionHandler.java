package com.munot.hospital.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ErrorResponse> build(HttpStatus s, String msg, Map<String, String> fields) {
        return ResponseEntity.status(s).body(new ErrorResponse(LocalDateTime.now(), s.value(), msg, fields));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> api(ApiException e) { return build(e.getStatus(), e.getMessage(), null); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraint(ConstraintViolationException e) {
        return build(HttpStatus.BAD_REQUEST, "Validation failed", null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> badInput(Exception e) { return build(HttpStatus.BAD_REQUEST, "Invalid request", null); }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> method(Exception e) { return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", null); }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> denied(AccessDeniedException e) { return build(HttpStatus.FORBIDDEN, "Access denied", null); }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> auth(AuthenticationException e) { return build(HttpStatus.UNAUTHORIZED, "Authentication required", null); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e) {
        String cause = String.valueOf(e.getMostSpecificCause().getMessage());
        log.warn("Data integrity violation: {}", cause);
        if (cause.contains("Data too long")) return build(HttpStatus.BAD_REQUEST, "One of the values is too long. Please shorten it and try again.", null);
        return build(HttpStatus.CONFLICT, "This record conflicts with existing data (duplicate or in use)", null);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> lock(Exception e) { return build(HttpStatus.CONFLICT, "System busy, please retry", null); }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> size(Exception e) { return build(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large (max 15 MB)", null); }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> other(Exception e) {
        log.error("Unhandled error", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", null);
    }
}
