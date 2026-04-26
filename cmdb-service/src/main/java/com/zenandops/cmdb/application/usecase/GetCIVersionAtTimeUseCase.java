package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.application.port.CIVersionRepository;
import com.zenandops.cmdb.domain.entity.CIVersion;
import com.zenandops.cmdb.domain.exception.CINotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;

/**
 * Use case for finding the CIVersion that was active at a specific point in time.
 * Finds the version where startDate <= timestamp AND (endDate > timestamp OR endDate is null).
 */
@ApplicationScoped
public class GetCIVersionAtTimeUseCase {

    private final CIRepository ciRepository;
    private final CIVersionRepository ciVersionRepository;

    @Inject
    public GetCIVersionAtTimeUseCase(CIRepository ciRepository,
                                     CIVersionRepository ciVersionRepository) {
        this.ciRepository = ciRepository;
        this.ciVersionRepository = ciVersionRepository;
    }

    /**
     * Find the CIVersion active at the given timestamp.
     *
     * @param ciId      the CI identifier
     * @param timestamp the point in time to query
     * @return the CIVersion active at that time
     * @throws CINotFoundException if the CI does not exist
     * @throws NotFoundException   if no version was active at the given time
     */
    public CIVersion execute(String ciId, Instant timestamp) {
        if (!ciRepository.existsById(ciId)) {
            throw new CINotFoundException("CI not found with id: " + ciId);
        }

        return ciVersionRepository.findByCiIdAtTime(ciId, timestamp)
                .orElseThrow(() -> new NotFoundException(
                        "No CI version found for CI " + ciId
                                + " at time " + timestamp));
    }
}
