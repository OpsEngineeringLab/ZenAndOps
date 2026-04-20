package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.PasswordEncoder;
import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.exception.InvalidPasswordException;
import com.zenandops.auth.domain.exception.UserNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;

/**
 * Use case for changing the current user's password.
 * Verifies the current password before applying the new one.
 */
@ApplicationScoped
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public ChangePasswordUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Change the password of the currently authenticated user.
     *
     * @param login           the user's login (from JWT sub claim)
     * @param currentPassword the user's current plain-text password for verification
     * @param newPassword     the new plain-text password to set
     * @throws UserNotFoundException    if the user does not exist
     * @throws InvalidPasswordException if the current password does not match
     */
    public void execute(String login, String currentPassword, String newPassword) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException("User not found with login: " + login));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
    }
}
