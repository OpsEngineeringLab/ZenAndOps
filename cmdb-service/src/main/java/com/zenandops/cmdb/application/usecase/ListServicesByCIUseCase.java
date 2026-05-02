package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.PaginatedResult;
import com.zenandops.cmdb.application.port.ServiceCIRepository;
import com.zenandops.cmdb.domain.entity.ServiceCI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all services associated with a given CI with pagination support.
 */
@ApplicationScoped
public class ListServicesByCIUseCase {

    private final ServiceCIRepository serviceCIRepository;

    @Inject
    public ListServicesByCIUseCase(ServiceCIRepository serviceCIRepository) {
        this.serviceCIRepository = serviceCIRepository;
    }

    /**
     * List all service-CI associations for a CI with pagination.
     *
     * @param ciId the CI identifier
     * @param page zero-based page number
     * @param size number of items per page
     * @return paginated result containing items and total count
     */
    public PaginatedResult<ServiceCI> execute(String ciId, int page, int size) {
        List<ServiceCI> items = serviceCIRepository.findWithFilters(null, ciId, page, size);
        long totalItems = serviceCIRepository.countWithFilters(null, ciId);
        return new PaginatedResult<>(items, totalItems);
    }
}
