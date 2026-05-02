package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.TagManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Use case for updating a tag definition.
 * Provides realm attribute read/write operations for tag updates.
 */
@ApplicationScoped
public class UpdateTagUseCase {

    private final TagManagementPort tagManagementPort;

    public UpdateTagUseCase(TagManagementPort tagManagementPort) {
        this.tagManagementPort = tagManagementPort;
    }

    public Map<String, Object> getRealmRepresentation() {
        return tagManagementPort.getRealmRepresentation();
    }

    public void updateRealmRepresentation(Map<String, Object> realmRepresentation) {
        tagManagementPort.updateRealmRepresentation(realmRepresentation);
    }
}
