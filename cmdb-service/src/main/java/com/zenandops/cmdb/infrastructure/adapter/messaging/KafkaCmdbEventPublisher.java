package com.zenandops.cmdb.infrastructure.adapter.messaging;

import com.zenandops.cmdb.application.port.CmdbEventPublisher;
import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.entity.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka adapter implementing the CmdbEventPublisher port using SmallRye Reactive Messaging.
 * Publishes domain events to the cmdb-events topic.
 * Handles Kafka unavailability gracefully by logging and continuing.
 */
@ApplicationScoped
public class KafkaCmdbEventPublisher implements CmdbEventPublisher {

    private static final Logger LOG = Logger.getLogger(KafkaCmdbEventPublisher.class);

    @Inject
    @Channel("cmdb-events")
    Emitter<String> emitter;

    private final ObjectMapper objectMapper;

    public KafkaCmdbEventPublisher() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void publishAssetCreated(Asset asset, String userId) {
        publish("ASSET_CREATED", asset.getId(), "ASSET", userId,
                Map.of("name", asset.getName(), "type", asset.getType().name()));
    }

    @Override
    public void publishAssetUpdated(Asset asset, String userId) {
        publish("ASSET_UPDATED", asset.getId(), "ASSET", userId,
                Map.of("name", asset.getName(), "type", asset.getType().name()));
    }

    @Override
    public void publishCICreated(CI ci, String userId) {
        publish("CI_CREATED", ci.getId(), "CI", userId,
                Map.of("name", ci.getName(), "type", ci.getType().name()));
    }

    @Override
    public void publishCIUpdated(CI ci, String userId) {
        publish("CI_UPDATED", ci.getId(), "CI", userId,
                Map.of("name", ci.getName(), "type", ci.getType().name()));
    }

    @Override
    public void publishServiceCreated(Service service, String userId) {
        publish("SERVICE_CREATED", service.getId(), "SERVICE", userId,
                Map.of("name", service.getName(), "type", service.getType().name()));
    }

    @Override
    public void publishServiceUpdated(Service service, String userId) {
        publish("SERVICE_UPDATED", service.getId(), "SERVICE", userId,
                Map.of("name", service.getName(), "type", service.getType().name()));
    }

    @Override
    public void publishVersionCreated(String entityId, String entityType, String userId) {
        publish("VERSION_CREATED", entityId, entityType, userId, Map.of());
    }

    private void publish(String eventType, String entityId, String entityType,
                         String userId, Map<String, Object> metadata) {
        try {
            CmdbEvent event = new CmdbEvent(
                    UUID.randomUUID().toString(),
                    eventType,
                    entityId,
                    entityType,
                    userId,
                    Instant.now(),
                    metadata
            );
            String json = objectMapper.writeValueAsString(event);
            emitter.send(json);
            LOG.infof("Published CMDB event: type=%s, entityId=%s, entityType=%s",
                    eventType, entityId, entityType);
        } catch (Exception e) {
            LOG.warnf("Failed to publish CMDB event: type=%s, entityId=%s, reason=%s",
                    eventType, entityId, e.getMessage());
        }
    }
}
