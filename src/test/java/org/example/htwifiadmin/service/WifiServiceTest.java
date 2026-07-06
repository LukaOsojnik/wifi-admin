package org.example.htwifiadmin.service;

import jakarta.xml.ws.WebServiceException;
import org.example.htwifiadmin.exception.CpeNotFoundException;
import org.example.htwifiadmin.exception.PlatformCommunicationException;
import org.example.htwifiadmin.mapper.WifiConfigurationMapper;
import org.example.htwifiadmin.mapper.WifiEntityMapper;
import org.example.htwifiadmin.persistence.WifiConfigurationEntity;
import org.example.htwifiadmin.persistence.WifiConfigurationRepository;
import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.example.htwifiadmin.soap.GetCpeIdResponse;
import org.example.htwifiadmin.soap.WifiBandType;
import org.example.htwifiadmin.soap.WifiConfigurationType;
import org.example.htwifiadmin.soap.WifiPlatformPortType;
import org.example.htwifiadmin.validation.WifiConfigurationValidator;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WifiServiceTest {

    private final WifiPlatformPortType port = mock(WifiPlatformPortType.class);
    private final WifiConfigurationRepository repository = mock(WifiConfigurationRepository.class);
    private final WifiService service = new WifiService(
            port, new WifiConfigurationMapper(), new WifiConfigurationValidator(),
            repository, new WifiEntityMapper());

    @Test
    void getWifiParameter_cacheMiss_fetchesFromPlatformAndCaches() {
        when(repository.findById("CPE_001")).thenReturn(Optional.empty());
        WifiConfigurationType config = new WifiConfigurationType();
        config.setCpeId("CPE_001");
        config.setWifiBand(WifiBandType.BAND_2_4_GHZ);
        config.setSsid("Office-2G");
        GetCpeIdResponse response = new GetCpeIdResponse();
        response.setConfiguration(config);
        when(port.getCpeID(any())).thenReturn(response);

        WifiConfiguration result = service.getWifiParameter("CPE_001");

        assertThat(result.getSsid()).isEqualTo("Office-2G");
        verify(repository).save(any(WifiConfigurationEntity.class)); // write-through on miss
    }

    @Test
    void getWifiParameter_cacheHit_servesFromDbWithoutCallingPlatform() {
        WifiConfigurationEntity entity = new WifiConfigurationEntity();
        entity.setCpeId("CPE_001");
        entity.setWifiBand("BAND_2_4_GHZ");
        entity.setSsid("Cached-SSID");
        when(repository.findById("CPE_001")).thenReturn(Optional.of(entity));

        WifiConfiguration result = service.getWifiParameter("CPE_001");

        assertThat(result.getSsid()).isEqualTo("Cached-SSID");
        verify(port, never()).getCpeID(any()); // platform not touched on a hit
    }

    @Test
    void getWifiParameter_notFoundFault_throwsCpeNotFound() {
        when(repository.findById("NOPE")).thenReturn(Optional.empty());
        // mimics the mock's malformed fault surfacing through CXF
        when(port.getCpeID(any()))
                .thenThrow(new WebServiceException(
                        new RuntimeException("Invalid QName in mapping: tns:NotFound")));

        assertThatThrownBy(() -> service.getWifiParameter("NOPE"))
                .isInstanceOf(CpeNotFoundException.class);
        verify(repository, never()).save(any()); // a miss is not cached
    }

    @Test
    void getWifiParameter_transportFailure_throwsPlatformCommunication() {
        when(repository.findById("CPE_001")).thenReturn(Optional.empty());
        when(port.getCpeID(any()))
                .thenThrow(new WebServiceException("Could not send Message."));

        assertThatThrownBy(() -> service.getWifiParameter("CPE_001"))
                .isInstanceOf(PlatformCommunicationException.class);
    }
}
