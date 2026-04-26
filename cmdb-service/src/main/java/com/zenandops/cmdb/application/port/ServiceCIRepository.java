package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.ServiceCI;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for ServiceCI association persistence operations.
 */
public interface ServiceCIRepository {

    void save(ServiceCI serviceCI);

    Optional<ServiceCI> findById(String id);

    List<ServiceCI> findByServiceId(String serviceId);

    List<ServiceCI> findByCiId(String ciId);

    void deleteById(String id);

    boolean existsByServiceIdAndCiId(String serviceId, String ciId);

    long countByCiId(String ciId);

    long countByServiceId(String serviceId);
}
