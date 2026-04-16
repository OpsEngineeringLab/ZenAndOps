package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.TagRepository;
import com.zenandops.auth.domain.entity.Tag;
import com.zenandops.auth.domain.exception.TagNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for retrieving a single Tag by its identifier.
 */
@ApplicationScoped
public class GetTagUseCase {

    private final TagRepository tagRepository;

    @Inject
    public GetTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * Retrieve a Tag by id.
     *
     * @param id the tag identifier
     * @return the Tag
     * @throws TagNotFoundException if no Tag exists with the given id
     */
    public Tag execute(String id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new TagNotFoundException("Tag not found with id: " + id));
    }
}
