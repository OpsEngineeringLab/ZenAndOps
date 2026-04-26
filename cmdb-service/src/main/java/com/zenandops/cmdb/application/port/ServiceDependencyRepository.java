package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.ServiceDependency;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for ServiceDependency persistence operations.
 */
public interface ServiceDependencyRepository {

    void save(ServiceDependency dependency);

    Optional<ServiceDependency> findById(String id);

    List<ServiceDependency> findBySourceServiceId(String sourceServiceId);

    List<ServiceDependency> findByTargetServiceId(String targetServiceId);

    void deleteById(String id);

    boolean existsBySourceServiceIdAndTargetServiceId(String sourceServiceId, String targetServiceId);

    long countBySourceServiceIdOrTargetServiceId(String serviceId);
}
