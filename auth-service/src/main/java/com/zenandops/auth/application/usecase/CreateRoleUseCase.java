package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.RoleRepository;
import com.zenandops.auth.domain.entity.Role;
import com.zenandops.auth.domain.exception.RoleAlreadyExistsException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;

/**
 * Use case for creating a new Role.
 * Enforces name uniqueness via RoleRepository.
 */
@ApplicationScoped
public class CreateRoleUseCase {

    private final RoleRepository roleRepository;

    @Inject
    public CreateRoleUseCase(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Create a new Role with the given name, description, and permissions.
     *
     * @param name        the role name (must be unique)
     * @param description optional description
     * @param permissions list of permission strings
     * @return the created Role
     * @throws RoleAlreadyExistsException if a Role with the same name already exists
     */
    public Role execute(String name, String description, List<String> permissions) {
        roleRepository.findByName(name).ifPresent(existing -> {
            throw new RoleAlreadyExistsException(
                    "A role with name '" + name + "' already exists");
        });

        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setPermissions(permissions != null ? permissions : List.of());
        role.setCreatedAt(Instant.now());
        role.setUpdatedAt(Instant.now());

        roleRepository.save(role);
        return role;
    }
}
