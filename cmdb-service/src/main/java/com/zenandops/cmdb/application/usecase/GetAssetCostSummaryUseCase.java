package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.CostSummaryEntry;
import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.vo.CostType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Use case for computing asset cost summary grouped by organization and cost type.
 * Computes the summary in-memory from all assets.
 */
@ApplicationScoped
public class GetAssetCostSummaryUseCase {

    private final AssetRepository assetRepository;

    @Inject
    public GetAssetCostSummaryUseCase(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    /**
     * Returns cost summary grouped by organization and cost type.
     *
     * @return list of cost summary entries
     */
    public List<CostSummaryEntry> execute() {
        List<Asset> allAssets = assetRepository.findAll();

        Map<Map.Entry<String, CostType>, BigDecimal> grouped = allAssets.stream()
                .filter(a -> a.getCost() != null && a.getOrganizationId() != null && a.getCostType() != null)
                .collect(Collectors.groupingBy(
                        a -> new AbstractMap.SimpleEntry<>(a.getOrganizationId(), a.getCostType()),
                        Collectors.reducing(BigDecimal.ZERO, Asset::getCost, BigDecimal::add)
                ));

        return grouped.entrySet().stream()
                .map(e -> new CostSummaryEntry(e.getKey().getKey(), e.getKey().getValue(), e.getValue()))
                .toList();
    }
}
