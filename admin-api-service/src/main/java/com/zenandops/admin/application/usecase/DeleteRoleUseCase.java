package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.RoleManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Use case for deleting a realm role.
 */
@ApplicationScoped
public class DeleteRoleUseCase {

    private final RoleManagementPort roleManagementPort;

    public DeleteRoleUseCase(RoleManagementPort roleManagementPort) {
        this.roleManagementPort = roleManagementPort;
    }

    public void execute(String roleId) {
        roleManagementPort.deleteRealmRole(roleId);
    }
}
