package org.example.htwifiadmin.mapper;

import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.example.htwifiadmin.soap.WifiBandType;
import org.example.htwifiadmin.soap.WifiConfigurationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link WifiConfigurationMapper} — no Spring context, no mock.
 * The key assertion is the WPA2_PSK round-trip: it proves enums map by wire value
 * (SOAP name WPA_2_PSK vs REST name WPA2_PSK) rather than by name().
 */
class WifiConfigurationMapperTest {

    private final WifiConfigurationMapper mapper = new WifiConfigurationMapper();

    @Test
    void toRest_mapsAllFields_includingTrickyEncryptionEnum() {
        WifiConfigurationType soap = new WifiConfigurationType();
        soap.setCpeId("CPE_001");
        soap.setWifiBand(WifiBandType.BAND_2_4_GHZ);
        soap.setSsid("Office-2G");
        soap.setEncryptionType(org.example.htwifiadmin.soap.EncryptionType.WPA_2_PSK);
        soap.setPassword("seed-wifi-01");

        WifiConfiguration rest = mapper.toRest(soap);

        assertThat(rest.getCpeId()).isEqualTo("CPE_001");
        assertThat(rest.getWifiBand()).isEqualTo(org.example.htwifiadmin.rest.model.WifiBand.BAND_2_4_GHZ);
        assertThat(rest.getSsid()).isEqualTo("Office-2G");
        assertThat(rest.getEncryptionType())
                .isEqualTo(org.example.htwifiadmin.rest.model.EncryptionType.WPA2_PSK);
        assertThat(rest.getPassword()).isEqualTo("seed-wifi-01");
    }

    @Test
    void toSoap_mapsAllFields_includingTrickyEncryptionEnum() {
        WifiConfiguration rest = new WifiConfiguration();
        rest.setCpeId("CPE_005");
        rest.setWifiBand(org.example.htwifiadmin.rest.model.WifiBand.BAND_5_GHZ);
        rest.setSsid("Office-5G");
        rest.setEncryptionType(org.example.htwifiadmin.rest.model.EncryptionType.WPA2_PSK);
        rest.setPassword("pw");

        WifiConfigurationType soap = mapper.toSoap(rest);

        assertThat(soap.getCpeId()).isEqualTo("CPE_005");
        assertThat(soap.getWifiBand()).isEqualTo(WifiBandType.BAND_5_GHZ);
        assertThat(soap.getSsid()).isEqualTo("Office-5G");
        assertThat(soap.getEncryptionType())
                .isEqualTo(org.example.htwifiadmin.soap.EncryptionType.WPA_2_PSK);
        assertThat(soap.getPassword()).isEqualTo("pw");
    }

    @Test
    void roundTrip_soapToRestToSoap_preservesValues() {
        WifiConfigurationType original = new WifiConfigurationType();
        original.setCpeId("CPE_009");
        original.setWifiBand(WifiBandType.BAND_5_GHZ);
        original.setSsid("Lab");
        original.setEncryptionType(org.example.htwifiadmin.soap.EncryptionType.WPA_3_SAE);
        original.setPassword("secret");

        WifiConfigurationType back = mapper.toSoap(mapper.toRest(original));

        assertThat(back.getCpeId()).isEqualTo("CPE_009");
        assertThat(back.getWifiBand()).isEqualTo(WifiBandType.BAND_5_GHZ);
        assertThat(back.getSsid()).isEqualTo("Lab");
        assertThat(back.getEncryptionType()).isEqualTo(org.example.htwifiadmin.soap.EncryptionType.WPA_3_SAE);
        assertThat(back.getPassword()).isEqualTo("secret");
    }

    @Test
    void handlesNulls_openNetworkWithoutEncryptionOrPassword() {
        WifiConfigurationType soap = new WifiConfigurationType();
        soap.setCpeId("CPE_003");
        soap.setWifiBand(WifiBandType.BAND_2_4_GHZ);
        soap.setSsid("Guest");
        // encryptionType + password left null (OPEN network)

        WifiConfiguration rest = mapper.toRest(soap);

        assertThat(rest.getEncryptionType()).isNull();
        assertThat(rest.getPassword()).isNull();
        assertThat(rest.getCpeId()).isEqualTo("CPE_003");
    }

    @Test
    void nullConfiguration_mapsToNull() {
        assertThat(mapper.toRest(null)).isNull();
        assertThat(mapper.toSoap(null)).isNull();
    }
}
