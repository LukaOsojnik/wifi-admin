package org.example.htwifiadmin.validation;

import org.example.htwifiadmin.exception.InvalidWifiConfigurationException;
import org.example.htwifiadmin.rest.model.EncryptionType;
import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.springframework.stereotype.Component;

/**
 * Business-rule validation that OpenAPI/XSD cannot express (a cross-field rule):
 * a password is required when the encryption type uses a key.
 *
 * <p>Rule (option a): OPEN — or an omitted encryption type (defaults to OPEN) — needs no
 * password; every other type does. Note: WPA2_ENTERPRISE technically uses 802.1X credentials
 * rather than a shared password; if you want to exempt it, exclude it in {@link #requiresPassword}.
 */
@Component
public class WifiConfigurationValidator {

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

    private boolean requiresPassword(EncryptionType encryptionType) {
        // null encryptionType defaults to OPEN -> no password needed.
        return encryptionType != null && encryptionType != EncryptionType.OPEN;
    }
}
