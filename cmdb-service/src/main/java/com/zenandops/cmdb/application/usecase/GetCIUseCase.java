package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.port.CIRepository;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.exception.CINotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for retrieving a single CI by its identifier.
 */
@ApplicationScoped
public class GetCIUseCase {

    private final CIRepository ciRepository;

    @Inject
    public GetCIUseCase(CIRepository ciRepository) {
        this.ciRepository = ciRepository;
    }

    /**
     * Retrieve a CI by id.
     *
     * @param id the CI identifier
     * @return the CI
     * @throws CINotFoundException if no CI exists with the given id
     */
    public CI execute(String id) {
        return ciRepository.findById(id)
                .orElseThrow(() -> new CINotFoundException(
                        "CI not found with id: " + id));
    }
}
