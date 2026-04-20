package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.exception.UserNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;

/**
 * Use case for updating the current user's profile (name and email only).
 */
@ApplicationScoped
public class UpdateProfileUseCase {

    private final UserRepository userRepository;

    @Inject
    public UpdateProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Update the profile of the currently authenticated user.
     * Only name and email are modifiable.
     *
     * @param login the user's login (from JWT sub claim)
     * @param name  the new display name
     * @param email the new email
     * @return the updated User entity
     * @throws UserNotFoundException if the user does not exist
     */
    public User execute(String login, String name, String email) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException("User not found with login: " + login));

        user.setName(name);
        user.setEmail(email);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        return user;
    }
}
