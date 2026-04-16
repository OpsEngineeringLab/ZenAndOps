package com.zenandops.auth.infrastructure.adapter.policy;

import com.zenandops.auth.application.port.PolicyEngine;
import com.zenandops.auth.application.port.TagRepository;
import com.zenandops.auth.domain.entity.Tag;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.domain.valueobject.AbacPolicy;
import com.zenandops.auth.domain.valueobject.RbacPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

/**
 * Default adapter implementing the PolicyEngine port.
 * Evaluates RBAC rules by role matching and ABAC rules by Tag key-value matching.
 */
@ApplicationScoped
public class DefaultPolicyEngine implements PolicyEngine {

    private final TagRepository tagRepository;

    @Inject
    public DefaultPolicyEngine(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public boolean evaluateRbac(User user, RbacPolicy policy) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        if (policy.requiredRoles() == null || policy.requiredRoles().isEmpty()) {
            return true;
        }
        // User must have at least one of the required roles
        return user.getRoles().stream()
                .anyMatch(policy.requiredRoles()::contains);
    }

    @Override
    public boolean evaluateAbac(User user, AbacPolicy policy) {
        if (policy.requiredUserAttributes() == null || policy.requiredUserAttributes().isEmpty()) {
            return true;
        }
        // User must have tags assigned to be evaluated against ABAC policies
        if (user.getTagIds() == null || user.getTagIds().isEmpty()) {
            return false;
        }

        // Resolve user's tag IDs into Tag entities
        List<Tag> userTags = tagRepository.findAllByIds(user.getTagIds());

        // Every required attribute must match at least one of the user's resolved tags
        for (Map.Entry<String, String> required : policy.requiredUserAttributes().entrySet()) {
            boolean matched = userTags.stream()
                    .anyMatch(tag -> tag.getKey().equals(required.getKey())
                            && tag.getValue().equals(required.getValue()));
            if (!matched) {
                return false;
            }
        }
        return true;
    }
}
