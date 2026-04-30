package com.zenandops.gateway.infrastructure.adapter.keycloak;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenandops.gateway.infrastructure.rest.dto.CreateTagRequest;
import com.zenandops.gateway.infrastructure.rest.dto.TagAssignment;
import com.zenandops.gateway.infrastructure.rest.dto.TagResponse;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Translates between Keycloak realm attribute JSON (tag definitions stored in
 * {@code _zenandops_tags}) and ZenAndOps tag DTOs.
 * <p>
 * Also handles user tag assignments stored as the {@code tags} user attribute.
 * <p>
 * This is a stateless utility class with static methods — no CDI needed.
 * An {@link ObjectMapper} is passed explicitly to avoid hidden global state.
 */
public final class TagResponseTranslator {

    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<TagAssignment>> TAG_ASSIGNMENT_LIST_TYPE = new TypeReference<>() {};
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private TagResponseTranslator() {
        // utility class
    }

    // ── Tag definition operations ───────────────────────────────────────

    /**
     * Parse the {@code _zenandops_tags} realm attribute JSON into a list of
     * {@link TagResponse} objects.
     *
     * @param json the JSON string from the realm attribute
     * @return the parsed list of tag responses, or an empty list on parse failure
     */
    public static List<TagResponse> parseTagDefinitions(String json) {
        return parseTagDefinitions(json, DEFAULT_MAPPER);
    }

    /**
     * Parse the {@code _zenandops_tags} realm attribute JSON into a list of
     * {@link TagResponse} objects using the provided {@link ObjectMapper}.
     *
     * @param json         the JSON string from the realm attribute
     * @param objectMapper the Jackson ObjectMapper for JSON parsing
     * @return the parsed list of tag responses, or an empty list on parse failure
     */
    public static List<TagResponse> parseTagDefinitions(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> tagMaps = objectMapper.readValue(json, LIST_MAP_TYPE);
            return tagMaps.stream()
                    .map(TagResponseTranslator::toTagResponse)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Serialize a list of {@link TagResponse} objects back to the JSON format
     * used by the {@code _zenandops_tags} realm attribute.
     *
     * @param tags the list of tag responses
     * @return the serialized JSON string
     */
    public static String serializeTagDefinitions(List<TagResponse> tags) {
        return serializeTagDefinitions(tags, DEFAULT_MAPPER);
    }

    /**
     * Serialize a list of {@link TagResponse} objects back to the JSON format
     * using the provided {@link ObjectMapper}.
     *
     * @param tags         the list of tag responses
     * @param objectMapper the Jackson ObjectMapper for JSON serialization
     * @return the serialized JSON string
     */
    public static String serializeTagDefinitions(List<TagResponse> tags, ObjectMapper objectMapper) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        try {
            List<Map<String, String>> tagMaps = tags.stream()
                    .<Map<String, String>>map(tag -> {
                        // Use a LinkedHashMap to preserve insertion order for readability
                        var map = new java.util.LinkedHashMap<String, String>();
                        map.put("id", tag.id());
                        map.put("key", tag.key());
                        map.put("value", tag.value());
                        map.put("description", tag.description());
                        map.put("createdAt", tag.createdAt());
                        map.put("updatedAt", tag.updatedAt());
                        return map;
                    })
                    .toList();
            return objectMapper.writeValueAsString(tagMaps);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * Convert a single tag definition map to a {@link TagResponse}.
     *
     * @param tagMap the tag definition map
     * @return the tag response
     */
    public static TagResponse toTagResponse(Map<String, Object> tagMap) {
        return new TagResponse(
                stringValue(tagMap.get("id")),
                stringValue(tagMap.get("key")),
                stringValue(tagMap.get("value")),
                stringValue(tagMap.get("description")),
                stringValue(tagMap.get("createdAt")),
                stringValue(tagMap.get("updatedAt"))
        );
    }

    /**
     * Create a new tag definition map from a {@link CreateTagRequest}.
     * Generates a UUID for the id and sets createdAt/updatedAt to the current time.
     *
     * @param request the create tag request
     * @return the tag definition as a map
     */
    public static Map<String, Object> createTagDefinition(CreateTagRequest request) {
        String now = Instant.now().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER);
        String id = UUID.randomUUID().toString();

        var tagMap = new java.util.LinkedHashMap<String, Object>();
        tagMap.put("id", id);
        tagMap.put("key", request.key());
        tagMap.put("value", request.value());
        tagMap.put("description", request.description());
        tagMap.put("createdAt", now);
        tagMap.put("updatedAt", now);
        return tagMap;
    }

    // ── User tag assignment operations ──────────────────────────────────

    /**
     * Parse the user's {@code tags} attribute into a list of {@link TagAssignment} objects.
     * The attribute is stored as a single-element list containing a JSON array string
     * (Keycloak user attribute format).
     *
     * @param keycloakUser the Keycloak user representation map
     * @return the parsed list of tag assignments, or an empty list on parse failure
     */
    public static List<TagAssignment> parseUserTags(Map<String, Object> keycloakUser) {
        return parseUserTags(keycloakUser, DEFAULT_MAPPER);
    }

    /**
     * Parse the user's {@code tags} attribute into a list of {@link TagAssignment} objects
     * using the provided {@link ObjectMapper}.
     *
     * @param keycloakUser the Keycloak user representation map
     * @param objectMapper the Jackson ObjectMapper for JSON parsing
     * @return the parsed list of tag assignments, or an empty list on parse failure
     */
    @SuppressWarnings("unchecked")
    public static List<TagAssignment> parseUserTags(Map<String, Object> keycloakUser,
                                                    ObjectMapper objectMapper) {
        Object attributesObj = keycloakUser.get("attributes");
        if (!(attributesObj instanceof Map<?, ?> attributes)) {
            return List.of();
        }

        Object tagsObj = attributes.get("tags");
        if (tagsObj == null) {
            return List.of();
        }

        // Keycloak user attributes are Map<String, List<String>>
        // The tags value is a list with a single JSON array string
        String tagsJson;
        if (tagsObj instanceof List<?> tagsList && !tagsList.isEmpty()) {
            tagsJson = String.valueOf(tagsList.getFirst());
        } else if (tagsObj instanceof String s) {
            tagsJson = s;
        } else {
            return List.of();
        }

        try {
            return objectMapper.readValue(tagsJson, TAG_ASSIGNMENT_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Serialize a list of {@link TagAssignment} objects to the JSON format
     * used by the Keycloak user {@code tags} attribute.
     *
     * @param tags the list of tag assignments
     * @return the serialized JSON string
     */
    public static String serializeUserTags(List<TagAssignment> tags) {
        return serializeUserTags(tags, DEFAULT_MAPPER);
    }

    /**
     * Serialize a list of {@link TagAssignment} objects to the JSON format
     * using the provided {@link ObjectMapper}.
     *
     * @param tags         the list of tag assignments
     * @param objectMapper the Jackson ObjectMapper for JSON serialization
     * @return the serialized JSON string
     */
    public static String serializeUserTags(List<TagAssignment> tags, ObjectMapper objectMapper) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (Exception e) {
            return "[]";
        }
    }

    // ── Internal helpers ────────────────────────────────────────────────

    private static String stringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}
