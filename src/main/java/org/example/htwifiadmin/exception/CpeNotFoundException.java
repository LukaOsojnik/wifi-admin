package org.example.htwifiadmin.exception;

/** Thrown when the platform says the requested CPE doesn't exist. Becomes HTTP 404. */
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
