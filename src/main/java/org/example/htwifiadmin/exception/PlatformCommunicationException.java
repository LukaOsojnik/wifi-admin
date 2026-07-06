package org.example.htwifiadmin.exception;

/** Thrown when the SOAP platform can't be reached or fails unexpectedly. Becomes HTTP 502. */
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
