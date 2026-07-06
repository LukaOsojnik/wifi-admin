package org.example.htwifiadmin.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * DB table row for a cached WiFi configuration — one row per CPE.
 * The password is stored in plain text for this task because it has to be readable back;
 * a real deployment would encrypt it.
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
