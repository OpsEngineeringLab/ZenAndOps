package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.ServiceDependencyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for deleting a ServiceDependency.
 * Validates the dependency exists before deletion.
 */
@ApplicationScoped
public class DeleteServiceDependencyUseCase {

    private final ServiceDependencyRepository serviceDependencyRepository;

    @Inject
    public DeleteServiceDependencyUseCase(ServiceDependencyRepository serviceDependencyRepository) {
        this.serviceDependencyRepository = serviceDependencyRepository;
    }

    /**
     * Delete a service dependency by id.
     *
     * @param id the dependency identifier
     * @throws IllegalArgumentException if no dependency exists with the given id
     */
    public void execute(String id) {
        serviceDependencyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Service dependency not found with id: " + id));

        serviceDependencyRepository.deleteById(id);
    }
}
