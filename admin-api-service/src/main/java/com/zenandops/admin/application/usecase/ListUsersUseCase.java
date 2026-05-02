package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.UserManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

/**
 * Use case for listing users with optional pagination.
 */
@ApplicationScoped
public class ListUsersUseCase {

    private final UserManagementPort userManagementPort;

    public ListUsersUseCase(UserManagementPort userManagementPort) {
        this.userManagementPort = userManagementPort;
    }

    public List<Map<String, Object>> execute(Integer first, Integer max) {
        return userManagementPort.listUsers(first, max);
    }
}
