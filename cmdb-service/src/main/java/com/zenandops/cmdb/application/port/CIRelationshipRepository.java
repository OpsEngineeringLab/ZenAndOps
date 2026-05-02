package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.CIRelationship;
import com.zenandops.cmdb.domain.vo.RelationshipType;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for CIRelationship persistence operations.
 */
public interface CIRelationshipRepository {

    void save(CIRelationship relationship);

    Optional<CIRelationship> findById(String id);

    List<CIRelationship> findBySourceCIId(String sourceCIId);

    List<CIRelationship> findByTargetCIId(String targetCIId);

    void deleteById(String id);

    boolean existsBySourceCIIdAndTargetCIIdAndRelationshipType(String sourceCIId, String targetCIId,
                                                                RelationshipType relationshipType);

    long countBySourceCIIdOrTargetCIId(String ciId);

    List<CIRelationship> findWithFilters(String ciId, int page, int size);

    long countWithFilters(String ciId);
}
