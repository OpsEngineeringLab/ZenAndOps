package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.PasswordEncoder;
import com.zenandops.auth.application.port.RoleRepository;
import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.Role;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.exception.RoleNotFoundException;
import com.zenandops.auth.domain.exception.UserAlreadyExistsException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Use case for creating a new User.
 * Validates login uniqueness, role names existence, and hashes the password.
 */
@ApplicationScoped
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public CreateUserUseCase(UserRepository userRepository,
                             RoleRepository roleRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a new User with the given data.
     *
     * @param login    the user login (must be unique)
     * @param name     the user display name
     * @param email    the user email
     * @param password the raw password (will be hashed)
     * @param roles    list of role names to assign
     * @param tagIds   list of tag identifiers to assign
     * @return the created User
     * @throws UserAlreadyExistsException if a User with the same login already exists
     * @throws RoleNotFoundException      if any of the specified role names do not exist
     */
    public User execute(String login, String name, String email, String password,
                        List<String> roles, List<String> tagIds) {
        if (userRepository.existsByLogin(login)) {
            throw new UserAlreadyExistsException(
                    "A user with login '" + login + "' already exists");
        }

        if (roles != null && !roles.isEmpty()) {
            validateRolesExist(roles);
        }

        User user = new User();
        user.setLogin(login);
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(roles != null ? new ArrayList<>(roles) : new ArrayList<>());
        user.setTagIds(tagIds != null ? new ArrayList<>(tagIds) : new ArrayList<>());
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        return user;
    }

    private void validateRolesExist(List<String> roleNames) {
        List<Role> foundRoles = roleRepository.findAllByNames(roleNames);
        if (foundRoles.size() != roleNames.size()) {
            List<String> foundNames = foundRoles.stream().map(Role::getName).toList();
            List<String> missing = roleNames.stream()
                    .filter(n -> !foundNames.contains(n))
                    .toList();
            throw new RoleNotFoundException(
                    "Roles not found: " + String.join(", ", missing));
        }
    }
}
