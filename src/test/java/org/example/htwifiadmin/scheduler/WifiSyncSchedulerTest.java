package org.example.htwifiadmin.scheduler;

import org.example.htwifiadmin.persistence.WifiConfigurationEntity;
import org.example.htwifiadmin.persistence.WifiConfigurationRepository;
import org.example.htwifiadmin.service.WifiService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WifiSyncSchedulerTest {

    private final WifiConfigurationRepository repository = mock(WifiConfigurationRepository.class);
    private final WifiService wifiService = mock(WifiService.class);
    private final WifiSyncScheduler scheduler = new WifiSyncScheduler(repository, wifiService, 50);

    private WifiConfigurationEntity entity(String cpeId) {
        WifiConfigurationEntity e = new WifiConfigurationEntity();
        e.setCpeId(cpeId);
        return e;
    }

    @Test
    void refreshesEachCpeInBatch() {
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity("CPE_001"), entity("CPE_002"))));

        scheduler.syncStaleCpes();

        verify(wifiService).refreshFromPlatform("CPE_001");
        verify(wifiService).refreshFromPlatform("CPE_002");
    }

    @Test
    void oneFailure_doesNotAbortTheRest() {
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity("CPE_001"), entity("CPE_002"))));
        when(wifiService.refreshFromPlatform("CPE_001"))
                .thenThrow(new RuntimeException("platform down"));

        scheduler.syncStaleCpes();

        // CPE_002 still processed despite CPE_001 failing
        verify(wifiService).refreshFromPlatform("CPE_002");
    }

    @Test
    void emptyCache_doesNothing() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        scheduler.syncStaleCpes();

        verify(wifiService, never()).refreshFromPlatform(any());
    }
}
