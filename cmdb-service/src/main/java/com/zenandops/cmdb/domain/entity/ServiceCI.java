package com.zenandops.cmdb.domain.entity;

import java.time.Instant;

/**
 * Association linking a CI to a Service.
 * Designed as a mutable class for MongoDB Panache compatibility.
 */
public class ServiceCI {

    private String id;
    private String serviceId;
    private String ciId;
    private Instant createdAt;

    public ServiceCI() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getCiId() {
        return ciId;
    }

    public void setCiId(String ciId) {
        this.ciId = ciId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
