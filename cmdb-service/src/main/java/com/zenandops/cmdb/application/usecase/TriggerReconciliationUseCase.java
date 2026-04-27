package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.application.port.AssetVersionRepository;
import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.application.port.CIVersionRepository;
import com.zenandops.cmdb.application.port.DataSourceRepository;
import com.zenandops.cmdb.application.port.ReconciliationRecordRepository;
import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.entity.AssetVersion;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.entity.CIVersion;
import com.zenandops.cmdb.domain.entity.DataSource;
import com.zenandops.cmdb.domain.entity.ReconciliationRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Use case for triggering a reconciliation process.
 * Compares records from different data sources, identifies duplicates
 * by name+type+organization, and resolves conflicts by preferring
 * the version from the data source with the higher reliability rating.
 */
@ApplicationScoped
public class TriggerReconciliationUseCase {

    private final ReconciliationRecordRepository reconciliationRecordRepository;
    private final AssetRepository assetRepository;
    private final AssetVersionRepository assetVersionRepository;
    private final CIRepository ciRepository;
    private final CIVersionRepository ciVersionRepository;
    private final DataSourceRepository dataSourceRepository;

    @Inject
    public TriggerReconciliationUseCase(ReconciliationRecordRepository reconciliationRecordRepository,
                                        AssetRepository assetRepository,
                                        AssetVersionRepository assetVersionRepository,
                                        CIRepository ciRepository,
                                        CIVersionRepository ciVersionRepository,
                                        DataSourceRepository dataSourceRepository) {
        this.reconciliationRecordRepository = reconciliationRecordRepository;
        this.assetRepository = assetRepository;
        this.assetVersionRepository = assetVersionRepository;
        this.ciRepository = ciRepository;
        this.ciVersionRepository = ciVersionRepository;
        this.dataSourceRepository = dataSourceRepository;
    }

    /**
     * Trigger a reconciliation process for the given entity type.
     *
     * @param entityType the entity type to reconcile (ASSET or CI)
     * @param userId     the authenticated user triggering the reconciliation
     * @return the ReconciliationRecord with report
     */
    public ReconciliationRecord execute(String entityType, String userId) {
        if ("ASSET".equalsIgnoreCase(entityType)) {
            return reconcileAssets(userId);
        } else if ("CI".equalsIgnoreCase(entityType)) {
            return reconcileCIs(userId);
        } else {
            throw new IllegalArgumentException(
                    "Invalid entityType: " + entityType + ". Must be ASSET or CI");
        }
    }

    private ReconciliationRecord reconcileAssets(String userId) {
        List<Asset> allAssets = assetRepository.findAll();
        int recordsAnalyzed = allAssets.size();
        int duplicatesFound = 0;
        int conflictsResolved = 0;
        int unresolvedConflicts = 0;
        List<ReconciliationRecord.ReconciliationDetail> details = new ArrayList<>();

        // Group assets by name+type+organizationId to find duplicates
        Map<String, List<Asset>> groups = new HashMap<>();
        for (Asset asset : allAssets) {
            String key = asset.getName() + "|" + asset.getType() + "|" + asset.getOrganizationId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(asset);
        }

        for (Map.Entry<String, List<Asset>> entry : groups.entrySet()) {
            List<Asset> group = entry.getValue();
            if (group.size() <= 1) {
                continue;
            }

            duplicatesFound += group.size() - 1;

            // Try to resolve by comparing latest version data source reliability
            Asset preferred = resolveAssetConflict(group);
            if (preferred != null) {
                conflictsResolved++;
                details.add(new ReconciliationRecord.ReconciliationDetail(
                        preferred.getId(), preferred.getName(),
                        "DUPLICATE", "AUTO_RESOLVED",
                        getAssetDataSourceId(preferred)));
            } else {
                unresolvedConflicts++;
                details.add(new ReconciliationRecord.ReconciliationDetail(
                        group.getFirst().getId(), group.getFirst().getName(),
                        "DUPLICATE", "MANUAL_REVIEW", null));
            }
        }

        return saveRecord("ASSET", recordsAnalyzed, duplicatesFound,
                conflictsResolved, unresolvedConflicts, details, userId);
    }

    private ReconciliationRecord reconcileCIs(String userId) {
        List<CI> allCIs = ciRepository.findAll();
        int recordsAnalyzed = allCIs.size();
        int duplicatesFound = 0;
        int conflictsResolved = 0;
        int unresolvedConflicts = 0;
        List<ReconciliationRecord.ReconciliationDetail> details = new ArrayList<>();

        // Group CIs by name+type+organizationId to find duplicates
        Map<String, List<CI>> groups = new HashMap<>();
        for (CI ci : allCIs) {
            String key = ci.getName() + "|" + ci.getType() + "|" + ci.getOrganizationId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(ci);
        }

        for (Map.Entry<String, List<CI>> entry : groups.entrySet()) {
            List<CI> group = entry.getValue();
            if (group.size() <= 1) {
                continue;
            }

            duplicatesFound += group.size() - 1;

            // Try to resolve by comparing latest version data source reliability
            CI preferred = resolveCIConflict(group);
            if (preferred != null) {
                conflictsResolved++;
                details.add(new ReconciliationRecord.ReconciliationDetail(
                        preferred.getId(), preferred.getName(),
                        "DUPLICATE", "AUTO_RESOLVED",
                        getCIDataSourceId(preferred)));
            } else {
                unresolvedConflicts++;
                details.add(new ReconciliationRecord.ReconciliationDetail(
                        group.getFirst().getId(), group.getFirst().getName(),
                        "DUPLICATE", "MANUAL_REVIEW", null));
            }
        }

        return saveRecord("CI", recordsAnalyzed, duplicatesFound,
                conflictsResolved, unresolvedConflicts, details, userId);
    }

    private Asset resolveAssetConflict(List<Asset> duplicates) {
        Asset bestAsset = null;
        int bestRating = -1;

        for (Asset asset : duplicates) {
            Optional<AssetVersion> activeVersion = assetVersionRepository.findActiveByAssetId(asset.getId());
            if (activeVersion.isPresent()) {
                Optional<DataSource> ds = dataSourceRepository.findById(activeVersion.get().getDataSourceId());
                if (ds.isPresent() && ds.get().getReliabilityRating() > bestRating) {
                    bestRating = ds.get().getReliabilityRating();
                    bestAsset = asset;
                }
            }
        }

        return bestAsset;
    }

    private CI resolveCIConflict(List<CI> duplicates) {
        CI bestCI = null;
        int bestRating = -1;

        for (CI ci : duplicates) {
            Optional<CIVersion> activeVersion = ciVersionRepository.findActiveByCiId(ci.getId());
            if (activeVersion.isPresent()) {
                Optional<DataSource> ds = dataSourceRepository.findById(activeVersion.get().getDataSourceId());
                if (ds.isPresent() && ds.get().getReliabilityRating() > bestRating) {
                    bestRating = ds.get().getReliabilityRating();
                    bestCI = ci;
                }
            }
        }

        return bestCI;
    }

    private String getAssetDataSourceId(Asset asset) {
        return assetVersionRepository.findActiveByAssetId(asset.getId())
                .map(AssetVersion::getDataSourceId)
                .orElse(null);
    }

    private String getCIDataSourceId(CI ci) {
        return ciVersionRepository.findActiveByCiId(ci.getId())
                .map(CIVersion::getDataSourceId)
                .orElse(null);
    }

    private ReconciliationRecord saveRecord(String entityType, int recordsAnalyzed,
                                            int duplicatesFound, int conflictsResolved,
                                            int unresolvedConflicts,
                                            List<ReconciliationRecord.ReconciliationDetail> details,
                                            String userId) {
        ReconciliationRecord record = new ReconciliationRecord();
        record.setEntityType(entityType);
        record.setRecordsAnalyzed(recordsAnalyzed);
        record.setDuplicatesFound(duplicatesFound);
        record.setConflictsResolved(conflictsResolved);
        record.setUnresolvedConflicts(unresolvedConflicts);
        record.setDetails(details);
        record.setTriggeredBy(userId);
        record.setCreatedAt(Instant.now());

        reconciliationRecordRepository.save(record);
        return record;
    }
}
