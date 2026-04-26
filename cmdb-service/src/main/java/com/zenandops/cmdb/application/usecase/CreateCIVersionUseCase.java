package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.application.port.CIVersionRepository;
import com.zenandops.cmdb.application.port.CmdbEventPublisher;
import com.zenandops.cmdb.application.port.DataSourceRepository;
import com.zenandops.cmdb.domain.entity.CIVersion;
import com.zenandops.cmdb.domain.exception.CINotFoundException;
import com.zenandops.cmdb.domain.exception.DataSourceNotFoundException;
import com.zenandops.cmdb.domain.vo.DataOrigin;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for creating a new CIVersion.
 * Validates CI and data source exist, auto-assigns the next sequential version number,
 * closes the previous active version, and publishes a version created event.
 */
@ApplicationScoped
public class CreateCIVersionUseCase {

    private final CIRepository ciRepository;
    private final CIVersionRepository ciVersionRepository;
    private final DataSourceRepository dataSourceRepository;
    private final CmdbEventPublisher eventPublisher;

    @Inject
    public CreateCIVersionUseCase(CIRepository ciRepository,
                                  CIVersionRepository ciVersionRepository,
                                  DataSourceRepository dataSourceRepository,
                                  CmdbEventPublisher eventPublisher) {
        this.ciRepository = ciRepository;
        this.ciVersionRepository = ciVersionRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new CIVersion.
     *
     * @param ciId            the CI identifier
     * @param attributes      the version attributes (JSON map)
     * @param dataOrigin      the data origin (API, AGENT, FILE)
     * @param dataSourceId    the data source identifier
     * @param changeReference optional change reference
     * @param userId          the authenticated user performing the action
     * @return the created CIVersion
     */
    public CIVersion execute(String ciId, Map<String, Object> attributes,
                             DataOrigin dataOrigin, String dataSourceId,
                             String changeReference, String userId) {
        if (!ciRepository.existsById(ciId)) {
            throw new CINotFoundException("CI not found with id: " + ciId);
        }

        if (!dataSourceRepository.existsById(dataSourceId)) {
            throw new DataSourceNotFoundException(
                    "Data source not found with id: " + dataSourceId);
        }

        Instant now = Instant.now();

        // Close previous active version
        ciVersionRepository.findActiveByCiId(ciId).ifPresent(previous -> {
            previous.setEndDate(now);
            ciVersionRepository.save(previous);
        });

        // Auto-assign next sequential version number
        int nextVersion = ciVersionRepository.getMaxVersionNumber(ciId) + 1;

        CIVersion version = new CIVersion();
        version.setId(UUID.randomUUID().toString());
        version.setCiId(ciId);
        version.setVersionNumber(nextVersion);
        version.setAttributes(attributes != null ? attributes : Map.of());
        version.setStartDate(now);
        version.setEndDate(null);
        version.setDataOrigin(dataOrigin);
        version.setDataSourceId(dataSourceId);
        version.setChangeReference(changeReference);
        version.setCreatedAt(now);

        ciVersionRepository.save(version);
        eventPublisher.publishVersionCreated(version.getId(), "CI_VERSION", userId);
        return version;
    }
}
