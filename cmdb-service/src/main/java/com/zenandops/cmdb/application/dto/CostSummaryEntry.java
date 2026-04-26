package com.zenandops.cmdb.application.dto;

import com.zenandops.cmdb.domain.vo.CostType;

import java.math.BigDecimal;

/**
 * DTO representing a cost summary entry grouped by organization and cost type.
 *
 * @param organizationId the organization identifier
 * @param costType       the cost type (CAPEX or OPEX)
 * @param totalCost      the total cost for this group
 */
public record CostSummaryEntry(
        String organizationId,
        CostType costType,
        BigDecimal totalCost
) {
}
