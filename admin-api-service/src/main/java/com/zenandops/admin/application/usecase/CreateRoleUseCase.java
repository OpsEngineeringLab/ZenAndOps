package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.RoleManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Use case for creating a new realm role.
 */
@ApplicationScoped
public class CreateRoleUseCase {

    private final RoleManagementPort roleManagementPort;

    public CreateRoleUseCase(RoleManagementPort roleManagementPort) {
        this.roleManagementPort = roleManagementPort;
    }

    public void execute(Map<String, Object> roleRepresentation) {
        roleManagementPort.createRealmRole(roleRepresentation);
    }
}
