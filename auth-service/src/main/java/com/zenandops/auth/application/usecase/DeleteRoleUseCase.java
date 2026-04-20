package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.RoleRepository;
import com.zenandops.auth.domain.exception.RoleInUseException;
import com.zenandops.auth.domain.exception.RoleNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for deleting a Role.
 * Rejects deletion if the Role is currently assigned to any User.
 */
@ApplicationScoped
public class DeleteRoleUseCase {

    private final RoleRepository roleRepository;

    @Inject
    public DeleteRoleUseCase(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Delete a Role by id.
     *
     * @param id the role identifier
     * @throws RoleNotFoundException if no Role exists with the given id
     * @throws RoleInUseException    if the Role is assigned to one or more Users
     */
    public void execute(String id) {
        var role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with id: " + id));

        if (roleRepository.existsAssignedToAnyUser(role.getName())) {
            throw new RoleInUseException(
                    "Role '" + role.getName() + "' is assigned to one or more users and cannot be deleted");
        }

        roleRepository.delete(id);
    }
}
