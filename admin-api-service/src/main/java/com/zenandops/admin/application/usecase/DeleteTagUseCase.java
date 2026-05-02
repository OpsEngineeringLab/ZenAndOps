package com.zenandops.admin.application.usecase;

import com.zenandops.admin.application.port.TagManagementPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Use case for deleting a tag definition.
 * Provides realm attribute read/write operations for tag deletion.
 */
@ApplicationScoped
public class DeleteTagUseCase {

    private final TagManagementPort tagManagementPort;

    public DeleteTagUseCase(TagManagementPort tagManagementPort) {
        this.tagManagementPort = tagManagementPort;
    }

    public Map<String, Object> getRealmRepresentation() {
        return tagManagementPort.getRealmRepresentation();
    }

    public void updateRealmRepresentation(Map<String, Object> realmRepresentation) {
        tagManagementPort.updateRealmRepresentation(realmRepresentation);
    }
}
