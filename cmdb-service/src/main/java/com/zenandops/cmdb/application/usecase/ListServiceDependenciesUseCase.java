package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.ServiceDependencyRepository;
import com.zenandops.cmdb.domain.entity.ServiceDependency;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Use case for listing all dependencies for a given service.
 * Returns both upstream (where service is target) and downstream (where service is source)
 * dependencies, combining results from findBySourceServiceId and findByTargetServiceId.
 */
@ApplicationScoped
public class ListServiceDependenciesUseCase {

    private final ServiceDependencyRepository serviceDependencyRepository;

    @Inject
    public ListServiceDependenciesUseCase(ServiceDependencyRepository serviceDependencyRepository) {
        this.serviceDependencyRepository = serviceDependencyRepository;
    }

    /**
     * List all dependencies for a service (both upstream and downstream).
     *
     * @param serviceId the service identifier
     * @return combined list of upstream and downstream dependencies
     */
    public List<ServiceDependency> execute(String serviceId) {
        List<ServiceDependency> downstream = serviceDependencyRepository
                .findBySourceServiceId(serviceId);
        List<ServiceDependency> upstream = serviceDependencyRepository
                .findByTargetServiceId(serviceId);

        Set<String> seenIds = new LinkedHashSet<>();
        List<ServiceDependency> combined = new ArrayList<>();

        for (ServiceDependency dep : downstream) {
            if (seenIds.add(dep.getId())) {
                combined.add(dep);
            }
        }
        for (ServiceDependency dep : upstream) {
            if (seenIds.add(dep.getId())) {
                combined.add(dep);
            }
        }

        return combined;
    }
}
