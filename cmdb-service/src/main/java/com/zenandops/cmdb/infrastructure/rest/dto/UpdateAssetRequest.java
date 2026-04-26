package com.zenandops.cmdb.infrastructure.rest.dto;

import com.zenandops.cmdb.domain.vo.AssetStatus;
import com.zenandops.cmdb.domain.vo.CostType;

import java.math.BigDecimal;

/**
 * Request DTO for updating an existing Asset.
 *
 * @param name     the new asset name
 * @param cost     the new cost
 * @param costType the new cost type (CAPEX, OPEX)
 * @param status   the new asset status
 * @param supplier the new supplier name
 */
public record UpdateAssetRequest(
        String name,
        BigDecimal cost,
        CostType costType,
        AssetStatus status,
        String supplier
) {
}
