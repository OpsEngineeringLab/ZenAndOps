package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.UserManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

/**
 * Use case for removing realm-level roles from a user.
 */
@ApplicationScoped
public class RemoveUserRolesUseCase {

    private final UserManagementPort userManagementPort;

    public RemoveUserRolesUseCase(UserManagementPort userManagementPort) {
        this.userManagementPort = userManagementPort;
    }

    public void execute(String userId, List<Map<String, Object>> roles) {
        userManagementPort.removeRealmRoles(userId, roles);
    }
}
