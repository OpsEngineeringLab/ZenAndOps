package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.Service;
import com.zenandops.cmdb.domain.vo.CriticalityLevel;
import com.zenandops.cmdb.domain.vo.ServiceStatus;
import com.zenandops.cmdb.domain.vo.ServiceType;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Service persistence operations.
 */
public interface ServiceRepository {

    void save(Service service);

    Optional<Service> findById(String id);

    List<Service> findAll();

    List<Service> findByParentId(String parentId);

    List<Service> findByOrganizationId(String organizationId);

    void deleteById(String id);

    boolean existsById(String id);

    long countByParentId(String parentId);

    long countByOrganizationId(String organizationId);

    List<Service> findWithFilters(String organizationId, ServiceType type,
                                  CriticalityLevel criticality, ServiceStatus status);
}
