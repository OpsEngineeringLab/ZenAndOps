package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.vo.AssetStatus;
import com.zenandops.cmdb.domain.vo.AssetType;
import com.zenandops.cmdb.domain.vo.CostType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Outbound port for Asset persistence operations.
 */
public interface AssetRepository {

    void save(Asset asset);

    Optional<Asset> findById(String id);

    List<Asset> findAll();

    List<Asset> findByOrganizationId(String organizationId);

    void deleteById(String id);

    boolean existsById(String id);

    long countByOrganizationId(String organizationId);

    List<Asset> findWithFilters(String organizationId, AssetType type, CostType costType,
                                AssetStatus status, String supplier);

    List<Asset> findWithFilters(String organizationId, AssetType type, CostType costType,
                                AssetStatus status, String supplier, int page, int size);

    long countWithFilters(String organizationId, AssetType type, CostType costType,
                          AssetStatus status, String supplier);

    List<Asset> getCostSummary();
}
