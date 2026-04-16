package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.TagRepository;
import com.zenandops.auth.domain.entity.Tag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for retrieving a paginated list of all Tags.
 */
@ApplicationScoped
public class ListTagsUseCase {

    private final TagRepository tagRepository;

    @Inject
    public ListTagsUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * Retrieve a paginated list of Tags.
     *
     * @param page the page number (zero-based)
     * @param size the number of items per page
     * @return a paginated result containing the Tags
     */
    public PaginatedResult<Tag> execute(int page, int size) {
        List<Tag> items = tagRepository.findAll(page, size);
        long totalItems = tagRepository.count();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        return new PaginatedResult<>(items, page, size, totalItems, totalPages);
    }
}
