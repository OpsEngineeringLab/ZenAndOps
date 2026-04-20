package com.zenandops.auth.domain.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Role entity representing a named authorization role with associated permissions (RBAC layer).
 * Roles are assigned to users and carry permission strings that define what actions are allowed.
 * Designed as a mutable class for MongoDB Panache compatibility.
 */
public class Role {

    private String id;
    private String name;
    private String description;
    private List<String> permissions;
    private Instant createdAt;
    private Instant updatedAt;

    public Role() {
        this.permissions = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
