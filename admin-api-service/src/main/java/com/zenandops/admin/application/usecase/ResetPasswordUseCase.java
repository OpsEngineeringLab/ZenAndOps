package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.ProfileManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Use case for resetting a user's password.
 */
@ApplicationScoped
public class ResetPasswordUseCase {

    private final ProfileManagementPort profileManagementPort;

    public ResetPasswordUseCase(ProfileManagementPort profileManagementPort) {
        this.profileManagementPort = profileManagementPort;
    }

    public void execute(String userId, String newPassword, boolean temporary) {
        profileManagementPort.resetPassword(userId, newPassword, temporary);
    }
}
