package com.zenandops.cmdb.infrastructure.adapter.seed;

import com.zenandops.cmdb.application.port.AssetRepository;
import com.zenandops.cmdb.application.port.CIRelationshipRepository;
import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.application.port.DataSourceRepository;
import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.application.port.ServiceCIRepository;
import com.zenandops.cmdb.application.port.ServiceDependencyRepository;
import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.entity.CIRelationship;
import com.zenandops.cmdb.domain.entity.DataSource;
import com.zenandops.cmdb.domain.entity.Organization;
import com.zenandops.cmdb.domain.entity.Service;
import com.zenandops.cmdb.domain.entity.ServiceCI;
import com.zenandops.cmdb.domain.entity.ServiceDependency;
import com.zenandops.cmdb.domain.vo.AssetStatus;
import com.zenandops.cmdb.domain.vo.AssetType;
import com.zenandops.cmdb.domain.vo.CIStatus;
import com.zenandops.cmdb.domain.vo.CIType;
import com.zenandops.cmdb.domain.vo.CostType;
import com.zenandops.cmdb.domain.vo.CriticalityLevel;
import com.zenandops.cmdb.domain.vo.DataSourceType;
import com.zenandops.cmdb.domain.vo.DependencyType;
import com.zenandops.cmdb.domain.vo.OrganizationType;
import com.zenandops.cmdb.domain.vo.RelationshipType;
import com.zenandops.cmdb.domain.vo.ServiceStatus;
import com.zenandops.cmdb.domain.vo.ServiceType;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Idempotent seed data service that populates MongoDB with sample CMDB data
 * on fresh deployments. Observes Quarkus StartupEvent to run before
 * the HTTP listener is ready.
 */
@ApplicationScoped
public class SeedDataService {

    private static final Logger LOG = Logger.getLogger(SeedDataService.class);

    private final OrganizationRepository organizationRepository;
    private final AssetRepository assetRepository;
    private final CIRepository ciRepository;
    private final ServiceRepository serviceRepository;
    private final CIRelationshipRepository ciRelationshipRepository;
    private final ServiceCIRepository serviceCIRepository;
    private final ServiceDependencyRepository serviceDependencyRepository;
    private final DataSourceRepository dataSourceRepository;

    /** Maps logical seed keys to MongoDB-generated ObjectId strings. */
    private final Map<String, String> idMap = new HashMap<>();

    @Inject
    public SeedDataService(OrganizationRepository organizationRepository,
                           AssetRepository assetRepository,
                           CIRepository ciRepository,
                           ServiceRepository serviceRepository,
                           CIRelationshipRepository ciRelationshipRepository,
                           ServiceCIRepository serviceCIRepository,
                           ServiceDependencyRepository serviceDependencyRepository,
                           DataSourceRepository dataSourceRepository) {
        this.organizationRepository = organizationRepository;
        this.assetRepository = assetRepository;
        this.ciRepository = ciRepository;
        this.serviceRepository = serviceRepository;
        this.ciRelationshipRepository = ciRelationshipRepository;
        this.serviceCIRepository = serviceCIRepository;
        this.serviceDependencyRepository = serviceDependencyRepository;
        this.dataSourceRepository = dataSourceRepository;
    }

    void onStart(@Observes StartupEvent event) {
        try {
            seed();
        } catch (Exception e) {
            LOG.error("Seed data routine failed. Continuing startup without terminating.", e);
        }
    }

    private void seed() {
        if (!organizationRepository.findAll().isEmpty()) {
            LOG.info("Organizations collection is not empty. Skipping seed data routine.");
            return;
        }

        LOG.info("CMDB collections are empty. Seeding sample data...");

        seedOrganizations();
        seedAssets();
        seedCIs();
        seedServices();
        seedCIRelationships();
        seedServiceCIs();
        seedServiceDependencies();
        seedDataSources();

        LOG.info("CMDB seed data routine completed successfully.");
    }

    /**
     * Resolves a logical key to its MongoDB-generated ObjectId string.
     * Returns null if the key is null.
     */
    private String resolve(String key) {
        if (key == null) {
            return null;
        }
        return idMap.get(key);
    }

    // =========================================================================
    // Organizations
    // =========================================================================

    private void seedOrganizations() {
        LOG.info("Seeding organizations...");

        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
        Instant threeMonthsAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        createOrganization("org-root", "ZenAndOps Corporation", OrganizationType.ROOT,
                null, "John Mitchell", "CC-0000", oneYearAgo);
        createOrganization("org-engineering", "Engineering", OrganizationType.BUSINESS_UNIT,
                "org-root", "Sarah Chen", "CC-1000", oneYearAgo);
        createOrganization("org-operations", "Operations", OrganizationType.BUSINESS_UNIT,
                "org-root", "Michael Torres", "CC-2000", oneYearAgo);
        createOrganization("org-finance", "Finance", OrganizationType.BUSINESS_UNIT,
                "org-root", "Emily Watson", "CC-3000", oneYearAgo);
        createOrganization("org-platform", "Platform Engineering", OrganizationType.DEPARTMENT,
                "org-engineering", "David Kim", "CC-1100", sixMonthsAgo);
        createOrganization("org-backend", "Backend Development", OrganizationType.DEPARTMENT,
                "org-engineering", "Ana Rodriguez", "CC-1200", sixMonthsAgo);
        createOrganization("org-frontend", "Frontend Development", OrganizationType.DEPARTMENT,
                "org-engineering", "James Park", "CC-1300", sixMonthsAgo);
        createOrganization("org-infra", "Infrastructure", OrganizationType.DEPARTMENT,
                "org-operations", "Carlos Mendez", "CC-2100", sixMonthsAgo);
        createOrganization("org-sre", "Site Reliability Engineering", OrganizationType.DEPARTMENT,
                "org-operations", "Lisa Nakamura", "CC-2200", sixMonthsAgo);
        createOrganization("org-team-alpha", "Team Alpha", OrganizationType.TEAM,
                "org-platform", "Kevin O'Brien", "CC-1101", threeMonthsAgo);
        createOrganization("org-team-beta", "Team Beta", OrganizationType.TEAM,
                "org-backend", "Maria Silva", "CC-1201", threeMonthsAgo);

        LOG.info("Organizations seeding completed.");
    }

    private void createOrganization(String key, String name, OrganizationType type,
                                    String parentKey, String responsiblePerson,
                                    String costCenter, Instant createdAt) {
        Organization org = new Organization();
        org.setName(name);
        org.setType(type);
        org.setParentId(resolve(parentKey));
        org.setResponsiblePerson(responsiblePerson);
        org.setCostCenter(costCenter);
        org.setCreatedAt(createdAt);
        org.setUpdatedAt(createdAt);
        organizationRepository.save(org);
        idMap.put(key, org.getId());
    }

    // =========================================================================
    // Assets
    // =========================================================================

    private void seedAssets() {
        LOG.info("Seeding assets...");

        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
        Instant threeMonthsAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        // Hardware
        createAsset("asset-srv-prod-01", "Production Server Cluster Node 01", AssetType.HARDWARE,
                "org-infra", new BigDecimal("45000.00"), CostType.CAPEX, oneYearAgo,
                AssetStatus.ACTIVE, "Dell Technologies", oneYearAgo);
        createAsset("asset-srv-prod-02", "Production Server Cluster Node 02", AssetType.HARDWARE,
                "org-infra", new BigDecimal("45000.00"), CostType.CAPEX, oneYearAgo,
                AssetStatus.ACTIVE, "Dell Technologies", oneYearAgo);
        createAsset("asset-srv-staging", "Staging Server", AssetType.HARDWARE,
                "org-infra", new BigDecimal("22000.00"), CostType.CAPEX, sixMonthsAgo,
                AssetStatus.ACTIVE, "HPE", sixMonthsAgo);
        createAsset("asset-network-fw", "Core Firewall Appliance", AssetType.HARDWARE,
                "org-infra", new BigDecimal("18500.00"), CostType.CAPEX, oneYearAgo,
                AssetStatus.ACTIVE, "Palo Alto Networks", oneYearAgo);
        createAsset("asset-network-switch", "Core Network Switch", AssetType.HARDWARE,
                "org-infra", new BigDecimal("12000.00"), CostType.CAPEX, oneYearAgo,
                AssetStatus.ACTIVE, "Cisco Systems", oneYearAgo);
        createAsset("asset-srv-legacy", "Legacy Application Server", AssetType.HARDWARE,
                "org-infra", new BigDecimal("15000.00"), CostType.CAPEX, oneYearAgo,
                AssetStatus.RETIRED, "Dell Technologies", oneYearAgo);

        // Software
        createAsset("asset-sw-jira", "Jira Software Cloud - Enterprise", AssetType.SOFTWARE,
                "org-engineering", new BigDecimal("2400.00"), CostType.OPEX, oneYearAgo,
                AssetStatus.ACTIVE, "Atlassian", oneYearAgo);
        createAsset("asset-sw-datadog", "Datadog Monitoring Platform", AssetType.SOFTWARE,
                "org-sre", new BigDecimal("5600.00"), CostType.OPEX, sixMonthsAgo,
                AssetStatus.ACTIVE, "Datadog Inc.", sixMonthsAgo);
        createAsset("asset-sw-github", "GitHub Enterprise", AssetType.SOFTWARE,
                "org-engineering", new BigDecimal("8400.00"), CostType.OPEX, oneYearAgo,
                AssetStatus.ACTIVE, "GitHub (Microsoft)", oneYearAgo);
        createAsset("asset-sw-oracle-db", "Oracle Database Enterprise License", AssetType.SOFTWARE,
                "org-platform", new BigDecimal("35000.00"), CostType.CAPEX, oneYearAgo,
                AssetStatus.ACTIVE, "Oracle Corporation", oneYearAgo);

        // Cloud
        createAsset("asset-cloud-aws-prod", "AWS Production Account", AssetType.CLOUD,
                "org-platform", new BigDecimal("12500.00"), CostType.OPEX, oneYearAgo,
                AssetStatus.ACTIVE, "Amazon Web Services", oneYearAgo);
        createAsset("asset-cloud-aws-staging", "AWS Staging Account", AssetType.CLOUD,
                "org-platform", new BigDecimal("3200.00"), CostType.OPEX, sixMonthsAgo,
                AssetStatus.ACTIVE, "Amazon Web Services", sixMonthsAgo);
        createAsset("asset-cloud-gcp-analytics", "GCP Analytics Project", AssetType.CLOUD,
                "org-platform", new BigDecimal("4800.00"), CostType.OPEX, threeMonthsAgo,
                AssetStatus.ACTIVE, "Google Cloud Platform", threeMonthsAgo);
        createAsset("asset-cloud-azure-backup", "Azure Backup & DR", AssetType.CLOUD,
                "org-infra", new BigDecimal("1800.00"), CostType.OPEX, sixMonthsAgo,
                AssetStatus.ACTIVE, "Microsoft Azure", sixMonthsAgo);

        LOG.info("Assets seeding completed.");
    }

    private void createAsset(String key, String name, AssetType type, String organizationKey,
                             BigDecimal cost, CostType costType, Instant acquisitionDate,
                             AssetStatus status, String supplier, Instant createdAt) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(type);
        asset.setOrganizationId(resolve(organizationKey));
        asset.setCost(cost);
        asset.setCostType(costType);
        asset.setAcquisitionDate(acquisitionDate);
        asset.setStatus(status);
        asset.setSupplier(supplier);
        asset.setCreatedAt(createdAt);
        asset.setUpdatedAt(createdAt);
        assetRepository.save(asset);
        idMap.put(key, asset.getId());
    }

    // =========================================================================
    // Configuration Items
    // =========================================================================

    private void seedCIs() {
        LOG.info("Seeding configuration items...");

        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
        Instant threeMonthsAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        // VMs
        createCI("ci-vm-api-01", "api-gateway-prod-01", CIType.VM,
                "org-platform", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, sixMonthsAgo);
        createCI("ci-vm-api-02", "api-gateway-prod-02", CIType.VM,
                "org-platform", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, sixMonthsAgo);
        createCI("ci-vm-app-01", "app-server-prod-01", CIType.VM,
                "org-backend", "asset-srv-prod-01", CIStatus.ACTIVE, false, oneYearAgo);
        createCI("ci-vm-app-02", "app-server-prod-02", CIType.VM,
                "org-backend", "asset-srv-prod-02", CIStatus.ACTIVE, false, oneYearAgo);
        createCI("ci-vm-staging", "app-server-staging-01", CIType.VM,
                "org-backend", "asset-srv-staging", CIStatus.ACTIVE, false, sixMonthsAgo);
        createCI("ci-vm-legacy", "legacy-monolith-01", CIType.VM,
                "org-backend", "asset-srv-legacy", CIStatus.INACTIVE, true, oneYearAgo);

        // Databases
        createCI("ci-db-postgres-prod", "postgres-primary-prod", CIType.DATABASE,
                "org-platform", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, oneYearAgo);
        createCI("ci-db-postgres-replica", "postgres-replica-prod", CIType.DATABASE,
                "org-platform", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, oneYearAgo);
        createCI("ci-db-mongo-prod", "mongodb-cluster-prod", CIType.DATABASE,
                "org-platform", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, sixMonthsAgo);
        createCI("ci-db-redis-prod", "redis-cache-prod", CIType.DATABASE,
                "org-platform", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, sixMonthsAgo);
        createCI("ci-db-oracle-legacy", "oracle-erp-database", CIType.DATABASE,
                "org-platform", "asset-sw-oracle-db", CIStatus.ACTIVE, true, oneYearAgo);

        // APIs
        createCI("ci-api-auth", "keycloak-auth-api", CIType.API,
                "org-platform", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, sixMonthsAgo);
        createCI("ci-api-cmdb", "cmdb-service-api", CIType.API,
                "org-platform", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, sixMonthsAgo);
        createCI("ci-api-incident", "incident-service-api", CIType.API,
                "org-platform", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, threeMonthsAgo);
        createCI("ci-api-notification", "notification-service-api", CIType.API,
                "org-backend", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, threeMonthsAgo);
        createCI("ci-api-payment", "payment-gateway-api", CIType.API,
                "org-backend", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, sixMonthsAgo);

        // Storage
        createCI("ci-storage-s3-prod", "s3-documents-prod", CIType.STORAGE,
                "org-platform", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, oneYearAgo);
        createCI("ci-storage-s3-backup", "s3-backup-vault", CIType.STORAGE,
                "org-infra", "asset-cloud-azure-backup", CIStatus.ACTIVE, false, sixMonthsAgo);

        // Network
        createCI("ci-net-lb-prod", "load-balancer-prod", CIType.NETWORK,
                "org-infra", "asset-cloud-aws-prod", CIStatus.ACTIVE, false, oneYearAgo);
        createCI("ci-net-firewall", "perimeter-firewall", CIType.NETWORK,
                "org-infra", "asset-network-fw", CIStatus.ACTIVE, false, oneYearAgo);
        createCI("ci-net-vpn", "corporate-vpn-gateway", CIType.NETWORK,
                "org-infra", "asset-network-switch", CIStatus.ACTIVE, false, oneYearAgo);

        LOG.info("Configuration items seeding completed.");
    }

    private void createCI(String key, String name, CIType type, String organizationKey,
                          String assetKey, CIStatus status, boolean controlledExceptionFlag,
                          Instant createdAt) {
        CI ci = new CI();
        ci.setName(name);
        ci.setType(type);
        ci.setOrganizationId(resolve(organizationKey));
        ci.setAssetId(resolve(assetKey));
        ci.setStatus(status);
        ci.setControlledExceptionFlag(controlledExceptionFlag);
        ci.setCreatedAt(createdAt);
        ci.setUpdatedAt(createdAt);
        ciRepository.save(ci);
        idMap.put(key, ci.getId());
    }

    // =========================================================================
    // Services
    // =========================================================================

    private void seedServices() {
        LOG.info("Seeding services...");

        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
        Instant threeMonthsAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        // Domain services
        createService("svc-domain-itsm", "IT Service Management",
                "Core ITSM platform providing incident, problem, and change management capabilities",
                ServiceType.DOMAIN, null, "org-engineering",
                "John Mitchell", "Sarah Chen", CriticalityLevel.CRITICAL,
                ServiceStatus.ACTIVE, oneYearAgo);
        createService("svc-domain-platform", "Platform Services",
                "Shared platform infrastructure supporting all business applications",
                ServiceType.DOMAIN, null, "org-platform",
                "John Mitchell", "David Kim", CriticalityLevel.CRITICAL,
                ServiceStatus.ACTIVE, oneYearAgo);
        createService("svc-domain-analytics", "Analytics & Reporting",
                "Business intelligence and operational analytics platform",
                ServiceType.DOMAIN, null, "org-engineering",
                "Emily Watson", "Ana Rodriguez", CriticalityLevel.HIGH,
                ServiceStatus.ACTIVE, sixMonthsAgo);

        // Business services
        createService("svc-biz-incident-mgmt", "Incident Management",
                "End-to-end incident lifecycle management from detection to resolution",
                ServiceType.BUSINESS, "svc-domain-itsm", "org-backend",
                "Michael Torres", "Ana Rodriguez", CriticalityLevel.CRITICAL,
                ServiceStatus.ACTIVE, oneYearAgo);
        createService("svc-biz-cmdb", "Configuration Management",
                "CMDB service for tracking assets, CIs, and their relationships",
                ServiceType.BUSINESS, "svc-domain-itsm", "org-platform",
                "Michael Torres", "David Kim", CriticalityLevel.HIGH,
                ServiceStatus.ACTIVE, sixMonthsAgo);
        createService("svc-biz-change-mgmt", "Change Management",
                "Change request workflow and approval management",
                ServiceType.BUSINESS, "svc-domain-itsm", "org-backend",
                "Michael Torres", "Maria Silva", CriticalityLevel.HIGH,
                ServiceStatus.ACTIVE, threeMonthsAgo);
        createService("svc-biz-notifications", "Notification Service",
                "Multi-channel notification delivery (email, SMS, push, webhook)",
                ServiceType.BUSINESS, "svc-domain-platform", "org-backend",
                "Sarah Chen", "James Park", CriticalityLevel.MEDIUM,
                ServiceStatus.ACTIVE, threeMonthsAgo);

        // Technical services
        createService("svc-tech-auth", "Authentication & Authorization",
                "JWT-based authentication, RBAC, and SSO integration",
                ServiceType.TECHNICAL, "svc-domain-platform", "org-platform",
                "David Kim", "Kevin O'Brien", CriticalityLevel.CRITICAL,
                ServiceStatus.ACTIVE, oneYearAgo);
        createService("svc-tech-messaging", "Event Messaging",
                "Kafka-based event streaming and async communication backbone",
                ServiceType.TECHNICAL, "svc-domain-platform", "org-platform",
                "David Kim", "Kevin O'Brien", CriticalityLevel.HIGH,
                ServiceStatus.ACTIVE, sixMonthsAgo);
        createService("svc-tech-monitoring", "Observability Platform",
                "Centralized monitoring, logging, and tracing infrastructure",
                ServiceType.TECHNICAL, "svc-domain-platform", "org-sre",
                "Lisa Nakamura", "Lisa Nakamura", CriticalityLevel.HIGH,
                ServiceStatus.ACTIVE, sixMonthsAgo);
        createService("svc-tech-backup", "Backup & Disaster Recovery",
                "Automated backup, replication, and disaster recovery procedures",
                ServiceType.TECHNICAL, "svc-domain-platform", "org-infra",
                "Carlos Mendez", "Carlos Mendez", CriticalityLevel.CRITICAL,
                ServiceStatus.ACTIVE, sixMonthsAgo);
        createService("svc-tech-legacy-erp", "Legacy ERP Integration",
                "Integration layer for the legacy Oracle ERP system",
                ServiceType.TECHNICAL, "svc-domain-platform", "org-backend",
                "Emily Watson", "Maria Silva", CriticalityLevel.MEDIUM,
                ServiceStatus.DEPRECATED, oneYearAgo);

        LOG.info("Services seeding completed.");
    }

    private void createService(String key, String name, String description, ServiceType type,
                               String parentKey, String organizationKey, String businessOwner,
                               String technicalOwner, CriticalityLevel criticality,
                               ServiceStatus status, Instant createdAt) {
        Service service = new Service();
        service.setName(name);
        service.setDescription(description);
        service.setType(type);
        service.setParentId(resolve(parentKey));
        service.setOrganizationId(resolve(organizationKey));
        service.setBusinessOwner(businessOwner);
        service.setTechnicalOwner(technicalOwner);
        service.setCriticality(criticality);
        service.setStatus(status);
        service.setCreatedAt(createdAt);
        service.setUpdatedAt(createdAt);
        serviceRepository.save(service);
        idMap.put(key, service.getId());
    }

    // =========================================================================
    // CI Relationships
    // =========================================================================

    private void seedCIRelationships() {
        LOG.info("Seeding CI relationships...");

        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
        Instant threeMonthsAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        // Load balancer routes to API gateways
        createCIRelationship("ci-net-lb-prod", "ci-vm-api-01",
                RelationshipType.CONNECTS_TO, sixMonthsAgo);
        createCIRelationship("ci-net-lb-prod", "ci-vm-api-02",
                RelationshipType.CONNECTS_TO, sixMonthsAgo);

        // API gateways connect to app servers
        createCIRelationship("ci-vm-api-01", "ci-vm-app-01",
                RelationshipType.CONNECTS_TO, sixMonthsAgo);
        createCIRelationship("ci-vm-api-02", "ci-vm-app-02",
                RelationshipType.CONNECTS_TO, sixMonthsAgo);

        // App servers depend on databases
        createCIRelationship("ci-vm-app-01", "ci-db-postgres-prod",
                RelationshipType.DEPENDS_ON, oneYearAgo);
        createCIRelationship("ci-vm-app-02", "ci-db-postgres-prod",
                RelationshipType.DEPENDS_ON, oneYearAgo);
        createCIRelationship("ci-vm-app-01", "ci-db-mongo-prod",
                RelationshipType.DEPENDS_ON, sixMonthsAgo);
        createCIRelationship("ci-vm-app-01", "ci-db-redis-prod",
                RelationshipType.DEPENDS_ON, sixMonthsAgo);

        // Postgres primary hosts replica
        createCIRelationship("ci-db-postgres-prod", "ci-db-postgres-replica",
                RelationshipType.HOSTS, oneYearAgo);

        // APIs depend on app servers
        createCIRelationship("ci-api-auth", "ci-vm-app-01",
                RelationshipType.DEPENDS_ON, sixMonthsAgo);
        createCIRelationship("ci-api-cmdb", "ci-vm-app-01",
                RelationshipType.DEPENDS_ON, sixMonthsAgo);
        createCIRelationship("ci-api-incident", "ci-vm-app-02",
                RelationshipType.DEPENDS_ON, threeMonthsAgo);
        createCIRelationship("ci-api-notification", "ci-vm-app-02",
                RelationshipType.DEPENDS_ON, threeMonthsAgo);

        // Firewall and VPN
        createCIRelationship("ci-net-firewall", "ci-net-lb-prod",
                RelationshipType.CONNECTS_TO, oneYearAgo);
        createCIRelationship("ci-net-vpn", "ci-net-firewall",
                RelationshipType.CONNECTS_TO, oneYearAgo);

        // Storage dependencies
        createCIRelationship("ci-vm-app-01", "ci-storage-s3-prod",
                RelationshipType.DEPENDS_ON, oneYearAgo);
        createCIRelationship("ci-db-postgres-prod", "ci-storage-s3-backup",
                RelationshipType.CONNECTS_TO, sixMonthsAgo);

        // Legacy
        createCIRelationship("ci-vm-legacy", "ci-db-oracle-legacy",
                RelationshipType.DEPENDS_ON, oneYearAgo);

        // Payment depends on auth
        createCIRelationship("ci-api-payment", "ci-api-auth",
                RelationshipType.DEPENDS_ON, sixMonthsAgo);

        LOG.info("CI relationships seeding completed.");
    }

    private void createCIRelationship(String sourceCIKey, String targetCIKey,
                                      RelationshipType type, Instant createdAt) {
        String sourceId = resolve(sourceCIKey);
        String targetId = resolve(targetCIKey);
        if (ciRelationshipRepository.existsBySourceCIIdAndTargetCIIdAndRelationshipType(
                sourceId, targetId, type)) {
            return;
        }
        CIRelationship rel = new CIRelationship();
        rel.setSourceCIId(sourceId);
        rel.setTargetCIId(targetId);
        rel.setRelationshipType(type);
        rel.setCreatedAt(createdAt);
        ciRelationshipRepository.save(rel);
    }

    // =========================================================================
    // Service-CI Associations
    // =========================================================================

    private void seedServiceCIs() {
        LOG.info("Seeding service-CI associations...");

        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
        Instant threeMonthsAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        // Authentication service
        createServiceCI("svc-tech-auth", "ci-api-auth", sixMonthsAgo);
        createServiceCI("svc-tech-auth", "ci-vm-app-01", sixMonthsAgo);
        createServiceCI("svc-tech-auth", "ci-db-postgres-prod", sixMonthsAgo);
        createServiceCI("svc-tech-auth", "ci-db-redis-prod", sixMonthsAgo);

        // CMDB service
        createServiceCI("svc-biz-cmdb", "ci-api-cmdb", sixMonthsAgo);
        createServiceCI("svc-biz-cmdb", "ci-vm-app-01", sixMonthsAgo);
        createServiceCI("svc-biz-cmdb", "ci-db-mongo-prod", sixMonthsAgo);

        // Incident management
        createServiceCI("svc-biz-incident-mgmt", "ci-api-incident", threeMonthsAgo);
        createServiceCI("svc-biz-incident-mgmt", "ci-vm-app-02", threeMonthsAgo);
        createServiceCI("svc-biz-incident-mgmt", "ci-db-postgres-prod", threeMonthsAgo);

        // Notification service
        createServiceCI("svc-biz-notifications", "ci-api-notification", threeMonthsAgo);
        createServiceCI("svc-biz-notifications", "ci-vm-app-02", threeMonthsAgo);

        // Messaging service
        createServiceCI("svc-tech-messaging", "ci-vm-app-01", sixMonthsAgo);
        createServiceCI("svc-tech-messaging", "ci-vm-app-02", sixMonthsAgo);

        // Monitoring service
        createServiceCI("svc-tech-monitoring", "ci-vm-app-01", sixMonthsAgo);
        createServiceCI("svc-tech-monitoring", "ci-vm-app-02", sixMonthsAgo);

        // Backup service
        createServiceCI("svc-tech-backup", "ci-storage-s3-backup", sixMonthsAgo);
        createServiceCI("svc-tech-backup", "ci-db-postgres-prod", sixMonthsAgo);
        createServiceCI("svc-tech-backup", "ci-db-mongo-prod", sixMonthsAgo);

        // Legacy ERP
        createServiceCI("svc-tech-legacy-erp", "ci-vm-legacy", oneYearAgo);
        createServiceCI("svc-tech-legacy-erp", "ci-db-oracle-legacy", oneYearAgo);

        // Platform shared infra
        createServiceCI("svc-domain-platform", "ci-net-lb-prod", oneYearAgo);
        createServiceCI("svc-domain-platform", "ci-net-firewall", oneYearAgo);
        createServiceCI("svc-domain-platform", "ci-net-vpn", oneYearAgo);
        createServiceCI("svc-domain-platform", "ci-storage-s3-prod", oneYearAgo);

        LOG.info("Service-CI associations seeding completed.");
    }

    private void createServiceCI(String serviceKey, String ciKey, Instant createdAt) {
        String serviceId = resolve(serviceKey);
        String ciId = resolve(ciKey);
        if (serviceCIRepository.existsByServiceIdAndCiId(serviceId, ciId)) {
            return;
        }
        ServiceCI serviceCI = new ServiceCI();
        serviceCI.setServiceId(serviceId);
        serviceCI.setCiId(ciId);
        serviceCI.setCreatedAt(createdAt);
        serviceCIRepository.save(serviceCI);
    }

    // =========================================================================
    // Service Dependencies
    // =========================================================================

    private void seedServiceDependencies() {
        LOG.info("Seeding service dependencies...");

        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
        Instant threeMonthsAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        // Incident Management dependencies
        createServiceDependency("svc-biz-incident-mgmt", "svc-tech-auth",
                DependencyType.SYNCHRONOUS, threeMonthsAgo);
        createServiceDependency("svc-biz-incident-mgmt", "svc-tech-messaging",
                DependencyType.ASYNCHRONOUS, threeMonthsAgo);
        createServiceDependency("svc-biz-incident-mgmt", "svc-biz-notifications",
                DependencyType.ASYNCHRONOUS, threeMonthsAgo);

        // CMDB dependencies
        createServiceDependency("svc-biz-cmdb", "svc-tech-auth",
                DependencyType.SYNCHRONOUS, sixMonthsAgo);
        createServiceDependency("svc-biz-cmdb", "svc-tech-messaging",
                DependencyType.ASYNCHRONOUS, sixMonthsAgo);

        // Change Management dependencies
        createServiceDependency("svc-biz-change-mgmt", "svc-tech-auth",
                DependencyType.SYNCHRONOUS, threeMonthsAgo);
        createServiceDependency("svc-biz-change-mgmt", "svc-biz-cmdb",
                DependencyType.SYNCHRONOUS, threeMonthsAgo);
        createServiceDependency("svc-biz-change-mgmt", "svc-biz-notifications",
                DependencyType.ASYNCHRONOUS, threeMonthsAgo);

        // Notifications dependencies
        createServiceDependency("svc-biz-notifications", "svc-tech-auth",
                DependencyType.SYNCHRONOUS, threeMonthsAgo);
        createServiceDependency("svc-biz-notifications", "svc-tech-messaging",
                DependencyType.CRITICAL, threeMonthsAgo);

        // Monitoring dependencies
        createServiceDependency("svc-tech-monitoring", "svc-tech-messaging",
                DependencyType.CRITICAL, sixMonthsAgo);

        // Backup dependencies
        createServiceDependency("svc-tech-backup", "svc-tech-monitoring",
                DependencyType.ASYNCHRONOUS, sixMonthsAgo);

        // Legacy ERP dependencies
        createServiceDependency("svc-tech-legacy-erp", "svc-tech-auth",
                DependencyType.SYNCHRONOUS, oneYearAgo);

        LOG.info("Service dependencies seeding completed.");
    }

    private void createServiceDependency(String sourceServiceKey, String targetServiceKey,
                                         DependencyType type, Instant createdAt) {
        String sourceId = resolve(sourceServiceKey);
        String targetId = resolve(targetServiceKey);
        if (serviceDependencyRepository.existsBySourceServiceIdAndTargetServiceId(
                sourceId, targetId)) {
            return;
        }
        ServiceDependency dep = new ServiceDependency();
        dep.setSourceServiceId(sourceId);
        dep.setTargetServiceId(targetId);
        dep.setDependencyType(type);
        dep.setCreatedAt(createdAt);
        serviceDependencyRepository.save(dep);
    }

    // =========================================================================
    // Data Sources
    // =========================================================================

    private void seedDataSources() {
        LOG.info("Seeding data sources...");

        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
        Instant threeMonthsAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        createDataSource("ds-manual-portal", "CMDB Admin Portal", DataSourceType.API, 5, oneYearAgo);
        createDataSource("ds-aws-discovery", "AWS Cloud Discovery Agent", DataSourceType.AGENT, 4, sixMonthsAgo);
        createDataSource("ds-network-scanner", "Network Topology Scanner", DataSourceType.AGENT, 3, sixMonthsAgo);
        createDataSource("ds-csv-import", "Legacy CSV Import", DataSourceType.FILE, 2, oneYearAgo);
        createDataSource("ds-servicenow-sync", "ServiceNow Migration Sync", DataSourceType.API, 4, threeMonthsAgo);

        LOG.info("Data sources seeding completed.");
    }

    private void createDataSource(String key, String name, DataSourceType type,
                                  int reliabilityRating, Instant createdAt) {
        DataSource ds = new DataSource();
        ds.setName(name);
        ds.setType(type);
        ds.setReliabilityRating(reliabilityRating);
        ds.setCreatedAt(createdAt);
        ds.setUpdatedAt(createdAt);
        dataSourceRepository.save(ds);
        idMap.put(key, ds.getId());
    }
}
