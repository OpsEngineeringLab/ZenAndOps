package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.UserManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

/**
 * Use case for assigning realm-level roles to a user.
 */
@ApplicationScoped
public class AssignUserRolesUseCase {

    private final UserManagementPort userManagementPort;

    public AssignUserRolesUseCase(UserManagementPort userManagementPort) {
        this.userManagementPort = userManagementPort;
    }

    public void execute(String userId, List<Map<String, Object>> roles) {
        userManagementPort.assignRealmRoles(userId, roles);
    }
}
