package org.example.htwifiadmin.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the cached WiFi configuration (one row per CPE). Lives only behind the
 * repository/persistence boundary — never exposed to the REST or SOAP layers; the
 * {@code WifiEntityMapper} converts at the edge.
 *
 * <p>Enums are stored as their stable string value (not ordinals). The password is stored
 * as-is for this task because it must be retrievable (shown to the user, pushed to the device);
 * a real deployment would ENCRYPT it at rest (not hash — hashing is irreversible).
 */
@Entity
@Table(name = "wifi_configuration")
public class WifiConfigurationEntity {

    @Id
    @Column(name = "cpe_id")
    private String cpeId;

    @Column(name = "wifi_band")
    private String wifiBand;

    @Column(name = "ssid")
    private String ssid;

    @Column(name = "encryption_type")
    private String encryptionType;

    @Column(name = "password")
    private String password;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    public String getCpeId() {
        return cpeId;
    }

    public void setCpeId(String cpeId) {
        this.cpeId = cpeId;
    }

    public String getWifiBand() {
        return wifiBand;
    }

    public void setWifiBand(String wifiBand) {
        this.wifiBand = wifiBand;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getEncryptionType() {
        return encryptionType;
    }

    public void setEncryptionType(String encryptionType) {
        this.encryptionType = encryptionType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
