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
 * Use case for removing one or more Tags from a User.
 */
@ApplicationScoped
public class RemoveTagsFromUserUseCase {

    private final UserRepository userRepository;

    @Inject
    public RemoveTagsFromUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Remove tags from a user.
     *
     * @param userId the user identifier
     * @param tagIds the list of tag identifiers to remove
     * @return the updated User
     * @throws UserNotFoundException if the user does not exist
     */
    public User execute(String userId, List<String> tagIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        List<String> remaining = new ArrayList<>(user.getTagIds());
        remaining.removeAll(tagIds);
        user.setTagIds(remaining);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        return user;
    }
}
