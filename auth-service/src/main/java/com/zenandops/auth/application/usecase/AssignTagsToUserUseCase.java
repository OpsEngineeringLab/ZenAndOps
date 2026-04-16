package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.TagRepository;
import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.Tag;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.exception.TagNotFoundException;
import com.zenandops.auth.domain.exception.UserNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Use case for assigning one or more Tags to a User.
 * Validates that all tag ids exist and the user exists. Ignores duplicates.
 */
@ApplicationScoped
public class AssignTagsToUserUseCase {

    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    @Inject
    public AssignTagsToUserUseCase(UserRepository userRepository, TagRepository tagRepository) {
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
    }

    /**
     * Assign tags to a user.
     *
     * @param userId the user identifier
     * @param tagIds the list of tag identifiers to assign
     * @return the updated User
     * @throws UserNotFoundException if the user does not exist
     * @throws TagNotFoundException  if any of the tag ids do not exist
     */
    public User execute(String userId, List<String> tagIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        List<Tag> tags = tagRepository.findAllByIds(tagIds);
        if (tags.size() != tagIds.size()) {
            throw new TagNotFoundException("One or more tag identifiers do not exist");
        }

        Set<String> merged = new LinkedHashSet<>(user.getTagIds());
        merged.addAll(tagIds);
        user.setTagIds(new ArrayList<>(merged));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        return user;
    }
}
