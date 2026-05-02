package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.PaginatedResult;
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
 * Supports pagination via page and size parameters.
 */
@ApplicationScoped
public class ListServicesUseCase {

    private final ServiceRepository serviceRepository;

    @Inject
    public ListServicesUseCase(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    /**
     * List services with optional filters and pagination.
     *
     * @param organizationId optional organization filter
     * @param type           optional service type filter
     * @param criticality    optional criticality filter
     * @param status         optional status filter
     * @param page           zero-based page number
     * @param size           number of items per page
     * @return paginated result containing items and total count
     */
    public PaginatedResult<Service> execute(String organizationId, ServiceType type,
                                            CriticalityLevel criticality, ServiceStatus status,
                                            int page, int size) {
        List<Service> items = serviceRepository.findWithFilters(organizationId, type, criticality, status, page, size);
        long totalItems = serviceRepository.countWithFilters(organizationId, type, criticality, status);
        return new PaginatedResult<>(items, totalItems);
    }
}
