package com.zenandops.cmdb.application.dto;

import com.zenandops.cmdb.domain.vo.CriticalityLevel;
import com.zenandops.cmdb.domain.vo.ServiceStatus;
import com.zenandops.cmdb.domain.vo.ServiceType;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a node in the service hierarchy tree.
 */
public class ServiceTreeNode {

    private String id;
    private String name;
    private String description;
    private ServiceType type;
    private String organizationId;
    private String businessOwner;
    private String technicalOwner;
    private CriticalityLevel criticality;
    private ServiceStatus status;
    private List<ServiceTreeNode> children;

    public ServiceTreeNode() {
        this.children = new ArrayList<>();
    }

    public ServiceTreeNode(String id, String name, String description, ServiceType type,
                           String organizationId, String businessOwner, String technicalOwner,
                           CriticalityLevel criticality, ServiceStatus status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.organizationId = organizationId;
        this.businessOwner = businessOwner;
        this.technicalOwner = technicalOwner;
        this.criticality = criticality;
        this.status = status;
        this.children = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ServiceType getType() {
        return type;
    }

    public void setType(ServiceType type) {
        this.type = type;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getBusinessOwner() {
        return businessOwner;
    }

    public void setBusinessOwner(String businessOwner) {
        this.businessOwner = businessOwner;
    }

    public String getTechnicalOwner() {
        return technicalOwner;
    }

    public void setTechnicalOwner(String technicalOwner) {
        this.technicalOwner = technicalOwner;
    }

    public CriticalityLevel getCriticality() {
        return criticality;
    }

    public void setCriticality(CriticalityLevel criticality) {
        this.criticality = criticality;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
    }

    public List<ServiceTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<ServiceTreeNode> children) {
        this.children = children;
    }
}
