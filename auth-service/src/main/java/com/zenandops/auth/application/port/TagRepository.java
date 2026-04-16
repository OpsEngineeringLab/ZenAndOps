package com.zenandops.auth.application.port;

import com.zenandops.auth.domain.entity.Tag;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Tag persistence operations.
 */
public interface TagRepository {

    void save(Tag tag);

    Optional<Tag> findById(String id);

    List<Tag> findAll(int page, int size);

    long count();

    Optional<Tag> findByKeyAndValue(String key, String value);

    void delete(String id);

    List<Tag> findAllByIds(List<String> ids);

    boolean existsAssignedToAnyUser(String tagId);
}
