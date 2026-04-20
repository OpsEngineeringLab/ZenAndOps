package com.zenandops.auth.infrastructure.adapter.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.zenandops.auth.application.port.RoleRepository;
import com.zenandops.auth.domain.entity.Role;
import io.quarkus.panache.common.Page;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB Panache adapter implementing the RoleRepository port.
 * Creates a unique index on { name: 1 } at startup.
 */
@ApplicationScoped
public class MongoRoleRepository implements RoleRepository {

    @Inject
    MongoClient mongoClient;

    void onStartup(@Observes StartupEvent event) {
        mongoClient.getDatabase("zenandops-auth")
                .getCollection("roles")
                .createIndex(
                        Indexes.ascending("name"),
                        new IndexOptions().unique(true)
                );
    }

    @Override
    public void save(Role role) {
        RolePanacheEntity entity = toEntity(role);
        if (role.getId() != null) {
            entity.id = new ObjectId(role.getId());
            entity.update();
        } else {
            entity.persist();
            role.setId(entity.id.toString());
        }
    }

    @Override
    public Optional<Role> findById(String id) {
        return RolePanacheEntity.<RolePanacheEntity>findByIdOptional(new ObjectId(id))
                .map(this::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return RolePanacheEntity.<RolePanacheEntity>find("name", name)
                .firstResultOptional()
                .map(this::toDomain);
    }

    @Override
    public List<Role> findAllByNames(List<String> names) {
        return RolePanacheEntity.<RolePanacheEntity>find("name in ?1", names)
                .list()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Role> findAll(int page, int size) {
        return RolePanacheEntity.<RolePanacheEntity>findAll()
                .page(Page.of(page, size))
                .list()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return RolePanacheEntity.count();
    }

    @Override
    public void delete(String id) {
        RolePanacheEntity.deleteById(new ObjectId(id));
    }

    @Override
    public boolean existsAssignedToAnyUser(String roleName) {
        return UserPanacheEntity.count("roles", roleName) > 0;
    }

    private Role toDomain(RolePanacheEntity entity) {
        Role role = new Role();
        role.setId(entity.id.toString());
        role.setName(entity.name);
        role.setDescription(entity.description);
        role.setPermissions(entity.permissions);
        role.setCreatedAt(entity.createdAt);
        role.setUpdatedAt(entity.updatedAt);
        return role;
    }

    private RolePanacheEntity toEntity(Role role) {
        RolePanacheEntity entity = new RolePanacheEntity();
        entity.name = role.getName();
        entity.description = role.getDescription();
        entity.permissions = role.getPermissions();
        entity.createdAt = role.getCreatedAt();
        entity.updatedAt = role.getUpdatedAt();
        return entity;
    }
}
