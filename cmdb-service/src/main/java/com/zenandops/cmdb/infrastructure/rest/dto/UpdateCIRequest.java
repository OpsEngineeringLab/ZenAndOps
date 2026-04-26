package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.CIStatus;

/**
 * Request DTO for updating an existing CI.
 *
 * @param name                    the new CI name
 * @param status                  the new CI status
 * @param controlledExceptionFlag the new controlled exception flag
 */
public record UpdateCIRequest(
        String name,
        CIStatus status,
        boolean controlledExceptionFlag
) {
}
