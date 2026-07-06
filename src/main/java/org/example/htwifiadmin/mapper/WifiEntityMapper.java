package org.example.htwifiadmin.mapper;

import org.example.htwifiadmin.persistence.WifiConfigurationEntity;
import org.example.htwifiadmin.rest.model.EncryptionType;
import org.example.htwifiadmin.rest.model.WifiBand;
import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.springframework.stereotype.Component;

/**
 * Converts between the DB entity and the REST model.
 * Enums are stored as plain strings in the DB; lastUpdated is set by the service, not here.
 */
@Component
public class WifiEntityMapper {

    /** Converts a DB row into the REST model. */
    public WifiConfiguration toRest(WifiConfigurationEntity entity) {
        if (entity == null) {
            return null;
        }
        WifiConfiguration rest = new WifiConfiguration();
        rest.setCpeId(entity.getCpeId());
        rest.setWifiBand(entity.getWifiBand() == null ? null : WifiBand.fromValue(entity.getWifiBand()));
        rest.setSsid(entity.getSsid());
        rest.setEncryptionType(entity.getEncryptionType() == null
                ? null : EncryptionType.fromValue(entity.getEncryptionType()));
        rest.setPassword(entity.getPassword());
        return rest;
    }

    /** Converts a REST config into a DB row. */
    public WifiConfigurationEntity toEntity(WifiConfiguration rest) {
        if (rest == null) {
            return null;
        }
        WifiConfigurationEntity entity = new WifiConfigurationEntity();
        entity.setCpeId(rest.getCpeId());
        entity.setWifiBand(rest.getWifiBand() == null ? null : rest.getWifiBand().getValue());
        entity.setSsid(rest.getSsid());
        entity.setEncryptionType(rest.getEncryptionType() == null
                ? null : rest.getEncryptionType().getValue());
        entity.setPassword(rest.getPassword());
        return entity;
    }
}
