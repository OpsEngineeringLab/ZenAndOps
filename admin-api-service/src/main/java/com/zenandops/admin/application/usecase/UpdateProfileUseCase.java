package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.ProfileManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Use case for updating the current user's profile.
 */
@ApplicationScoped
public class UpdateProfileUseCase {

    private final ProfileManagementPort profileManagementPort;

    public UpdateProfileUseCase(ProfileManagementPort profileManagementPort) {
        this.profileManagementPort = profileManagementPort;
    }

    public void execute(String userId, Map<String, Object> userRepresentation) {
        profileManagementPort.updateUser(userId, userRepresentation);
    }
}
