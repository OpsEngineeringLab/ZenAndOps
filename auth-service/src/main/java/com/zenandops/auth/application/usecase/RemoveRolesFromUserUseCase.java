package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.exception.UserNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Use case for removing one or more Roles from a User.
 */
@ApplicationScoped
public class RemoveRolesFromUserUseCase {

    private final UserRepository userRepository;

    @Inject
    public RemoveRolesFromUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Remove roles from a user.
     *
     * @param userId    the user identifier
     * @param roleNames the list of role names to remove
     * @return the updated User
     * @throws UserNotFoundException if the user does not exist
     */
    public User execute(String userId, List<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        List<String> remaining = new ArrayList<>(user.getRoles());
        remaining.removeAll(roleNames);
        user.setRoles(remaining);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        return user;
    }
}
