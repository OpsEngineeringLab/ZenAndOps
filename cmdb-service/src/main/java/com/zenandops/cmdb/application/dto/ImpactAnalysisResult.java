package com.zenandops.cmdb.application.dto;

import java.util.List;

/**
 * Result of an impact analysis traversal.
 *
 * @param rootEntityId                the root entity identifier
 * @param rootEntityName              the root entity name
 * @param rootEntityType              the root entity type (CI or SERVICE)
 * @param affectedEntities            list of affected entities with paths and depths
 * @param totalAffectedServices       total count of affected services
 * @param totalAffectedCIs            total count of affected CIs
 * @param circularDependencyWarnings  list of circular dependency warnings
 * @param maxDepthReached             whether the max traversal depth was reached
 */
public record ImpactAnalysisResult(
        String rootEntityId,
        String rootEntityName,
        String rootEntityType,
        List<AffectedEntity> affectedEntities,
        int totalAffectedServices,
        int totalAffectedCIs,
        List<String> circularDependencyWarnings,
        boolean maxDepthReached
) {
}
