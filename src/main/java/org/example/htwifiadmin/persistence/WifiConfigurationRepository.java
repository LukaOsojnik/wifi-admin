package org.example.htwifiadmin.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** DB access for cached WiFi configurations, looked up by cpeId. Spring Data provides all methods. */
@Repository
public interface WifiConfigurationRepository extends JpaRepository<WifiConfigurationEntity, String> {
}
