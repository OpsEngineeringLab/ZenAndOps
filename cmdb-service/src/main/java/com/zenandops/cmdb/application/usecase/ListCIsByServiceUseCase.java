package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.ServiceCIRepository;
import com.zenandops.cmdb.domain.entity.ServiceCI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all CIs associated with a given service.
 */
@ApplicationScoped
public class ListCIsByServiceUseCase {

    private final ServiceCIRepository serviceCIRepository;

    @Inject
    public ListCIsByServiceUseCase(ServiceCIRepository serviceCIRepository) {
        this.serviceCIRepository = serviceCIRepository;
    }

    /**
     * List all service-CI associations for a service.
     *
     * @param serviceId the service identifier
     * @return list of ServiceCI associations
     */
    public List<ServiceCI> execute(String serviceId) {
        return serviceCIRepository.findByServiceId(serviceId);
    }
}
