package org.example.htwifiadmin.validation;

import org.example.htwifiadmin.exception.InvalidWifiConfigurationException;
import org.example.htwifiadmin.rest.model.EncryptionType;
import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.springframework.stereotype.Component;

/**
 * Business rule the schema can't check: a password is required
 * unless the encryption type is OPEN (or not set, which means OPEN).
 */
@Component
public class WifiConfigurationValidator {

    /** Throws InvalidWifiConfigurationException if a required password is missing or blank. */
    public void validate(WifiConfiguration configuration) {
        if (requiresPassword(configuration.getEncryptionType())) {
            String password = configuration.getPassword();
            if (password == null || password.isBlank()) {
                throw new InvalidWifiConfigurationException(
                        "PASSWORD_REQUIRED",
                        "Password is required for encryption type "
                                + configuration.getEncryptionType().getValue());
            }
        }
    }

    /** Every encryption type needs a password except OPEN (and null, which defaults to OPEN). */
    private boolean requiresPassword(EncryptionType encryptionType) {
        return encryptionType != null && encryptionType != EncryptionType.OPEN;
    }
}
