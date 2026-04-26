package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.CIStatus;
import com.zenandops.cmdb.domain.vo.CIType;

/**
 * Request DTO for creating a new CI.
 *
 * @param name                    the CI name
 * @param type                    the CI type (VM, DATABASE, API, STORAGE, NETWORK)
 * @param organizationId          the owning organization ID
 * @param assetId                 optional associated asset ID
 * @param status                  the CI status (ACTIVE, INACTIVE, DECOMMISSIONED)
 * @param controlledExceptionFlag whether the CI has a controlled exception
 */
public record CreateCIRequest(
        String name,
        CIType type,
        String organizationId,
        String assetId,
        CIStatus status,
        boolean controlledExceptionFlag
) {
}
