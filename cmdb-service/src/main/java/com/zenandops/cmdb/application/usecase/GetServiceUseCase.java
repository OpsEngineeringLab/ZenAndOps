package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.entity.Service;
import com.zenandops.cmdb.domain.exception.ServiceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for retrieving a single Service by its identifier.
 */
@ApplicationScoped
public class GetServiceUseCase {

    private final ServiceRepository serviceRepository;

    @Inject
    public GetServiceUseCase(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    /**
     * Retrieve a Service by id.
     *
     * @param id the service identifier
     * @return the Service
     * @throws ServiceNotFoundException if no Service exists with the given id
     */
    public Service execute(String id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFoundException(
                        "Service not found with id: " + id));
    }
}
