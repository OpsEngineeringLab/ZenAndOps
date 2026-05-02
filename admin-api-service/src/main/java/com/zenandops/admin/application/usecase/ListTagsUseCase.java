package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.TagManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Use case for listing tag definitions from realm attributes.
 */
@ApplicationScoped
public class ListTagsUseCase {

    private final TagManagementPort tagManagementPort;

    public ListTagsUseCase(TagManagementPort tagManagementPort) {
        this.tagManagementPort = tagManagementPort;
    }

    public Map<String, Object> execute() {
        return tagManagementPort.getRealmRepresentation();
    }
}
