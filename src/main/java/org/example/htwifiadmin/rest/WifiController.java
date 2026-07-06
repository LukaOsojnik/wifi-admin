package org.example.htwifiadmin.rest;

import org.example.htwifiadmin.rest.api.WiFiApi;
import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.example.htwifiadmin.service.WifiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for the WiFi API. Just passes requests to the service
 * and wraps the result in an HTTP response — no logic of its own.
 */
@RestController
public class WifiController implements WiFiApi {

    private final WifiService service;

    public WifiController(WifiService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<WifiConfiguration> getWifiParameter(String cpeId) {
        return ResponseEntity.ok(service.getWifiParameter(cpeId));
    }

    @Override
    public ResponseEntity<WifiConfiguration> putWifiParameter(WifiConfiguration wifiConfiguration) {
        return ResponseEntity.ok(service.updateWifiParameter(wifiConfiguration));
    }
}
