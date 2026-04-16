package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.TagRepository;
import com.zenandops.auth.domain.exception.TagInUseException;
import com.zenandops.auth.domain.exception.TagNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for deleting a Tag.
 * Rejects deletion if the Tag is currently assigned to any User.
 */
@ApplicationScoped
public class DeleteTagUseCase {

    private final TagRepository tagRepository;

    @Inject
    public DeleteTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * Delete a Tag by id.
     *
     * @param id the tag identifier
     * @throws TagNotFoundException if no Tag exists with the given id
     * @throws TagInUseException    if the Tag is assigned to one or more Users
     */
    public void execute(String id) {
        tagRepository.findById(id)
                .orElseThrow(() -> new TagNotFoundException("Tag not found with id: " + id));

        if (tagRepository.existsAssignedToAnyUser(id)) {
            throw new TagInUseException("Tag is assigned to one or more users and cannot be deleted");
        }

        tagRepository.delete(id);
    }
}
