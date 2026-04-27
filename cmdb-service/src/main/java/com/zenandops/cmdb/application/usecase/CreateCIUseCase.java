package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.application.port.CmdbEventPublisher;
import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.exception.AssetNotFoundException;
import com.zenandops.cmdb.domain.exception.OrganizationNotFoundException;
import com.zenandops.cmdb.domain.vo.CIStatus;
import com.zenandops.cmdb.domain.vo.CIType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;

/**
 * Use case for creating a new CI.
 * Validates organization exists, validates asset exists if provided,
 * sets controlledExceptionFlag default to false, and publishes a CI created event.
 */
@ApplicationScoped
public class CreateCIUseCase {

    private final CIRepository ciRepository;
    private final OrganizationRepository organizationRepository;
    private final AssetRepository assetRepository;
    private final CmdbEventPublisher eventPublisher;

    @Inject
    public CreateCIUseCase(CIRepository ciRepository,
                           OrganizationRepository organizationRepository,
                           AssetRepository assetRepository,
                           CmdbEventPublisher eventPublisher) {
        this.ciRepository = ciRepository;
        this.organizationRepository = organizationRepository;
        this.assetRepository = assetRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new CI.
     *
     * @param name                   the CI name
     * @param type                   the CI type (VM, DATABASE, API, STORAGE, NETWORK)
     * @param organizationId         the owning organization ID
     * @param assetId                optional associated asset ID
     * @param status                 the CI status
     * @param controlledExceptionFlag whether the CI has a controlled exception
     * @param userId                 the authenticated user performing the action
     * @return the created CI
     */
    public CI execute(String name, CIType type, String organizationId,
                      String assetId, CIStatus status,
                      boolean controlledExceptionFlag, String userId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException(
                    "Organization not found with id: " + organizationId);
        }

        if (assetId != null && !assetId.isBlank() && !assetRepository.existsById(assetId)) {
            throw new AssetNotFoundException("Asset not found with id: " + assetId);
        }

        Instant now = Instant.now();
        CI ci = new CI();
        ci.setName(name);
        ci.setType(type);
        ci.setOrganizationId(organizationId);
        ci.setAssetId(assetId);
        ci.setStatus(status);
        ci.setControlledExceptionFlag(controlledExceptionFlag);
        ci.setCreatedAt(now);
        ci.setUpdatedAt(now);

        ciRepository.save(ci);
        eventPublisher.publishCICreated(ci, userId);
        return ci;
    }
}
