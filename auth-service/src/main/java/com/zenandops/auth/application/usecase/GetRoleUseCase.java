package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.RoleRepository;
import com.zenandops.auth.domain.entity.Role;
import com.zenandops.auth.domain.exception.RoleNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for retrieving a single Role by its identifier.
 */
@ApplicationScoped
public class GetRoleUseCase {

    private final RoleRepository roleRepository;

    @Inject
    public GetRoleUseCase(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Retrieve a Role by id.
     *
     * @param id the role identifier
     * @return the Role
     * @throws RoleNotFoundException if no Role exists with the given id
     */
    public Role execute(String id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with id: " + id));
    }
}
