package org.example.htwifiadmin.validation;

import org.example.htwifiadmin.exception.InvalidWifiConfigurationException;
import org.example.htwifiadmin.rest.model.EncryptionType;
import org.example.htwifiadmin.rest.model.WifiBand;
import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WifiConfigurationValidatorTest {

    private final WifiConfigurationValidator validator = new WifiConfigurationValidator();

    private WifiConfiguration base(EncryptionType enc, String password) {
        WifiConfiguration c = new WifiConfiguration();
        c.setCpeId("CPE_001");
        c.setWifiBand(WifiBand.BAND_2_4_GHZ);
        c.setSsid("Net");
        c.setEncryptionType(enc);
        c.setPassword(password);
        return c;
    }

    @Test
    void open_withoutPassword_isValid() {
        assertThatCode(() -> validator.validate(base(EncryptionType.OPEN, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void omittedEncryption_defaultsToOpen_isValid() {
        assertThatCode(() -> validator.validate(base(null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void wpa2Psk_withPassword_isValid() {
        assertThatCode(() -> validator.validate(base(EncryptionType.WPA2_PSK, "secret")))
                .doesNotThrowAnyException();
    }

    @Test
    void wpa2Psk_withoutPassword_throwsWithCode() {
        assertThatExceptionOfType(InvalidWifiConfigurationException.class)
                .isThrownBy(() -> validator.validate(base(EncryptionType.WPA2_PSK, null)))
                .satisfies(e -> assertThat(e.getCode()).isEqualTo("PASSWORD_REQUIRED"));
    }

    @Test
    void wpa2Psk_withBlankPassword_throws() {
        assertThatThrownBy(() -> validator.validate(base(EncryptionType.WPA2_PSK, "   ")))
                .isInstanceOf(InvalidWifiConfigurationException.class);
    }

    @Test
    void wpa2Enterprise_withoutPassword_throws_optionA() {
        assertThatThrownBy(() -> validator.validate(base(EncryptionType.WPA2_ENTERPRISE, null)))
                .isInstanceOf(InvalidWifiConfigurationException.class);
    }
}
