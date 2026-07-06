package org.example.htwifiadmin.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.example.htwifiadmin.persistence.WifiConfigurationEntity;
import org.example.htwifiadmin.persistence.WifiConfigurationRepository;
import org.example.htwifiadmin.service.WifiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Nightly job that refreshes the DB cache from the platform. Thin — it decides only
 * <i>when</i> (cron) and <i>which/how many</i> (the N stalest cached CPEs); the per-CPE
 * SOAP+DB work is delegated to {@link WifiService#refreshFromPlatform(String)}.
 *
 * <p>DB-driven (Option X): it refreshes CPEs already in the cache (populated by GETs), oldest
 * {@code lastUpdated} first, so entries rotate through over successive runs. It does not discover
 * never-accessed CPEs. Disabled in tests via {@code sync.enabled=false}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sync.enabled", havingValue = "true", matchIfMissing = true)
public class WifiSyncScheduler {

    private final WifiConfigurationRepository repository;
    private final WifiService wifiService;
    private final int batchSize;

    public WifiSyncScheduler(WifiConfigurationRepository repository, WifiService wifiService,
                             @Value("${sync.batch-size:50}") int batchSize) {
        this.repository = repository;
        this.wifiService = wifiService;
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "${sync.cron}")
    public void syncStaleCpes() {
        List<WifiConfigurationEntity> batch = repository
                .findAll(PageRequest.of(0, batchSize, Sort.by("lastUpdated").ascending()))
                .getContent();

        if (batch.isEmpty()) {
            log.info("Nightly sync: no CPEs in cache to refresh");
            return;
        }

        log.info("Nightly sync: refreshing {} CPE(s) from platform", batch.size());
        int synced = 0;
        int failed = 0;
        for (WifiConfigurationEntity entity : batch) {
            String cpeId = entity.getCpeId();
            try {
                wifiService.refreshFromPlatform(cpeId);
                synced++;
            } catch (Exception e) {
                // One bad CPE must not abort the whole run.
                failed++;
                log.warn("Nightly sync failed for cpeId={}: {}", cpeId, e.getMessage());
            }
        }
        log.info("Nightly sync complete: synced={}, failed={}", synced, failed);
    }
}
