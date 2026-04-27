package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CmdbEventPublisher;
import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.entity.Service;
import com.zenandops.cmdb.domain.exception.OrganizationNotFoundException;
import com.zenandops.cmdb.domain.exception.ServiceNotFoundException;
import com.zenandops.cmdb.domain.vo.CriticalityLevel;
import com.zenandops.cmdb.domain.vo.ServiceStatus;
import com.zenandops.cmdb.domain.vo.ServiceType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;

/**
 * Use case for creating a new Service.
 * Validates organization exists, parent service exists if provided,
 * and both business and technical owners are non-null/non-blank.
 * Publishes a service created event via CmdbEventPublisher.
 */
@ApplicationScoped
public class CreateServiceUseCase {

    private final ServiceRepository serviceRepository;
    private final OrganizationRepository organizationRepository;
    private final CmdbEventPublisher eventPublisher;

    @Inject
    public CreateServiceUseCase(ServiceRepository serviceRepository,
                                OrganizationRepository organizationRepository,
                                CmdbEventPublisher eventPublisher) {
        this.serviceRepository = serviceRepository;
        this.organizationRepository = organizationRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new Service.
     *
     * @param name            the service name
     * @param description     the service description
     * @param type            the service type
     * @param parentId        optional parent service ID
     * @param organizationId  the owning organization ID
     * @param businessOwner   the business owner (required, non-blank)
     * @param technicalOwner  the technical owner (required, non-blank)
     * @param criticality     the criticality level
     * @param status          the service status
     * @param userId          the authenticated user performing the action
     * @return the created Service
     */
    public Service execute(String name, String description, ServiceType type, String parentId,
                           String organizationId, String businessOwner, String technicalOwner,
                           CriticalityLevel criticality, ServiceStatus status, String userId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException(
                    "Organization not found with id: " + organizationId);
        }

        if (parentId != null && !serviceRepository.existsById(parentId)) {
            throw new ServiceNotFoundException(
                    "Parent service not found with id: " + parentId);
        }

        if (businessOwner == null || businessOwner.isBlank()) {
            throw new IllegalArgumentException("Business owner is required");
        }

        if (technicalOwner == null || technicalOwner.isBlank()) {
            throw new IllegalArgumentException("Technical owner is required");
        }

        Instant now = Instant.now();
        Service service = new Service();
        service.setName(name);
        service.setDescription(description);
        service.setType(type);
        service.setParentId(parentId);
        service.setOrganizationId(organizationId);
        service.setBusinessOwner(businessOwner);
        service.setTechnicalOwner(technicalOwner);
        service.setCriticality(criticality);
        service.setStatus(status);
        service.setCreatedAt(now);
        service.setUpdatedAt(now);

        serviceRepository.save(service);
        eventPublisher.publishServiceCreated(service, userId);
        return service;
    }
}
