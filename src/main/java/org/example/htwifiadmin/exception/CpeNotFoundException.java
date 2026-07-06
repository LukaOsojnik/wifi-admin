package org.example.htwifiadmin.exception;

/**
 * Thrown when the platform reports that the requested CPE does not exist.
 * Mapped to HTTP 404 by the exception advice.
 */
public class CpeNotFoundException extends RuntimeException {

    private final String code;

    public CpeNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
