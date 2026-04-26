package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.AssetStatus;
import com.zenandops.cmdb.domain.vo.AssetType;
import com.zenandops.cmdb.domain.vo.CostType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO representing an Asset.
 *
 * @param id              the asset identifier
 * @param name            the asset name
 * @param type            the asset type
 * @param organizationId  the owning organization ID
 * @param cost            the asset cost
 * @param costType        the cost type
 * @param acquisitionDate the acquisition date
 * @param status          the asset status
 * @param supplier        the supplier name
 * @param createdAt       when the asset was created
 * @param updatedAt       when the asset was last updated
 */
public record AssetResponse(
        String id,
        String name,
        AssetType type,
        String organizationId,
        BigDecimal cost,
        CostType costType,
        Instant acquisitionDate,
        AssetStatus status,
        String supplier,
        Instant createdAt,
        Instant updatedAt
) {
}
