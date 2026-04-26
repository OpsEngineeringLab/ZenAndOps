package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.ServiceDependencyRepository;
import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.entity.ServiceDependency;
import com.zenandops.cmdb.domain.exception.DuplicateDependencyException;
import com.zenandops.cmdb.domain.exception.SelfReferenceException;
import com.zenandops.cmdb.domain.exception.ServiceNotFoundException;
import com.zenandops.cmdb.domain.vo.DependencyType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case for creating a new ServiceDependency.
 * Validates both source and target services exist, prevents self-reference
 * and duplicates, and logs a warning for CRITICAL dependencies.
 */
@ApplicationScoped
public class CreateServiceDependencyUseCase {

    private static final Logger LOG = Logger.getLogger(CreateServiceDependencyUseCase.class);

    private final ServiceRepository serviceRepository;
    private final ServiceDependencyRepository serviceDependencyRepository;

    @Inject
    public CreateServiceDependencyUseCase(ServiceRepository serviceRepository,
                                          ServiceDependencyRepository serviceDependencyRepository) {
        this.serviceRepository = serviceRepository;
        this.serviceDependencyRepository = serviceDependencyRepository;
    }

    /**
     * Create a new service dependency.
     *
     * @param sourceServiceId the source service identifier
     * @param targetServiceId the target service identifier
     * @param dependencyType  the dependency type (SYNCHRONOUS, ASYNCHRONOUS, CRITICAL)
     * @return the created ServiceDependency
     */
    public ServiceDependency execute(String sourceServiceId, String targetServiceId,
                                     DependencyType dependencyType) {
        if (sourceServiceId.equals(targetServiceId)) {
            throw new SelfReferenceException(
                    "Source and target service cannot be the same: " + sourceServiceId);
        }

        if (!serviceRepository.existsById(sourceServiceId)) {
            throw new ServiceNotFoundException(
                    "Source service not found with id: " + sourceServiceId);
        }

        if (!serviceRepository.existsById(targetServiceId)) {
            throw new ServiceNotFoundException(
                    "Target service not found with id: " + targetServiceId);
        }

        if (serviceDependencyRepository.existsBySourceServiceIdAndTargetServiceId(
                sourceServiceId, targetServiceId)) {
            throw new DuplicateDependencyException(
                    "Dependency already exists between source " + sourceServiceId
                            + " and target " + targetServiceId);
        }

        if (dependencyType == DependencyType.CRITICAL) {
            LOG.warnf("Critical dependency established from service %s to service %s",
                    sourceServiceId, targetServiceId);
        }

        ServiceDependency dependency = new ServiceDependency();
        dependency.setId(UUID.randomUUID().toString());
        dependency.setSourceServiceId(sourceServiceId);
        dependency.setTargetServiceId(targetServiceId);
        dependency.setDependencyType(dependencyType);
        dependency.setCreatedAt(Instant.now());

        serviceDependencyRepository.save(dependency);
        return dependency;
    }
}
