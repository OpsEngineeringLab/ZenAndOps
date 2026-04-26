package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CIVersionRepository;
import com.zenandops.cmdb.domain.entity.CIVersion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing all versions of a given CI, ordered by version number.
 */
@ApplicationScoped
public class ListCIVersionsUseCase {

    private final CIVersionRepository ciVersionRepository;

    @Inject
    public ListCIVersionsUseCase(CIVersionRepository ciVersionRepository) {
        this.ciVersionRepository = ciVersionRepository;
    }

    /**
     * List all versions for a CI ordered by version number.
     *
     * @param ciId the CI identifier
     * @return list of CI versions ordered by version number
     */
    public List<CIVersion> execute(String ciId) {
        return ciVersionRepository.findByCiIdOrderByVersionNumber(ciId);
    }
}
