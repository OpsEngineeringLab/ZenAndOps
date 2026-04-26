package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CmdbEventPublisher;
import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.entity.Service;
import com.zenandops.cmdb.domain.exception.ServiceNotFoundException;
import com.zenandops.cmdb.domain.vo.CriticalityLevel;
import com.zenandops.cmdb.domain.vo.ServiceStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;

/**
 * Use case for updating an existing Service.
 * Updates mutable fields and publishes a service updated event.
 */
@ApplicationScoped
public class UpdateServiceUseCase {

    private final ServiceRepository serviceRepository;
    private final CmdbEventPublisher eventPublisher;

    @Inject
    public UpdateServiceUseCase(ServiceRepository serviceRepository,
                                CmdbEventPublisher eventPublisher) {
        this.serviceRepository = serviceRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Update a Service's mutable fields.
     *
     * @param id             the service identifier
     * @param name           the new name
     * @param description    the new description
     * @param businessOwner  the new business owner
     * @param technicalOwner the new technical owner
     * @param criticality    the new criticality level
     * @param status         the new status
     * @param userId         the authenticated user performing the action
     * @return the updated Service
     * @throws ServiceNotFoundException if no Service exists with the given id
     */
    public Service execute(String id, String name, String description,
                           String businessOwner, String technicalOwner,
                           CriticalityLevel criticality, ServiceStatus status,
                           String userId) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFoundException(
                        "Service not found with id: " + id));

        service.setName(name);
        service.setDescription(description);
        service.setBusinessOwner(businessOwner);
        service.setTechnicalOwner(technicalOwner);
        service.setCriticality(criticality);
        service.setStatus(status);
        service.setUpdatedAt(Instant.now());

        serviceRepository.save(service);
        eventPublisher.publishServiceUpdated(service, userId);
        return service;
    }
}
