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
 * Orchestrates the flows and owns two boundaries:
 * <ul>
 *   <li><b>SOAP</b> — builds requests, calls the platform, and translates SOAP transport/fault
 *       exceptions into domain exceptions ({@link CpeNotFoundException} 404,
 *       {@link PlatformCommunicationException} 502).</li>
 *   <li><b>DB cache</b> — GET is <i>read-through</i> (serve from DB; on miss fetch from the
 *       platform and cache); PUT is <i>write-through</i> (update the platform, then persist the
 *       confirmed config so later reads reflect the change).</li>
 * </ul>
 * It knows nothing about HTTP status codes; the exception advice maps domain exceptions to responses.
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

    /** GET flow (read-through): serve from DB; on miss fetch from the platform and cache. */
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

    /** PUT flow (write-through): update the platform, then persist the confirmed config. */
    public WifiConfiguration updateWifiParameter(WifiConfiguration configuration) {
        log.info("Updating WiFi parameters for cpeId={}", configuration.getCpeId());
        validator.validate(configuration);

        WifiConfiguration confirmed = updateOnPlatform(configuration);
        cache(confirmed);
        return confirmed;
    }

    /**
     * Force-refresh a CPE from the platform into the DB cache (used by the scheduler).
     * Always calls the platform (bypasses the read-through DB hit) and upserts the result.
     */
    public WifiConfiguration refreshFromPlatform(String cpeId) {
        WifiConfiguration fromPlatform = fetchFromPlatform(cpeId);
        cache(fromPlatform);
        return fromPlatform;
    }

    /** SOAP getCpeID, with fault translation. */
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

    /** SOAP updateCpeId, with fault translation. Returns the platform's confirmed config. */
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

    /** Upsert the configuration into the DB cache, stamping the sync time. */
    private void cache(WifiConfiguration configuration) {
        WifiConfigurationEntity entity = entityMapper.toEntity(configuration);
        entity.setLastUpdated(Instant.now());
        repository.save(entity);
    }

    /**
     * Translates a SOAP-layer failure into a domain exception: an unknown-CPE fault -> 404,
     * anything else (timeout, connection error, other fault) -> 502.
     */
    private RuntimeException translate(WebServiceException e, String cpeId) {
        if (indicatesNotFound(e)) {
            return new CpeNotFoundException("CPE_NOT_FOUND", "CPE not found: " + cpeId);
        }
        return new PlatformCommunicationException(
                "PLATFORM_UNAVAILABLE", "Error communicating with the WiFi platform", e);
    }

    /**
     * Detects the platform's "CPE not found" signal. Handles a spec-compliant
     * {@link SOAPFaultException} (real platform: faultcode/faultstring) AND the mock's
     * malformed fault, which surfaces as a wrapped "Invalid QName in mapping: tns:NotFound".
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
