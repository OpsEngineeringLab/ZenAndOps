package com.zenandops.auth.infrastructure.adapter.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.zenandops.auth.application.port.TagRepository;
import com.zenandops.auth.domain.entity.Tag;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB Panache adapter implementing the TagRepository port.
 * Creates a unique compound index on { key: 1, value: 1 } at startup.
 */
@ApplicationScoped
public class MongoTagRepository implements TagRepository {

    @Inject
    MongoClient mongoClient;

    void onStartup(@Observes StartupEvent event) {
        mongoClient.getDatabase("zenandops-auth")
                .getCollection("tags")
                .createIndex(
                        Indexes.compoundIndex(Indexes.ascending("key"), Indexes.ascending("value")),
                        new IndexOptions().unique(true)
                );
    }

    @Override
    public void save(Tag tag) {
        TagPanacheEntity entity = toEntity(tag);
        if (tag.getId() != null) {
            entity.id = new ObjectId(tag.getId());
            entity.update();
        } else {
            entity.persist();
            tag.setId(entity.id.toString());
        }
    }

    @Override
    public Optional<Tag> findById(String id) {
        return TagPanacheEntity.<TagPanacheEntity>findByIdOptional(new ObjectId(id))
                .map(this::toDomain);
    }

    @Override
    public List<Tag> findAll(int page, int size) {
        return TagPanacheEntity.<TagPanacheEntity>findAll()
                .page(page, size)
                .list()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return TagPanacheEntity.count();
    }

    @Override
    public Optional<Tag> findByKeyAndValue(String key, String value) {
        return TagPanacheEntity.<TagPanacheEntity>find("key = ?1 and value = ?2", key, value)
                .firstResultOptional()
                .map(this::toDomain);
    }

    @Override
    public void delete(String id) {
        TagPanacheEntity.deleteById(new ObjectId(id));
    }

    @Override
    public List<Tag> findAllByIds(List<String> ids) {
        List<ObjectId> objectIds = ids.stream()
                .map(ObjectId::new)
                .collect(Collectors.toList());
        return TagPanacheEntity.<TagPanacheEntity>find("_id in ?1", objectIds)
                .list()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsAssignedToAnyUser(String tagId) {
        return UserPanacheEntity.count("tagIds", tagId) > 0;
    }

    private Tag toDomain(TagPanacheEntity entity) {
        Tag tag = new Tag();
        tag.setId(entity.id.toString());
        tag.setKey(entity.key);
        tag.setValue(entity.value);
        tag.setDescription(entity.description);
        tag.setCreatedAt(entity.createdAt);
        tag.setUpdatedAt(entity.updatedAt);
        return tag;
    }

    private TagPanacheEntity toEntity(Tag tag) {
        TagPanacheEntity entity = new TagPanacheEntity();
        entity.key = tag.getKey();
        entity.value = tag.getValue();
        entity.description = tag.getDescription();
        entity.createdAt = tag.getCreatedAt();
        entity.updatedAt = tag.getUpdatedAt();
        return entity;
    }
}
