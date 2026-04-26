package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.domain.vo.AssetStatus;
import com.zenandops.cmdb.domain.vo.AssetType;
import com.zenandops.cmdb.domain.vo.CostType;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * MongoDB Panache entity mapping for the Asset domain entity.
 */
@MongoEntity(collection = "assets", database = "zenandops-cmdb")
public class AssetPanacheEntity extends PanacheMongoEntity {

    public String name;
    public AssetType type;

    @BsonProperty("organizationId")
    public String organizationId;

    public BigDecimal cost;

    @BsonProperty("costType")
    public CostType costType;

    @BsonProperty("acquisitionDate")
    public Instant acquisitionDate;

    public AssetStatus status;
    public String supplier;

    @BsonProperty("createdAt")
    public Instant createdAt;

    @BsonProperty("updatedAt")
    public Instant updatedAt;
}
