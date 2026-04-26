package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.entity.Service;
import com.zenandops.cmdb.domain.vo.CriticalityLevel;
import com.zenandops.cmdb.domain.vo.ServiceStatus;
import com.zenandops.cmdb.domain.vo.ServiceType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing services with optional filtering by organizationId, type, criticality, and status.
 */
@ApplicationScoped
public class ListServicesUseCase {

    private final ServiceRepository serviceRepository;

    @Inject
    public ListServicesUseCase(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    /**
     * List services with optional filters. Pass null for any filter to skip it.
     *
     * @param organizationId optional organization filter
     * @param type           optional service type filter
     * @param criticality    optional criticality filter
     * @param status         optional status filter
     * @return filtered list of services
     */
    public List<Service> execute(String organizationId, ServiceType type,
                                 CriticalityLevel criticality, ServiceStatus status) {
        return serviceRepository.findWithFilters(organizationId, type, criticality, status);
    }
}
