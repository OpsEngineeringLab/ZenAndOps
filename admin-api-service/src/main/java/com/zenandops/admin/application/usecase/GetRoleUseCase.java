package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.RoleManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Use case for retrieving a realm role by name or ID.
 */
@ApplicationScoped
public class GetRoleUseCase {

    private final RoleManagementPort roleManagementPort;

    public GetRoleUseCase(RoleManagementPort roleManagementPort) {
        this.roleManagementPort = roleManagementPort;
    }

    public Map<String, Object> executeByName(String roleName) {
        return roleManagementPort.getRealmRoleByName(roleName);
    }

    public Map<String, Object> executeById(String roleId) {
        return roleManagementPort.getRealmRoleById(roleId);
    }
}
