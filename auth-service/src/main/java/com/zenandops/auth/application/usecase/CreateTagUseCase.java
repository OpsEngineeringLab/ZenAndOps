package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.TagRepository;
import com.zenandops.auth.domain.entity.Tag;
import com.zenandops.auth.domain.exception.TagAlreadyExistsException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case for creating a new Tag.
 * Enforces key:value uniqueness via TagRepository.
 */
@ApplicationScoped
public class CreateTagUseCase {

    private final TagRepository tagRepository;

    @Inject
    public CreateTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * Create a new Tag with the given key, value, and description.
     *
     * @param key         the tag key
     * @param value       the tag value
     * @param description optional description
     * @return the created Tag
     * @throws TagAlreadyExistsException if a Tag with the same key:value already exists
     */
    public Tag execute(String key, String value, String description) {
        tagRepository.findByKeyAndValue(key, value).ifPresent(existing -> {
            throw new TagAlreadyExistsException(
                    "A tag with key '" + key + "' and value '" + value + "' already exists");
        });

        Tag tag = new Tag();
        tag.setId(UUID.randomUUID().toString());
        tag.setKey(key);
        tag.setValue(value);
        tag.setDescription(description);
        tag.setCreatedAt(Instant.now());
        tag.setUpdatedAt(Instant.now());

        tagRepository.save(tag);
        return tag;
    }
}
