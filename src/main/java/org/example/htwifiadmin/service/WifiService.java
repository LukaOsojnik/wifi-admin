package org.example.htwifiadmin.service;

import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import lombok.extern.slf4j.Slf4j;
import org.example.htwifiadmin.exception.CpeNotFoundException;
import org.example.htwifiadmin.exception.PlatformCommunicationException;
import org.example.htwifiadmin.mapper.WifiConfigurationMapper;
import org.example.htwifiadmin.mapper.WifiEntityMapper;
import org.example.htwifiadmin.persistence.WifiConfigurationEntity;
import org.example.htwifiadmin.persistence.WifiConfigurationRepository;
import org.example.htwifiadmin.rest.model.WifiConfiguration;
import org.example.htwifiadmin.soap.GetCpeIdRequest;
import org.example.htwifiadmin.soap.GetCpeIdResponse;
import org.example.htwifiadmin.soap.UpdateCpeIdRequest;
import org.example.htwifiadmin.soap.UpdateCpeIdResponse;
import org.example.htwifiadmin.soap.WifiPlatformPortType;
import org.example.htwifiadmin.validation.WifiConfigurationValidator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Core business logic: talks to the SOAP platform, caches results in the DB,
 * and turns SOAP errors into our own exceptions (so no SOAP details leak out).
 */
@Slf4j
@Service
public class WifiService {

    private final WifiPlatformPortType port;
    private final WifiConfigurationMapper mapper;
    private final WifiConfigurationValidator validator;
    private final WifiConfigurationRepository repository;
    private final WifiEntityMapper entityMapper;

    public WifiService(WifiPlatformPortType port, WifiConfigurationMapper mapper,
                       WifiConfigurationValidator validator,
                       WifiConfigurationRepository repository, WifiEntityMapper entityMapper) {
        this.port = port;
        this.mapper = mapper;
        this.validator = validator;
        this.repository = repository;
        this.entityMapper = entityMapper;
    }

    /** Returns the config from the DB if we have it; otherwise asks the platform and saves the result. */
    public WifiConfiguration getWifiParameter(String cpeId) {
        Optional<WifiConfigurationEntity> cached = repository.findById(cpeId);
        if (cached.isPresent()) {
            log.info("Serving cpeId={} from database (cache hit)", cpeId);
            return entityMapper.toRest(cached.get());
        }

        log.info("Cache miss for cpeId={}, fetching from platform", cpeId);
        WifiConfiguration fromPlatform = fetchFromPlatform(cpeId);
        cache(fromPlatform);
        return fromPlatform;
    }

    /** Validates the config, updates the platform, then saves the confirmed result in the DB. */
    public WifiConfiguration updateWifiParameter(WifiConfiguration configuration) {
        log.info("Updating WiFi parameters for cpeId={}", configuration.getCpeId());
        validator.validate(configuration);

        WifiConfiguration confirmed = updateOnPlatform(configuration);
        cache(confirmed);
        return confirmed;
    }

    /** Always fetches fresh data from the platform and updates the DB. Used by the nightly sync. */
    public WifiConfiguration refreshFromPlatform(String cpeId) {
        WifiConfiguration fromPlatform = fetchFromPlatform(cpeId);
        cache(fromPlatform);
        return fromPlatform;
    }

    /** Calls the platform's getCpeID and converts the response to our REST model. */
    private WifiConfiguration fetchFromPlatform(String cpeId) {
        GetCpeIdRequest request = new GetCpeIdRequest();
        request.setCpeId(cpeId);
        try {
            GetCpeIdResponse response = port.getCpeID(request);
            return mapper.toRest(response.getConfiguration());
        } catch (WebServiceException e) {
            throw translate(e, cpeId);
        }
    }

    /** Calls the platform's updateCpeId and returns the config the platform confirmed. */
    private WifiConfiguration updateOnPlatform(WifiConfiguration configuration) {
        UpdateCpeIdRequest request = new UpdateCpeIdRequest();
        request.setConfiguration(mapper.toSoap(configuration));
        try {
            UpdateCpeIdResponse response = port.updateCpeId(request);
            return mapper.toRest(response.getConfiguration());
        } catch (WebServiceException e) {
            throw translate(e, configuration.getCpeId());
        }
    }

    /** Saves the config to the DB with the current time as the sync timestamp. */
    private void cache(WifiConfiguration configuration) {
        WifiConfigurationEntity entity = entityMapper.toEntity(configuration);
        entity.setLastUpdated(Instant.now());
        repository.save(entity);
    }

    /** Converts a SOAP error into our own exception: unknown CPE -> not found, anything else -> platform error. */
    private RuntimeException translate(WebServiceException e, String cpeId) {
        if (indicatesNotFound(e)) {
            return new CpeNotFoundException("CPE_NOT_FOUND", "CPE not found: " + cpeId);
        }
        return new PlatformCommunicationException(
                "PLATFORM_UNAVAILABLE", "Error communicating with the WiFi platform", e);
    }

    /**
     * Checks whether the error means "CPE not found". Looks at the SOAP fault fields
     * and at plain error messages, because the mock reports it in a non-standard way.
     */
    private boolean indicatesNotFound(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof SOAPFaultException sfe && sfe.getFault() != null) {
                String faultCode = sfe.getFault().getFaultCode();
                String faultString = sfe.getFault().getFaultString();
                if (containsNotFound(faultCode) || containsNotFound(faultString)) {
                    return true;
                }
            }
            if (containsNotFound(c.getMessage())) {
                return true;
            }
            if (c.getCause() == c) {
                break;
            }
        }
        return false;
    }

    private boolean containsNotFound(String s) {
        if (s == null) {
            return false;
        }
        String lower = s.toLowerCase();
        return lower.contains("notfound") || lower.contains("not found");
    }
}
