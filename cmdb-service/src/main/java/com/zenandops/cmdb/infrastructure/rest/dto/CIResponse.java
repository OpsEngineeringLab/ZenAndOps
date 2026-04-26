package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.CIStatus;
import com.zenandops.cmdb.domain.vo.CIType;

import java.time.Instant;

/**
 * Response DTO representing a CI.
 *
 * @param id                      the CI identifier
 * @param name                    the CI name
 * @param type                    the CI type
 * @param organizationId          the owning organization ID
 * @param assetId                 optional associated asset ID
 * @param status                  the CI status
 * @param controlledExceptionFlag whether the CI has a controlled exception
 * @param createdAt               when the CI was created
 * @param updatedAt               when the CI was last updated
 */
public record CIResponse(
        String id,
        String name,
        CIType type,
        String organizationId,
        String assetId,
        CIStatus status,
        boolean controlledExceptionFlag,
        Instant createdAt,
        Instant updatedAt
) {
}
