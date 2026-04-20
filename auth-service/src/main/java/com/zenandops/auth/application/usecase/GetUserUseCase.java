package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.exception.UserNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for retrieving a single User by its identifier.
 */
@ApplicationScoped
public class GetUserUseCase {

    private final UserRepository userRepository;

    @Inject
    public GetUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieve a User by id.
     *
     * @param id the user identifier
     * @return the User
     * @throws UserNotFoundException if no User exists with the given id
     */
    public User execute(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }
}
