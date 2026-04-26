package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.application.port.ServiceCIRepository;
import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.entity.ServiceCI;
import com.zenandops.cmdb.domain.exception.CINotFoundException;
import com.zenandops.cmdb.domain.exception.DuplicateServiceCIException;
import com.zenandops.cmdb.domain.exception.ServiceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case for creating a new ServiceCI association.
 * Validates service and CI exist, prevents duplicates.
 */
@ApplicationScoped
public class CreateServiceCIUseCase {

    private final ServiceRepository serviceRepository;
    private final CIRepository ciRepository;
    private final ServiceCIRepository serviceCIRepository;

    @Inject
    public CreateServiceCIUseCase(ServiceRepository serviceRepository,
                                  CIRepository ciRepository,
                                  ServiceCIRepository serviceCIRepository) {
        this.serviceRepository = serviceRepository;
        this.ciRepository = ciRepository;
        this.serviceCIRepository = serviceCIRepository;
    }

    /**
     * Create a new service-CI association.
     *
     * @param serviceId the service identifier
     * @param ciId      the CI identifier
     * @return the created ServiceCI
     */
    public ServiceCI execute(String serviceId, String ciId) {
        if (!serviceRepository.existsById(serviceId)) {
            throw new ServiceNotFoundException(
                    "Service not found with id: " + serviceId);
        }

        if (!ciRepository.existsById(ciId)) {
            throw new CINotFoundException("CI not found with id: " + ciId);
        }

        if (serviceCIRepository.existsByServiceIdAndCiId(serviceId, ciId)) {
            throw new DuplicateServiceCIException(
                    "Service-CI association already exists between service "
                            + serviceId + " and CI " + ciId);
        }

        ServiceCI serviceCI = new ServiceCI();
        serviceCI.setId(UUID.randomUUID().toString());
        serviceCI.setServiceId(serviceId);
        serviceCI.setCiId(ciId);
        serviceCI.setCreatedAt(Instant.now());

        serviceCIRepository.save(serviceCI);
        return serviceCI;
    }
}
