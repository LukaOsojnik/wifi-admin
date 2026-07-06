package org.example.htwifiadmin.exception;

/** Thrown when a config breaks a business rule (e.g. missing password). Becomes HTTP 400. */
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
