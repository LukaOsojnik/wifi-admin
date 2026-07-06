package org.example.htwifiadmin.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for cached WiFi configurations, keyed by cpeId (the natural PK).
 * {@code findById} / {@code save} give read-through lookup and write-through upsert for free.
 */
@Repository
public interface WifiConfigurationRepository extends JpaRepository<WifiConfigurationEntity, String> {
}
