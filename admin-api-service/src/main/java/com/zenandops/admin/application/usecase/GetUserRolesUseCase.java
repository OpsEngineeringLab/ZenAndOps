package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.UserManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

/**
 * Use case for retrieving realm-level role mappings for a user.
 */
@ApplicationScoped
public class GetUserRolesUseCase {

    private final UserManagementPort userManagementPort;

    public GetUserRolesUseCase(UserManagementPort userManagementPort) {
        this.userManagementPort = userManagementPort;
    }

    public List<Map<String, Object>> execute(String userId) {
        return userManagementPort.getUserRealmRoles(userId);
    }
}
