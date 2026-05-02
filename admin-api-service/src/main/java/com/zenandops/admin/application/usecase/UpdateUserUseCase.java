package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.UserManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Use case for updating an existing user.
 */
@ApplicationScoped
public class UpdateUserUseCase {

    private final UserManagementPort userManagementPort;

    public UpdateUserUseCase(UserManagementPort userManagementPort) {
        this.userManagementPort = userManagementPort;
    }

    public void execute(String userId, Map<String, Object> userRepresentation) {
        userManagementPort.updateUser(userId, userRepresentation);
    }
}
