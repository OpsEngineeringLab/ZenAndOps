package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.TagRepository;
import com.zenandops.auth.domain.entity.Tag;
import com.zenandops.auth.domain.exception.TagNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;

/**
 * Use case for updating the description of an existing Tag.
 */
@ApplicationScoped
public class UpdateTagUseCase {

    private final TagRepository tagRepository;

    @Inject
    public UpdateTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * Update the description of a Tag.
     *
     * @param id          the tag identifier
     * @param description the new description
     * @return the updated Tag
     * @throws TagNotFoundException if no Tag exists with the given id
     */
    public Tag execute(String id, String description) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new TagNotFoundException("Tag not found with id: " + id));

        tag.setDescription(description);
        tag.setUpdatedAt(Instant.now());

        tagRepository.save(tag);
        return tag;
    }
}
