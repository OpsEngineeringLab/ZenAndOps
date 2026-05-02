package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.PaginatedResult;
import com.zenandops.cmdb.application.port.ServiceCIRepository;
import com.zenandops.cmdb.domain.entity.ServiceCI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all CIs associated with a given service with pagination support.
 */
@ApplicationScoped
public class ListCIsByServiceUseCase {

    private final ServiceCIRepository serviceCIRepository;

    @Inject
    public ListCIsByServiceUseCase(ServiceCIRepository serviceCIRepository) {
        this.serviceCIRepository = serviceCIRepository;
    }

    /**
     * List all service-CI associations for a service with pagination.
     *
     * @param serviceId the service identifier
     * @param page      zero-based page number
     * @param size      number of items per page
     * @return paginated result containing items and total count
     */
    public PaginatedResult<ServiceCI> execute(String serviceId, int page, int size) {
        List<ServiceCI> items = serviceCIRepository.findWithFilters(serviceId, null, page, size);
        long totalItems = serviceCIRepository.countWithFilters(serviceId, null);
        return new PaginatedResult<>(items, totalItems);
    }
}
