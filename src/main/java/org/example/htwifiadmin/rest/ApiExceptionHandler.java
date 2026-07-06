package org.example.htwifiadmin.rest;

import lombok.extern.slf4j.Slf4j;
import org.example.htwifiadmin.exception.CpeNotFoundException;
import org.example.htwifiadmin.exception.InvalidWifiConfigurationException;
import org.example.htwifiadmin.exception.PlatformCommunicationException;
import org.example.htwifiadmin.rest.model.ErrorBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Turns exceptions into JSON error responses with the right HTTP status.
 * Handles only our own exceptions and Spring validation errors — SOAP errors
 * never reach this class because the service already converts them.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Business rule broken (e.g. missing password) -> 400. */
    @ExceptionHandler(InvalidWifiConfigurationException.class)
    public ResponseEntity<ErrorBody> handleInvalidConfiguration(InvalidWifiConfigurationException e) {
        log.warn("Invalid WiFi configuration [{}]: {}", e.getCode(), e.getMessage());
        return build(HttpStatus.BAD_REQUEST, e.getCode(), e.getMessage());
    }

    /** Invalid request body (e.g. missing required field) -> 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleBodyValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("Invalid request body");
        log.warn("Request body validation failed: {}", message);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    /** Invalid request parameter (e.g. bad path variable) -> 400. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorBody> handleParamValidation(HandlerMethodValidationException e) {
        log.warn("Request parameter validation failed: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request parameters");
    }

    /** CPE doesn't exist on the platform -> 404. */
    @ExceptionHandler(CpeNotFoundException.class)
    public ResponseEntity<ErrorBody> handleNotFound(CpeNotFoundException e) {
        log.warn("CPE not found [{}]: {}", e.getCode(), e.getMessage());
        return build(HttpStatus.NOT_FOUND, e.getCode(), e.getMessage());
    }

    /** Platform unreachable or failed unexpectedly -> 502. */
    @ExceptionHandler(PlatformCommunicationException.class)
    public ResponseEntity<ErrorBody> handlePlatform(PlatformCommunicationException e) {
        log.error("Platform communication failure [{}]: {}", e.getCode(), e.getMessage(), e);
        return build(HttpStatus.BAD_GATEWAY, e.getCode(), e.getMessage());
    }

    private ResponseEntity<ErrorBody> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorBody().code(code).message(message));
    }
}
