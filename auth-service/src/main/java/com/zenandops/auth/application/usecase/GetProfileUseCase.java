package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.exception.UserNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for retrieving the current user's profile by login.
 */
@ApplicationScoped
public class GetProfileUseCase {

    private final UserRepository userRepository;

    @Inject
    public GetProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get the profile of the currently authenticated user.
     *
     * @param login the user's login (from JWT sub claim)
     * @return the User entity
     * @throws UserNotFoundException if the user does not exist
     */
    public User execute(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException("User not found with login: " + login));
    }
}
