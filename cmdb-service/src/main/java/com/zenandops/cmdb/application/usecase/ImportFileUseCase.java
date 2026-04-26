package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.application.port.AssetVersionRepository;
import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.application.port.CIVersionRepository;
import com.zenandops.cmdb.application.port.CmdbEventPublisher;
import com.zenandops.cmdb.application.port.DataSourceRepository;
import com.zenandops.cmdb.application.port.FileImportRecordRepository;
import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.entity.AssetVersion;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.entity.CIVersion;
import com.zenandops.cmdb.domain.entity.DataSource;
import com.zenandops.cmdb.domain.entity.FileImportRecord;
import com.zenandops.cmdb.domain.exception.InvalidFileFormatException;
import com.zenandops.cmdb.domain.vo.AssetStatus;
import com.zenandops.cmdb.domain.vo.AssetType;
import com.zenandops.cmdb.domain.vo.CIStatus;
import com.zenandops.cmdb.domain.vo.CIType;
import com.zenandops.cmdb.domain.vo.CostType;
import com.zenandops.cmdb.domain.vo.DataOrigin;
import com.zenandops.cmdb.domain.vo.DataSourceType;
import com.zenandops.cmdb.domain.vo.ImportStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for importing asset and CI data from structured files.
 * Validates file format (CSV, JSON, XML), processes records creating/updating
 * assets and CIs with new versions, handles partial failures.
 */
@ApplicationScoped
public class ImportFileUseCase {

    private final FileImportRecordRepository fileImportRecordRepository;
    private final DataSourceRepository dataSourceRepository;
    private final AssetRepository assetRepository;
    private final AssetVersionRepository assetVersionRepository;
    private final CIRepository ciRepository;
    private final CIVersionRepository ciVersionRepository;
    private final OrganizationRepository organizationRepository;
    private final CmdbEventPublisher eventPublisher;

    @Inject
    public ImportFileUseCase(FileImportRecordRepository fileImportRecordRepository,
                             DataSourceRepository dataSourceRepository,
                             AssetRepository assetRepository,
                             AssetVersionRepository assetVersionRepository,
                             CIRepository ciRepository,
                             CIVersionRepository ciVersionRepository,
                             OrganizationRepository organizationRepository,
                             CmdbEventPublisher eventPublisher) {
        this.fileImportRecordRepository = fileImportRecordRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.assetRepository = assetRepository;
        this.assetVersionRepository = assetVersionRepository;
        this.ciRepository = ciRepository;
        this.ciVersionRepository = ciVersionRepository;
        this.organizationRepository = organizationRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Import asset and CI data from a structured file.
     *
     * @param fileName the uploaded file name
     * @param fileFormat the file format (CSV, JSON, XML)
     * @param records the parsed records from the file
     * @param userId the authenticated user performing the import
     * @return the FileImportRecord with summary
     */
    public FileImportRecord execute(String fileName, String fileFormat,
                                    List<Map<String, Object>> records, String userId) {
        String normalizedFormat = fileFormat.toUpperCase();
        if (!normalizedFormat.equals("CSV") && !normalizedFormat.equals("JSON")
                && !normalizedFormat.equals("XML")) {
            throw new InvalidFileFormatException(
                    "Unsupported file format: " + fileFormat + ". Supported formats: CSV, JSON, XML");
        }

        // Find or create a FILE data source for this import
        DataSource fileDataSource = dataSourceRepository.findByName("FILE_IMPORT")
                .orElseGet(() -> {
                    DataSource ds = new DataSource();
                    ds.setId(UUID.randomUUID().toString());
                    ds.setName("FILE_IMPORT");
                    ds.setType(DataSourceType.FILE);
                    ds.setReliabilityRating(50);
                    Instant now = Instant.now();
                    ds.setCreatedAt(now);
                    ds.setUpdatedAt(now);
                    dataSourceRepository.save(ds);
                    return ds;
                });

        int successCount = 0;
        int failureCount = 0;
        List<FileImportRecord.ImportError> errors = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            try {
                processRecord(records.get(i), fileDataSource, userId);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                errors.add(new FileImportRecord.ImportError(i, "record", e.getMessage()));
            }
        }

        FileImportRecord record = new FileImportRecord();
        record.setId(UUID.randomUUID().toString());
        record.setFileName(fileName);
        record.setFileFormat(normalizedFormat);
        record.setDataSourceId(fileDataSource.getId());
        record.setTotalRecords(records.size());
        record.setSuccessCount(successCount);
        record.setFailureCount(failureCount);
        record.setErrors(errors);
        record.setImportedBy(userId);
        record.setCreatedAt(Instant.now());

        if (failureCount == 0) {
            record.setStatus(ImportStatus.COMPLETED);
        } else if (successCount > 0) {
            record.setStatus(ImportStatus.PARTIAL);
        } else {
            record.setStatus(ImportStatus.FAILED);
        }

        fileImportRecordRepository.save(record);
        return record;
    }

    private void processRecord(Map<String, Object> recordData, DataSource fileDataSource,
                               String userId) {
        String entityType = (String) recordData.get("entityType");
        if (entityType == null) {
            throw new IllegalArgumentException("Missing required field: entityType");
        }

        String name = (String) recordData.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Missing required field: name");
        }

        String organizationId = (String) recordData.get("organizationId");
        if (organizationId == null || organizationId.isBlank()) {
            throw new IllegalArgumentException("Missing required field: organizationId");
        }

        if (!organizationRepository.existsById(organizationId)) {
            throw new IllegalArgumentException(
                    "Organization not found with id: " + organizationId);
        }

        if ("ASSET".equalsIgnoreCase(entityType)) {
            processAssetRecord(recordData, name, organizationId, fileDataSource, userId);
        } else if ("CI".equalsIgnoreCase(entityType)) {
            processCIRecord(recordData, name, organizationId, fileDataSource, userId);
        } else {
            throw new IllegalArgumentException(
                    "Invalid entityType: " + entityType + ". Must be ASSET or CI");
        }
    }

    private void processAssetRecord(Map<String, Object> recordData, String name,
                                    String organizationId, DataSource fileDataSource,
                                    String userId) {
        String typeStr = (String) recordData.get("type");
        AssetType type = typeStr != null ? AssetType.valueOf(typeStr.toUpperCase()) : AssetType.SOFTWARE;

        Instant now = Instant.now();
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID().toString());
        asset.setName(name);
        asset.setType(type);
        asset.setOrganizationId(organizationId);
        asset.setCost(recordData.containsKey("cost")
                ? new BigDecimal(recordData.get("cost").toString()) : BigDecimal.ZERO);
        asset.setCostType(recordData.containsKey("costType")
                ? CostType.valueOf(recordData.get("costType").toString().toUpperCase()) : CostType.OPEX);
        asset.setAcquisitionDate(now);
        asset.setStatus(AssetStatus.ACTIVE);
        asset.setSupplier(recordData.containsKey("supplier")
                ? (String) recordData.get("supplier") : "Imported");
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);

        assetRepository.save(asset);
        eventPublisher.publishAssetCreated(asset, userId);

        // Create initial version
        AssetVersion version = new AssetVersion();
        version.setId(UUID.randomUUID().toString());
        version.setAssetId(asset.getId());
        version.setVersionNumber(1);
        version.setDescription("Imported from file");
        version.setAttributes(recordData.containsKey("attributes")
                ? asMap(recordData.get("attributes")) : Map.of());
        version.setStartDate(now);
        version.setEndDate(null);
        version.setDataOrigin(DataOrigin.FILE);
        version.setDataSourceId(fileDataSource.getId());
        version.setChangeReference(null);
        version.setCreatedAt(now);

        assetVersionRepository.save(version);
        eventPublisher.publishVersionCreated(version.getId(), "ASSET_VERSION", userId);
    }

    private void processCIRecord(Map<String, Object> recordData, String name,
                                 String organizationId, DataSource fileDataSource,
                                 String userId) {
        String typeStr = (String) recordData.get("type");
        CIType type = typeStr != null ? CIType.valueOf(typeStr.toUpperCase()) : CIType.VM;

        Instant now = Instant.now();
        CI ci = new CI();
        ci.setId(UUID.randomUUID().toString());
        ci.setName(name);
        ci.setType(type);
        ci.setOrganizationId(organizationId);
        ci.setAssetId(recordData.containsKey("assetId")
                ? (String) recordData.get("assetId") : null);
        ci.setStatus(CIStatus.ACTIVE);
        ci.setControlledExceptionFlag(true); // Imported CIs get exception flag
        ci.setCreatedAt(now);
        ci.setUpdatedAt(now);

        ciRepository.save(ci);
        eventPublisher.publishCICreated(ci, userId);

        // Create initial version
        CIVersion version = new CIVersion();
        version.setId(UUID.randomUUID().toString());
        version.setCiId(ci.getId());
        version.setVersionNumber(1);
        version.setAttributes(recordData.containsKey("attributes")
                ? asMap(recordData.get("attributes")) : Map.of());
        version.setStartDate(now);
        version.setEndDate(null);
        version.setDataOrigin(DataOrigin.FILE);
        version.setDataSourceId(fileDataSource.getId());
        version.setChangeReference(null);
        version.setCreatedAt(now);

        ciVersionRepository.save(version);
        eventPublisher.publishVersionCreated(version.getId(), "CI_VERSION", userId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return Map.of();
    }
}
