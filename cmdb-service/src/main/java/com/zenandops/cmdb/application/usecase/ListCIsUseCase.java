package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.vo.CIStatus;
import com.zenandops.cmdb.domain.vo.CIType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for listing CIs with optional filtering by organizationId, type, status, and assetId.
 */
@ApplicationScoped
public class ListCIsUseCase {

    private final CIRepository ciRepository;

    @Inject
    public ListCIsUseCase(CIRepository ciRepository) {
        this.ciRepository = ciRepository;
    }

    /**
     * List CIs with optional filters. Pass null for any filter to skip it.
     *
     * @param organizationId optional organization filter
     * @param type           optional CI type filter
     * @param status         optional status filter
     * @param assetId        optional asset filter
     * @return filtered list of CIs
     */
    public List<CI> execute(String organizationId, CIType type, CIStatus status, String assetId) {
        return ciRepository.findWithFilters(organizationId, type, status, assetId);
    }
}
