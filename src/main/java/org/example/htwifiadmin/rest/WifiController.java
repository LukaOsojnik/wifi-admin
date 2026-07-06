package org.example.htwifiadmin.rest;

import org.example.htwifiadmin.rest.api.WiFiApi;
import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.example.htwifiadmin.service.WifiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin HTTP adapter implementing the generated OpenAPI contract {@link WiFiApi}.
 * Delegates the actual work to {@link WifiService} and only wraps results in HTTP
 * responses. Schema-level validation (@NotNull / @Valid) is inherited from WiFiApi.
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
