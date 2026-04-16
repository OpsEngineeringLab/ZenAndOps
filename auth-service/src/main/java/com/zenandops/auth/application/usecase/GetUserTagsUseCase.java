package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.TagRepository;
import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.Tag;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.exception.UserNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for retrieving all Tags assigned to a User.
 */
@ApplicationScoped
public class GetUserTagsUseCase {

    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    @Inject
    public GetUserTagsUseCase(UserRepository userRepository, TagRepository tagRepository) {
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
    }

    /**
     * Retrieve all Tags assigned to a User.
     *
     * @param userId the user identifier
     * @return the list of Tags assigned to the user
     * @throws UserNotFoundException if the user does not exist
     */
    public List<Tag> execute(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (user.getTagIds() == null || user.getTagIds().isEmpty()) {
            return List.of();
        }

        return tagRepository.findAllByIds(user.getTagIds());
    }
}
