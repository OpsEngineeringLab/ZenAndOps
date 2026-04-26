package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.OrganizationTreeNode;
import com.zenandops.cmdb.application.port.OrganizationRepository;
import com.zenandops.cmdb.domain.entity.Organization;
import com.zenandops.cmdb.domain.vo.OrganizationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use case for retrieving the full organizational tree from ROOT.
 * Fetches all organizations and assembles parent-child relationships in memory.
 */
@ApplicationScoped
public class GetOrganizationTreeUseCase {

    private final OrganizationRepository organizationRepository;

    @Inject
    public GetOrganizationTreeUseCase(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /**
     * Build and return the full organizational tree starting from ROOT.
     *
     * @return list of root-level tree nodes (typically one ROOT node)
     */
    public List<OrganizationTreeNode> execute() {
        List<Organization> allOrgs = organizationRepository.findAll();

        Map<String, OrganizationTreeNode> nodeMap = new HashMap<>();
        for (Organization org : allOrgs) {
            nodeMap.put(org.getId(), toTreeNode(org));
        }

        List<OrganizationTreeNode> roots = new ArrayList<>();
        for (Organization org : allOrgs) {
            OrganizationTreeNode node = nodeMap.get(org.getId());
            if (org.getParentId() == null) {
                roots.add(node);
            } else {
                OrganizationTreeNode parent = nodeMap.get(org.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }

        return roots;
    }

    private OrganizationTreeNode toTreeNode(Organization org) {
        return new OrganizationTreeNode(
                org.getId(),
                org.getName(),
                org.getType(),
                org.getResponsiblePerson(),
                org.getCostCenter()
        );
    }
}
