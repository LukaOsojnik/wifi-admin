package org.example.htwifiadmin.exception;

/**
 * Thrown when the external SOAP platform cannot be reached or fails unexpectedly
 * (timeout, connection error, unrecognized SOAP fault). Mapped to HTTP 502.
 */
public class PlatformCommunicationException extends RuntimeException {

    private final String code;

    public PlatformCommunicationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
