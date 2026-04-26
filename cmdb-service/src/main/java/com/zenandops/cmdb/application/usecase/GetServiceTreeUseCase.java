package com.zenandops.cmdb.application.usecase;

import com.zenandops.cmdb.application.dto.ServiceTreeNode;
import com.zenandops.cmdb.application.port.ServiceRepository;
import com.zenandops.cmdb.domain.entity.Service;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use case for retrieving the service hierarchy tree.
 * Returns root-level services (no parent) with their children assembled in memory.
 */
@ApplicationScoped
public class GetServiceTreeUseCase {

    private final ServiceRepository serviceRepository;

    @Inject
    public GetServiceTreeUseCase(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    /**
     * Build and return the service hierarchy tree starting from root-level services.
     *
     * @return list of root-level service tree nodes
     */
    public List<ServiceTreeNode> execute() {
        List<Service> allServices = serviceRepository.findAll();

        Map<String, ServiceTreeNode> nodeMap = new HashMap<>();
        for (Service svc : allServices) {
            nodeMap.put(svc.getId(), toTreeNode(svc));
        }

        List<ServiceTreeNode> roots = new ArrayList<>();
        for (Service svc : allServices) {
            ServiceTreeNode node = nodeMap.get(svc.getId());
            if (svc.getParentId() == null) {
                roots.add(node);
            } else {
                ServiceTreeNode parent = nodeMap.get(svc.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }

        return roots;
    }

    private ServiceTreeNode toTreeNode(Service svc) {
        return new ServiceTreeNode(
                svc.getId(),
                svc.getName(),
                svc.getDescription(),
                svc.getType(),
                svc.getOrganizationId(),
                svc.getBusinessOwner(),
                svc.getTechnicalOwner(),
                svc.getCriticality(),
                svc.getStatus()
        );
    }
}
