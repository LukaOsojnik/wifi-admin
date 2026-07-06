package org.example.htwifiadmin.mapper;

import org.example.htwifiadmin.persistence.WifiConfigurationEntity;
import org.example.htwifiadmin.rest.model.EncryptionType;
import org.example.htwifiadmin.rest.model.WifiBand;
import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.springframework.stereotype.Component;

/**
 * Translates between the DB entity ({@link WifiConfigurationEntity}) and the REST model
 * ({@link WifiConfiguration}). Separate from {@link WifiConfigurationMapper} (SOAP⇄REST) so
 * each mapper owns exactly one boundary.
 *
 * <p>Enums are stored as their string wire value in the DB, so conversion is by
 * {@code getValue()} / {@code fromValue(...)}, null-guarded for the optional fields.
 * {@code lastUpdated} is set by the service at save time, not here.
 */
@Component
public class WifiEntityMapper {

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
