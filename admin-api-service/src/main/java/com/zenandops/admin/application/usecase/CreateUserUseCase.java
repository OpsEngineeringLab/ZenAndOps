package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.UserManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Use case for creating a new user. Returns the created user's ID.
 */
@ApplicationScoped
public class CreateUserUseCase {

    private final UserManagementPort userManagementPort;

    public CreateUserUseCase(UserManagementPort userManagementPort) {
        this.userManagementPort = userManagementPort;
    }

    public String execute(Map<String, Object> userRepresentation) {
        return userManagementPort.createUser(userRepresentation);
    }
}
