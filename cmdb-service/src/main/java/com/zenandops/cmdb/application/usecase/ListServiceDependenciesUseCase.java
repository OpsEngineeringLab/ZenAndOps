package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.PaginatedResult;
import com.zenandops.cmdb.application.port.ServiceDependencyRepository;
import com.zenandops.cmdb.domain.entity.ServiceDependency;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all dependencies for a given service with pagination support.
 * Uses the paginated repository method that combines both upstream and downstream dependencies.
 */
@ApplicationScoped
public class ListServiceDependenciesUseCase {

    private final ServiceDependencyRepository serviceDependencyRepository;

    @Inject
    public ListServiceDependenciesUseCase(ServiceDependencyRepository serviceDependencyRepository) {
        this.serviceDependencyRepository = serviceDependencyRepository;
    }

    /**
     * List all dependencies for a service with pagination.
     *
     * @param serviceId the service identifier
     * @param page      zero-based page number
     * @param size      number of items per page
     * @return paginated result containing items and total count
     */
    public PaginatedResult<ServiceDependency> execute(String serviceId, int page, int size) {
        List<ServiceDependency> items = serviceDependencyRepository.findWithFilters(serviceId, page, size);
        long totalItems = serviceDependencyRepository.countWithFilters(serviceId);
        return new PaginatedResult<>(items, totalItems);
    }
}
