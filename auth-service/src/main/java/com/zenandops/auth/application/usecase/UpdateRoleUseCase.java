package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.RoleRepository;
import com.zenandops.auth.domain.entity.Role;
import com.zenandops.auth.domain.exception.RoleAlreadyExistsException;
import com.zenandops.auth.domain.exception.RoleNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;

/**
 * Use case for updating an existing Role.
 * Validates name uniqueness if the name is changed.
 */
@ApplicationScoped
public class UpdateRoleUseCase {

    private final RoleRepository roleRepository;

    @Inject
    public UpdateRoleUseCase(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Update a Role's fields.
     *
     * @param id          the role identifier
     * @param name        the new name
     * @param description the new description
     * @param permissions the new permissions list
     * @return the updated Role
     * @throws RoleNotFoundException      if no Role exists with the given id
     * @throws RoleAlreadyExistsException if the new name conflicts with another existing Role
     */
    public Role execute(String id, String name, String description, List<String> permissions) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with id: " + id));

        if (!role.getName().equals(name)) {
            roleRepository.findByName(name).ifPresent(existing -> {
                throw new RoleAlreadyExistsException(
                        "A role with name '" + name + "' already exists");
            });
        }

        role.setName(name);
        role.setDescription(description);
        role.setPermissions(permissions != null ? permissions : List.of());
        role.setUpdatedAt(Instant.now());

        roleRepository.save(role);
        return role;
    }
}
