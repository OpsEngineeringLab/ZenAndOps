package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.RoleManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

/**
 * Use case for listing all realm roles.
 */
@ApplicationScoped
public class ListRolesUseCase {

    private final RoleManagementPort roleManagementPort;

    public ListRolesUseCase(RoleManagementPort roleManagementPort) {
        this.roleManagementPort = roleManagementPort;
    }

    public List<Map<String, Object>> execute() {
        return roleManagementPort.listRealmRoles();
    }
}
