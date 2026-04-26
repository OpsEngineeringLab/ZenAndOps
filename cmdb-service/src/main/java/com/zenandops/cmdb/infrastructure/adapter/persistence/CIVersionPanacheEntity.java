package com.zenandops.cmdb.infrastructure.adapter.persistence;

import com.zenandops.cmdb.domain.vo.DataOrigin;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB Panache entity mapping for the CIVersion domain entity.
 */
@MongoEntity(collection = "ci_versions", database = "zenandops-cmdb")
public class CIVersionPanacheEntity extends PanacheMongoEntity {

    @BsonProperty("ciId")
    public String ciId;

    @BsonProperty("versionNumber")
    public int versionNumber;

    public Map<String, Object> attributes;

    @BsonProperty("startDate")
    public Instant startDate;

    @BsonProperty("endDate")
    public Instant endDate;

    @BsonProperty("dataOrigin")
    public DataOrigin dataOrigin;

    @BsonProperty("dataSourceId")
    public String dataSourceId;

    @BsonProperty("changeReference")
    public String changeReference;

    @BsonProperty("createdAt")
    public Instant createdAt;
}
