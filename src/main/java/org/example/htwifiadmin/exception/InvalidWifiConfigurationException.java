package org.example.htwifiadmin.exception;

/**
 * Thrown when a WiFi configuration violates a business rule that the schema cannot
 * express (e.g. a password is required for the chosen encryption type). Carries a
 * stable {@code code} for the REST {@code ErrorBody}. Mapped to HTTP 400 by the
 * exception advice (Step 7).
 */
public class InvalidWifiConfigurationException extends RuntimeException {

    private final String code;

    public InvalidWifiConfigurationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
