package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.ServiceCIRepository;
import com.zenandops.cmdb.domain.entity.ServiceCI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all services associated with a given CI.
 */
@ApplicationScoped
public class ListServicesByCIUseCase {

    private final ServiceCIRepository serviceCIRepository;

    @Inject
    public ListServicesByCIUseCase(ServiceCIRepository serviceCIRepository) {
        this.serviceCIRepository = serviceCIRepository;
    }

    /**
     * List all service-CI associations for a CI.
     *
     * @param ciId the CI identifier
     * @return list of ServiceCI associations
     */
    public List<ServiceCI> execute(String ciId) {
        return serviceCIRepository.findByCiId(ciId);
    }
}
