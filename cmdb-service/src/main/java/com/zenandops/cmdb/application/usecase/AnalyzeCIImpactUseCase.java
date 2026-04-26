package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.AffectedEntity;
import com.zenandops.cmdb.application.dto.ImpactAnalysisResult;
import com.zenandops.cmdb.application.port.CIRelationshipRepository;
import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.application.port.ServiceCIRepository;
import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.entity.CIRelationship;
import com.zenandops.cmdb.domain.entity.Service;
import com.zenandops.cmdb.domain.entity.ServiceCI;
import com.zenandops.cmdb.domain.exception.CINotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Use case for analyzing the impact of a CI change or failure.
 * BFS traversal of CI relationships (downstream from source) and ServiceCI associations.
 * Configurable max depth, circular dependency detection.
 */
@ApplicationScoped
public class AnalyzeCIImpactUseCase {

    private final CIRepository ciRepository;
    private final CIRelationshipRepository ciRelationshipRepository;
    private final ServiceCIRepository serviceCIRepository;
    private final ServiceRepository serviceRepository;
    private final int maxDepth;

    @Inject
    public AnalyzeCIImpactUseCase(CIRepository ciRepository,
                                  CIRelationshipRepository ciRelationshipRepository,
                                  ServiceCIRepository serviceCIRepository,
                                  ServiceRepository serviceRepository,
                                  @ConfigProperty(name = "cmdb.impact-analysis.max-depth",
                                          defaultValue = "10") int maxDepth) {
        this.ciRepository = ciRepository;
        this.ciRelationshipRepository = ciRelationshipRepository;
        this.serviceCIRepository = serviceCIRepository;
        this.serviceRepository = serviceRepository;
        this.maxDepth = maxDepth;
    }

    /**
     * Analyze the impact of a CI change or failure.
     *
     * @param ciId the CI identifier to analyze
     * @return the impact analysis result
     * @throws CINotFoundException if the CI does not exist
     */
    public ImpactAnalysisResult execute(String ciId) {
        CI rootCI = ciRepository.findById(ciId)
                .orElseThrow(() -> new CINotFoundException(
                        "CI not found with id: " + ciId));

        List<AffectedEntity> affectedEntities = new ArrayList<>();
        Set<String> visitedCIs = new HashSet<>();
        Set<String> visitedServices = new HashSet<>();
        List<String> circularWarnings = new ArrayList<>();
        boolean[] maxDepthReached = {false};

        visitedCIs.add(ciId);

        // BFS traversal of CI relationships
        Queue<BfsNode> queue = new LinkedList<>();
        queue.add(new BfsNode(ciId, "CI", List.of(), 0));

        while (!queue.isEmpty()) {
            BfsNode current = queue.poll();

            if (current.depth >= maxDepth) {
                maxDepthReached[0] = true;
                continue;
            }

            if ("CI".equals(current.entityType)) {
                // Traverse downstream CI relationships (where this CI is the source)
                List<CIRelationship> downstream =
                        ciRelationshipRepository.findBySourceCIId(current.entityId);
                for (CIRelationship rel : downstream) {
                    String targetId = rel.getTargetCIId();
                    List<String> path = new ArrayList<>(current.path);
                    path.add(rel.getRelationshipType().name());

                    if (visitedCIs.contains(targetId)) {
                        circularWarnings.add("Circular dependency detected: "
                                + current.entityId + " -> " + targetId);
                        continue;
                    }

                    visitedCIs.add(targetId);
                    Optional<CI> targetCI = ciRepository.findById(targetId);
                    if (targetCI.isPresent()) {
                        int depth = current.depth + 1;
                        affectedEntities.add(new AffectedEntity(
                                targetId, targetCI.get().getName(), "CI", path, depth));
                        queue.add(new BfsNode(targetId, "CI", path, depth));
                    }
                }

                // Traverse ServiceCI associations for this CI
                List<ServiceCI> serviceAssociations =
                        serviceCIRepository.findByCiId(current.entityId);
                for (ServiceCI sci : serviceAssociations) {
                    String serviceId = sci.getServiceId();
                    if (visitedServices.contains(serviceId)) {
                        continue;
                    }

                    visitedServices.add(serviceId);
                    Optional<Service> service = serviceRepository.findById(serviceId);
                    if (service.isPresent()) {
                        List<String> path = new ArrayList<>(current.path);
                        path.add("SUPPORTS");
                        int depth = current.depth + 1;
                        affectedEntities.add(new AffectedEntity(
                                serviceId, service.get().getName(), "SERVICE", path, depth));
                    }
                }
            }
        }

        int totalAffectedCIs = (int) affectedEntities.stream()
                .filter(e -> "CI".equals(e.entityType())).count();
        int totalAffectedServices = (int) affectedEntities.stream()
                .filter(e -> "SERVICE".equals(e.entityType())).count();

        return new ImpactAnalysisResult(
                rootCI.getId(), rootCI.getName(), "CI",
                affectedEntities, totalAffectedServices, totalAffectedCIs,
                circularWarnings, maxDepthReached[0]);
    }

    private record BfsNode(String entityId, String entityType, List<String> path, int depth) {
    }
}
