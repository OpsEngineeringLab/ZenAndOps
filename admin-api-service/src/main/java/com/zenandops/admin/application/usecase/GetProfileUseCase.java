package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.ProfileManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Use case for retrieving the current user's profile.
 */
@ApplicationScoped
public class GetProfileUseCase {

    private final ProfileManagementPort profileManagementPort;

    public GetProfileUseCase(ProfileManagementPort profileManagementPort) {
        this.profileManagementPort = profileManagementPort;
    }

    public Map<String, Object> execute(String userId) {
        return profileManagementPort.getUser(userId);
    }
}
