package org.example.htwifiadmin.mapper;

import org.example.htwifiadmin.rest.model.EncryptionType;
import org.example.htwifiadmin.rest.model.WifiBand;
import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.example.htwifiadmin.soap.WifiBandType;
import org.example.htwifiadmin.soap.WifiConfigurationType;
import org.springframework.stereotype.Component;

/**
 * Converts between the SOAP model and the REST model.
 * Enums are matched by their string value (not the Java constant name),
 * because the two generated models name their constants differently.
 */
@Component
public class WifiConfigurationMapper {

    /** Converts a SOAP config (platform response) into the REST model. */
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

    /** Converts a REST config into the SOAP model for a platform request. */
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

    // Enum conversions below: null stays null, otherwise match by string value.

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
