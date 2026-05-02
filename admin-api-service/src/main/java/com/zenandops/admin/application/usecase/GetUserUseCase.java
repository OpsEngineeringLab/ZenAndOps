package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.UserManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Use case for retrieving a single user by ID.
 */
@ApplicationScoped
public class GetUserUseCase {

    private final UserManagementPort userManagementPort;

    public GetUserUseCase(UserManagementPort userManagementPort) {
        this.userManagementPort = userManagementPort;
    }

    public Map<String, Object> execute(String userId) {
        return userManagementPort.getUser(userId);
    }
}
