package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.Organization;
import com.zenandops.cmdb.domain.vo.OrganizationType;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Organization persistence operations.
 */
public interface OrganizationRepository {

    void save(Organization organization);

    Optional<Organization> findById(String id);

    List<Organization> findAll();

    List<Organization> findByParentId(String parentId);

    void deleteById(String id);

    boolean existsById(String id);

    long countByParentId(String parentId);

    boolean existsByParentIdAndName(String parentId, String name);

    long countByType(OrganizationType type);
}
