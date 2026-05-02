package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.RoleManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Use case for updating a realm role.
 */
@ApplicationScoped
public class UpdateRoleUseCase {

    private final RoleManagementPort roleManagementPort;

    public UpdateRoleUseCase(RoleManagementPort roleManagementPort) {
        this.roleManagementPort = roleManagementPort;
    }

    public void execute(String roleId, Map<String, Object> roleRepresentation) {
        roleManagementPort.updateRealmRole(roleId, roleRepresentation);
    }
}
