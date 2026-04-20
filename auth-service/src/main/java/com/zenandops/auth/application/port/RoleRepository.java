package com.zenandops.auth.application.port;

import com.zenandops.auth.domain.entity.Role;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Role persistence operations.
 */
public interface RoleRepository {

    void save(Role role);

    Optional<Role> findById(String id);

    Optional<Role> findByName(String name);

    List<Role> findAllByNames(List<String> names);

    List<Role> findAll(int page, int size);

    long count();

    void delete(String id);

    boolean existsAssignedToAnyUser(String roleName);
}
