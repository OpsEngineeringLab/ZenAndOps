package com.zenandops.auth.domain.valueobject;

import java.util.Map;

/**
 * Value object representing an ABAC authorization rule.
 * Access is granted when the user's resolved Tag key-value pairs, resource attributes,
 * and environment conditions all match the policy requirements.
 *
 * @param resource                     the protected resource identifier
 * @param action                       the action being performed on the resource
 * @param requiredUserAttributes       Tag key-value pairs the user must possess (key = Tag_Key, value = Tag_Value)
 * @param requiredResourceAttributes   attributes the resource must possess
 * @param environmentConditions        environmental conditions that must be met
 */
public record AbacPolicy(
        String resource,
        String action,
        Map<String, String> requiredUserAttributes,
        Map<String, String> requiredResourceAttributes,
        Map<String, String> environmentConditions
) {
}
