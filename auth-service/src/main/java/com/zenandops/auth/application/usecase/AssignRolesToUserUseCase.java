package com.zenandops.auth.application.usecase;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Use case for assigning one or more Roles to a User.
 * Validates that all role names exist as Role entities. Ignores duplicates.
 */
@ApplicationScoped
public class AssignRolesToUserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Inject
    public AssignRolesToUserUseCase(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Assign roles to a user.
     *
     * @param userId    the user identifier
     * @param roleNames the list of role names to assign
     * @return the updated User
     * @throws UserNotFoundException if the user does not exist
     * @throws RoleNotFoundException if any of the role names do not exist as Role entities
     */
    public User execute(String userId, List<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        List<Role> foundRoles = roleRepository.findAllByNames(roleNames);
        if (foundRoles.size() != roleNames.size()) {
            List<String> foundNames = foundRoles.stream().map(Role::getName).toList();
            List<String> missing = roleNames.stream()
                    .filter(n -> !foundNames.contains(n))
                    .toList();
            throw new RoleNotFoundException("Roles not found: " + String.join(", ", missing));
        }

        Set<String> merged = new LinkedHashSet<>(user.getRoles());
        merged.addAll(roleNames);
        user.setRoles(new ArrayList<>(merged));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        return user;
    }
}
