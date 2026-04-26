package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.ServiceCIRepository;
import com.zenandops.cmdb.application.port.ServiceDependencyRepository;
import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.exception.ServiceInUseException;
import com.zenandops.cmdb.domain.exception.ServiceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for deleting a Service.
 * Validates no children, dependencies, or CI associations exist before deletion.
 */
@ApplicationScoped
public class DeleteServiceUseCase {

    private final ServiceRepository serviceRepository;
    private final ServiceDependencyRepository serviceDependencyRepository;
    private final ServiceCIRepository serviceCIRepository;

    @Inject
    public DeleteServiceUseCase(ServiceRepository serviceRepository,
                                ServiceDependencyRepository serviceDependencyRepository,
                                ServiceCIRepository serviceCIRepository) {
        this.serviceRepository = serviceRepository;
        this.serviceDependencyRepository = serviceDependencyRepository;
        this.serviceCIRepository = serviceCIRepository;
    }

    /**
     * Delete a Service by id.
     *
     * @param id the service identifier
     * @throws ServiceNotFoundException if no Service exists with the given id
     * @throws ServiceInUseException    if the Service has children, dependencies, or CI associations
     */
    public void execute(String id) {
        if (!serviceRepository.existsById(id)) {
            throw new ServiceNotFoundException("Service not found with id: " + id);
        }

        if (serviceRepository.countByParentId(id) > 0) {
            throw new ServiceInUseException(
                    "Service has child services and cannot be deleted");
        }

        if (serviceDependencyRepository.countBySourceServiceIdOrTargetServiceId(id) > 0) {
            throw new ServiceInUseException(
                    "Service has dependencies and cannot be deleted");
        }

        if (serviceCIRepository.countByServiceId(id) > 0) {
            throw new ServiceInUseException(
                    "Service has CI associations and cannot be deleted");
        }

        serviceRepository.deleteById(id);
    }
}
