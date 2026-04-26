package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.application.port.CmdbEventPublisher;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.exception.CINotFoundException;
import com.zenandops.cmdb.domain.vo.CIStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;

/**
 * Use case for updating an existing CI.
 * Validates the CI exists, updates mutable fields, and publishes a CI updated event.
 */
@ApplicationScoped
public class UpdateCIUseCase {

    private final CIRepository ciRepository;
    private final CmdbEventPublisher eventPublisher;

    @Inject
    public UpdateCIUseCase(CIRepository ciRepository,
                           CmdbEventPublisher eventPublisher) {
        this.ciRepository = ciRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Update a CI's mutable fields.
     *
     * @param id                      the CI identifier
     * @param name                    the new name
     * @param status                  the new status
     * @param controlledExceptionFlag the new controlled exception flag
     * @param userId                  the authenticated user performing the action
     * @return the updated CI
     * @throws CINotFoundException if no CI exists with the given id
     */
    public CI execute(String id, String name, CIStatus status,
                      boolean controlledExceptionFlag, String userId) {
        CI ci = ciRepository.findById(id)
                .orElseThrow(() -> new CINotFoundException(
                        "CI not found with id: " + id));

        ci.setName(name);
        ci.setStatus(status);
        ci.setControlledExceptionFlag(controlledExceptionFlag);
        ci.setUpdatedAt(Instant.now());

        ciRepository.save(ci);
        eventPublisher.publishCIUpdated(ci, userId);
        return ci;
    }
}
