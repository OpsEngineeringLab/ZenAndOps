package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.vo.CIStatus;
import com.zenandops.cmdb.domain.vo.CIType;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for CI persistence operations.
 */
public interface CIRepository {

    void save(CI ci);

    Optional<CI> findById(String id);

    List<CI> findAll();

    List<CI> findByOrganizationId(String organizationId);

    List<CI> findByAssetId(String assetId);

    void deleteById(String id);

    boolean existsById(String id);

    long countByOrganizationId(String organizationId);

    long countByAssetId(String assetId);

    List<CI> findWithFilters(String organizationId, CIType type, CIStatus status, String assetId);
}
