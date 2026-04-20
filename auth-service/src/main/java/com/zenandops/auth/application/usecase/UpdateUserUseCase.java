package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.PasswordEncoder;
import com.zenandops.auth.application.port.RoleRepository;
import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.Role;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.exception.RoleNotFoundException;
import com.zenandops.auth.domain.exception.UserNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Use case for updating an existing User.
 * Updates permitted fields: name, email, active, roles, tagIds.
 * Hashes password if provided. Validates role names exist.
 */
@ApplicationScoped
public class UpdateUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public UpdateUserUseCase(UserRepository userRepository,
                             RoleRepository roleRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Update a User's fields.
     *
     * @param id       the user identifier
     * @param name     the new name
     * @param email    the new email
     * @param password the new password (optional, null means no change)
     * @param active   the new active status
     * @param roles    the new list of role names
     * @param tagIds   the new list of tag identifiers
     * @return the updated User
     * @throws UserNotFoundException if no User exists with the given id
     * @throws RoleNotFoundException if any of the specified role names do not exist
     */
    public User execute(String id, String name, String email, String password,
                        Boolean active, List<String> roles, List<String> tagIds) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        if (roles != null && !roles.isEmpty()) {
            validateRolesExist(roles);
        }

        user.setName(name);
        user.setEmail(email);

        if (password != null && !password.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        if (active != null) {
            user.setActive(active);
        }

        user.setRoles(roles != null ? new ArrayList<>(roles) : new ArrayList<>());
        user.setTagIds(tagIds != null ? new ArrayList<>(tagIds) : new ArrayList<>());
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
