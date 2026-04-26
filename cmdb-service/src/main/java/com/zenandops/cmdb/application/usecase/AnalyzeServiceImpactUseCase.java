package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.AffectedEntity;
import com.zenandops.cmdb.application.dto.ImpactAnalysisResult;
import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.application.port.ServiceCIRepository;
import com.zenandops.cmdb.application.port.ServiceDependencyRepository;
import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.entity.Service;
import com.zenandops.cmdb.domain.entity.ServiceCI;
import com.zenandops.cmdb.domain.entity.ServiceDependency;
import com.zenandops.cmdb.domain.exception.ServiceNotFoundException;
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
 * Use case for analyzing the impact of a Service change or failure.
 * BFS traversal of service dependencies (downstream) and associated CIs via ServiceCI.
 * Configurable max depth, circular dependency detection.
 */
@ApplicationScoped
public class AnalyzeServiceImpactUseCase {

    private final ServiceRepository serviceRepository;
    private final ServiceDependencyRepository serviceDependencyRepository;
    private final ServiceCIRepository serviceCIRepository;
    private final CIRepository ciRepository;
    private final int maxDepth;

    @Inject
    public AnalyzeServiceImpactUseCase(ServiceRepository serviceRepository,
                                       ServiceDependencyRepository serviceDependencyRepository,
                                       ServiceCIRepository serviceCIRepository,
                                       CIRepository ciRepository,
                                       @ConfigProperty(name = "cmdb.impact-analysis.max-depth",
                                               defaultValue = "10") int maxDepth) {
        this.serviceRepository = serviceRepository;
        this.serviceDependencyRepository = serviceDependencyRepository;
        this.serviceCIRepository = serviceCIRepository;
        this.ciRepository = ciRepository;
        this.maxDepth = maxDepth;
    }

    /**
     * Analyze the impact of a Service change or failure.
     *
     * @param serviceId the Service identifier to analyze
     * @return the impact analysis result
     * @throws ServiceNotFoundException if the Service does not exist
     */
    public ImpactAnalysisResult execute(String serviceId) {
        Service rootService = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(
                        "Service not found with id: " + serviceId));

        List<AffectedEntity> affectedEntities = new ArrayList<>();
        Set<String> visitedServices = new HashSet<>();
        Set<String> visitedCIs = new HashSet<>();
        List<String> circularWarnings = new ArrayList<>();
        boolean[] maxDepthReached = {false};

        visitedServices.add(serviceId);

        // BFS traversal of service dependencies
        Queue<BfsNode> queue = new LinkedList<>();
        queue.add(new BfsNode(serviceId, "SERVICE", List.of(), 0));

        while (!queue.isEmpty()) {
            BfsNode current = queue.poll();

            if (current.depth >= maxDepth) {
                maxDepthReached[0] = true;
                continue;
            }

            if ("SERVICE".equals(current.entityType)) {
                // Traverse downstream service dependencies (where this service is the source)
                List<ServiceDependency> downstream =
                        serviceDependencyRepository.findBySourceServiceId(current.entityId);
                for (ServiceDependency dep : downstream) {
                    String targetId = dep.getTargetServiceId();
                    List<String> path = new ArrayList<>(current.path);
                    path.add(dep.getDependencyType().name());

                    if (visitedServices.contains(targetId)) {
                        circularWarnings.add("Circular dependency detected: "
                                + current.entityId + " -> " + targetId);
                        continue;
                    }

                    visitedServices.add(targetId);
                    Optional<Service> targetService = serviceRepository.findById(targetId);
                    if (targetService.isPresent()) {
                        int depth = current.depth + 1;
                        affectedEntities.add(new AffectedEntity(
                                targetId, targetService.get().getName(), "SERVICE", path, depth));
                        queue.add(new BfsNode(targetId, "SERVICE", path, depth));
                    }
                }

                // Traverse associated CIs via ServiceCI
                List<ServiceCI> ciAssociations =
                        serviceCIRepository.findByServiceId(current.entityId);
                for (ServiceCI sci : ciAssociations) {
                    String ciId = sci.getCiId();
                    if (visitedCIs.contains(ciId)) {
                        continue;
                    }

                    visitedCIs.add(ciId);
                    Optional<CI> ci = ciRepository.findById(ciId);
                    if (ci.isPresent()) {
                        List<String> path = new ArrayList<>(current.path);
                        path.add("SUPPORTS");
                        int depth = current.depth + 1;
                        affectedEntities.add(new AffectedEntity(
                                ciId, ci.get().getName(), "CI", path, depth));
                    }
                }
            }
        }

        int totalAffectedServices = (int) affectedEntities.stream()
                .filter(e -> "SERVICE".equals(e.entityType())).count();
        int totalAffectedCIs = (int) affectedEntities.stream()
                .filter(e -> "CI".equals(e.entityType())).count();

        return new ImpactAnalysisResult(
                rootService.getId(), rootService.getName(), "SERVICE",
                affectedEntities, totalAffectedServices, totalAffectedCIs,
                circularWarnings, maxDepthReached[0]);
    }

    private record BfsNode(String entityId, String entityType, List<String> path, int depth) {
    }
}
