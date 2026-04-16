package com.zenandops.auth.infrastructure.adapter.persistence;

import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.User;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Panache adapter implementing the UserRepository port.
 */
@ApplicationScoped
public class MongoUserRepository implements UserRepository {

    @Override
    public Optional<User> findByLogin(String login) {
        return UserPanacheEntity.<UserPanacheEntity>find("login", login)
                .firstResultOptional()
                .map(this::toDomain);
    }

    @Override
    public Optional<User> findById(String id) {
        return UserPanacheEntity.<UserPanacheEntity>findByIdOptional(new org.bson.types.ObjectId(id))
                .map(this::toDomain);
    }

    @Override
    public List<User> findAll() {
        return UserPanacheEntity.<UserPanacheEntity>listAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(User user) {
        UserPanacheEntity entity = toEntity(user);
        if (user.getId() != null) {
            entity.id = new org.bson.types.ObjectId(user.getId());
            entity.update();
        } else {
            entity.persist();
            user.setId(entity.id.toString());
        }
    }

    private User toDomain(UserPanacheEntity entity) {
        User user = new User();
        user.setId(entity.id.toString());
        user.setLogin(entity.login);
        user.setName(entity.name);
        user.setEmail(entity.email);
        user.setPasswordHash(entity.passwordHash);
        user.setRoles(entity.roles);
        user.setTagIds(entity.tagIds);
        user.setActive(entity.active);
        user.setCreatedAt(entity.createdAt);
        user.setUpdatedAt(entity.updatedAt);
        return user;
    }

    private UserPanacheEntity toEntity(User user) {
        UserPanacheEntity entity = new UserPanacheEntity();
        entity.login = user.getLogin();
        entity.name = user.getName();
        entity.email = user.getEmail();
        entity.passwordHash = user.getPasswordHash();
        entity.roles = user.getRoles();
        entity.tagIds = user.getTagIds();
        entity.active = user.isActive();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        return entity;
    }
}
