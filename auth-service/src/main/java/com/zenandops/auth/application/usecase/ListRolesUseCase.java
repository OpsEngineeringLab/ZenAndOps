package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.RoleRepository;
import com.zenandops.auth.domain.entity.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for retrieving a paginated list of all Roles.
 */
@ApplicationScoped
public class ListRolesUseCase {

    private final RoleRepository roleRepository;

    @Inject
    public ListRolesUseCase(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Retrieve a paginated list of Roles.
     *
     * @param page the page number (zero-based)
     * @param size the number of items per page
     * @return a paginated result containing the Roles
     */
    public PaginatedResult<Role> execute(int page, int size) {
        List<Role> items = roleRepository.findAll(page, size);
        long totalItems = roleRepository.count();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        return new PaginatedResult<>(items, page, size, totalItems, totalPages);
    }
}
