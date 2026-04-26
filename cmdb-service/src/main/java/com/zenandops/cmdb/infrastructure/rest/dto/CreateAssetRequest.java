package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.AssetStatus;
import com.zenandops.cmdb.domain.vo.AssetType;
import com.zenandops.cmdb.domain.vo.CostType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request DTO for creating a new Asset.
 *
 * @param name            the asset name
 * @param type            the asset type (HARDWARE, SOFTWARE, CLOUD)
 * @param organizationId  the owning organization ID
 * @param cost            the asset cost
 * @param costType        the cost type (CAPEX, OPEX)
 * @param acquisitionDate the acquisition date
 * @param status          the asset status (ACTIVE, INACTIVE, RETIRED)
 * @param supplier        the supplier name
 */
public record CreateAssetRequest(
        String name,
        AssetType type,
        String organizationId,
        BigDecimal cost,
        CostType costType,
        Instant acquisitionDate,
        AssetStatus status,
        String supplier
) {
}
