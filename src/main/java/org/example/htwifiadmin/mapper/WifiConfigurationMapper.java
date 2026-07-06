package org.example.htwifiadmin.mapper;

import org.example.htwifiadmin.rest.model.EncryptionType;
import org.example.htwifiadmin.rest.model.WifiBand;
import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.example.htwifiadmin.soap.WifiBandType;
import org.example.htwifiadmin.soap.WifiConfigurationType;
import org.springframework.stereotype.Component;

/**
 * Translates the WiFi configuration between the two generated worlds:
 * SOAP ({@link WifiConfigurationType}) and REST ({@link WifiConfiguration}).
 * This is the only place the two models meet — controllers/services stay on the
 * REST types, the SOAP client stays on the SOAP types.
 *
 * <p>Enums are bridged by their <em>wire value</em>, not {@code name()}: JAXB names the
 * SOAP constants {@code WPA_2_PSK} while the REST constant is {@code WPA2_PSK}, yet both
 * serialize to the same string {@code "WPA2_PSK"}. Mapping by name would fail on those.
 */
@Component
public class WifiConfigurationMapper {

    /** SOAP -> REST (unwrap a platform response). */
    public WifiConfiguration toRest(WifiConfigurationType soap) {
        if (soap == null) {
            return null;
        }
        WifiConfiguration rest = new WifiConfiguration();
        rest.setCpeId(soap.getCpeId());
        rest.setWifiBand(bandToRest(soap.getWifiBand()));
        rest.setSsid(soap.getSsid());
        rest.setEncryptionType(encToRest(soap.getEncryptionType()));
        rest.setPassword(soap.getPassword());
        return rest;
    }

    /** REST -> SOAP (wrap an incoming request). */
    public WifiConfigurationType toSoap(WifiConfiguration rest) {
        if (rest == null) {
            return null;
        }
        WifiConfigurationType soap = new WifiConfigurationType();
        soap.setCpeId(rest.getCpeId());
        soap.setWifiBand(bandToSoap(rest.getWifiBand()));
        soap.setSsid(rest.getSsid());
        soap.setEncryptionType(encToSoap(rest.getEncryptionType()));
        soap.setPassword(rest.getPassword());
        return soap;
    }

    // --- enum bridges: null-guarded, mapped by wire value ---

    private WifiBand bandToRest(WifiBandType soap) {
        return soap == null ? null : WifiBand.fromValue(soap.value());
    }

    private WifiBandType bandToSoap(WifiBand rest) {
        return rest == null ? null : WifiBandType.fromValue(rest.getValue());
    }

    private EncryptionType encToRest(org.example.htwifiadmin.soap.EncryptionType soap) {
        return soap == null ? null : EncryptionType.fromValue(soap.value());
    }

    private org.example.htwifiadmin.soap.EncryptionType encToSoap(EncryptionType rest) {
        return rest == null ? null : org.example.htwifiadmin.soap.EncryptionType.fromValue(rest.getValue());
    }
}
