package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.PaginatedResult;
import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.domain.entity.Organization;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all Organizations with pagination support.
 */
@ApplicationScoped
public class ListOrganizationsUseCase {

    private final OrganizationRepository organizationRepository;

    @Inject
    public ListOrganizationsUseCase(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /**
     * Retrieve Organizations with pagination.
     *
     * @param page zero-based page number
     * @param size number of items per page
     * @return paginated result containing items and total count
     */
    public PaginatedResult<Organization> execute(int page, int size) {
        List<Organization> items = organizationRepository.findAll(page, size);
        long totalItems = organizationRepository.countAll();
        return new PaginatedResult<>(items, totalItems);
    }
}
