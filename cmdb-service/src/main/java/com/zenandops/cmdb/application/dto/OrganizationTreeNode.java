package com.zenandops.cmdb.application.dto;

import com.zenandops.cmdb.domain.vo.OrganizationType;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a node in the organizational hierarchy tree.
 */
public class OrganizationTreeNode {

    private String id;
    private String name;
    private OrganizationType type;
    private String responsiblePerson;
    private String costCenter;
    private List<OrganizationTreeNode> children;

    public OrganizationTreeNode() {
        this.children = new ArrayList<>();
    }

    public OrganizationTreeNode(String id, String name, OrganizationType type,
                                String responsiblePerson, String costCenter) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.responsiblePerson = responsiblePerson;
        this.costCenter = costCenter;
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

    public OrganizationType getType() {
        return type;
    }

    public void setType(OrganizationType type) {
        this.type = type;
    }

    public String getResponsiblePerson() {
        return responsiblePerson;
    }

    public void setResponsiblePerson(String responsiblePerson) {
        this.responsiblePerson = responsiblePerson;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public List<OrganizationTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<OrganizationTreeNode> children) {
        this.children = children;
    }
}
