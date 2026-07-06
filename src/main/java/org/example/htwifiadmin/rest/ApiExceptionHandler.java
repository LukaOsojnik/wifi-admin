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
 * Maps exceptions to the OpenAPI {@link ErrorBody} contract with the correct HTTP status.
 * It knows only domain exceptions + Spring validation exceptions — no SOAP/CXF types leak
 * here, because {@code WifiService} already translates transport failures into domain exceptions.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Business-rule violation (e.g. password required for encryption type). */
    @ExceptionHandler(InvalidWifiConfigurationException.class)
    public ResponseEntity<ErrorBody> handleInvalidConfiguration(InvalidWifiConfigurationException e) {
        log.warn("Invalid WiFi configuration [{}]: {}", e.getCode(), e.getMessage());
        return build(HttpStatus.BAD_REQUEST, e.getCode(), e.getMessage());
    }

    /** Schema validation on the request body (@Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleBodyValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("Invalid request body");
        log.warn("Request body validation failed: {}", message);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    /** Schema validation on method parameters (e.g. @NotNull path variable). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorBody> handleParamValidation(HandlerMethodValidationException e) {
        log.warn("Request parameter validation failed: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request parameters");
    }

    /** Unknown cpeId reported by the platform. */
    @ExceptionHandler(CpeNotFoundException.class)
    public ResponseEntity<ErrorBody> handleNotFound(CpeNotFoundException e) {
        log.warn("CPE not found [{}]: {}", e.getCode(), e.getMessage());
        return build(HttpStatus.NOT_FOUND, e.getCode(), e.getMessage());
    }

    /** Platform unreachable / timeout / unexpected SOAP fault. */
    @ExceptionHandler(PlatformCommunicationException.class)
    public ResponseEntity<ErrorBody> handlePlatform(PlatformCommunicationException e) {
        log.error("Platform communication failure [{}]: {}", e.getCode(), e.getMessage(), e);
        return build(HttpStatus.BAD_GATEWAY, e.getCode(), e.getMessage());
    }

    private ResponseEntity<ErrorBody> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorBody().code(code).message(message));
    }
}
