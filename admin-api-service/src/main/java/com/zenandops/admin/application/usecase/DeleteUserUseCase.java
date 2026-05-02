package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.UserManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Use case for deleting a user.
 */
@ApplicationScoped
public class DeleteUserUseCase {

    private final UserManagementPort userManagementPort;

    public DeleteUserUseCase(UserManagementPort userManagementPort) {
        this.userManagementPort = userManagementPort;
    }

    public void execute(String userId) {
        userManagementPort.deleteUser(userId);
    }
}
