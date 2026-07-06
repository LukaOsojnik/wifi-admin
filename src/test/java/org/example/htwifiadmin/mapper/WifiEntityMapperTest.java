package org.example.htwifiadmin.mapper;

import org.example.htwifiadmin.persistence.WifiConfigurationEntity;
import org.example.htwifiadmin.rest.model.EncryptionType;
import org.example.htwifiadmin.rest.model.WifiBand;
import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WifiEntityMapperTest {

    private final WifiEntityMapper mapper = new WifiEntityMapper();

    @Test
    void toEntity_storesEnumsAsStringValue() {
        WifiConfiguration rest = new WifiConfiguration();
        rest.setCpeId("CPE_001");
        rest.setWifiBand(WifiBand.BAND_2_4_GHZ);
        rest.setSsid("Office-2G");
        rest.setEncryptionType(EncryptionType.WPA2_PSK);
        rest.setPassword("pw");

        WifiConfigurationEntity entity = mapper.toEntity(rest);

        assertThat(entity.getCpeId()).isEqualTo("CPE_001");
        assertThat(entity.getWifiBand()).isEqualTo("BAND_2_4_GHZ");
        assertThat(entity.getEncryptionType()).isEqualTo("WPA2_PSK");
        assertThat(entity.getPassword()).isEqualTo("pw");
    }

    @Test
    void toRest_parsesEnumsFromStringValue() {
        WifiConfigurationEntity entity = new WifiConfigurationEntity();
        entity.setCpeId("CPE_005");
        entity.setWifiBand("BAND_5_GHZ");
        entity.setSsid("Office-5G");
        entity.setEncryptionType("WPA3_SAE");
        entity.setPassword("pw");

        WifiConfiguration rest = mapper.toRest(entity);

        assertThat(rest.getWifiBand()).isEqualTo(WifiBand.BAND_5_GHZ);
        assertThat(rest.getEncryptionType()).isEqualTo(EncryptionType.WPA3_SAE);
    }

    @Test
    void handlesNullOptionalFields() {
        WifiConfigurationEntity entity = new WifiConfigurationEntity();
        entity.setCpeId("CPE_003");
        entity.setWifiBand("BAND_2_4_GHZ");
        entity.setSsid("Guest");
        // encryptionType + password null (OPEN network)

        WifiConfiguration rest = mapper.toRest(entity);

        assertThat(rest.getEncryptionType()).isNull();
        assertThat(rest.getPassword()).isNull();
    }

    @Test
    void nullInput_mapsToNull() {
        assertThat(mapper.toRest(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }
}
