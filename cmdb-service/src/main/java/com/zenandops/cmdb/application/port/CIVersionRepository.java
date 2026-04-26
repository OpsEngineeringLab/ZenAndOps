package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.CIVersion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for CIVersion persistence operations.
 */
public interface CIVersionRepository {

    void save(CIVersion version);

    List<CIVersion> findByCiId(String ciId);

    Optional<CIVersion> findActiveByCiId(String ciId);

    List<CIVersion> findByCiIdOrderByVersionNumber(String ciId);

    long countByCiId(String ciId);

    int getMaxVersionNumber(String ciId);

    long countByDataSourceId(String dataSourceId);

    Optional<CIVersion> findByCiIdAtTime(String ciId, Instant timestamp);
}
