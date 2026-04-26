package com.zenandops.cmdb.application.port;

import com.zenandops.cmdb.domain.entity.Asset;
import com.zenandops.cmdb.domain.entity.CI;
import com.zenandops.cmdb.domain.entity.Service;

/**
 * Outbound port for publishing CMDB domain events to a message broker.
 */
public interface CmdbEventPublisher {

    void publishAssetCreated(Asset asset, String userId);

    void publishAssetUpdated(Asset asset, String userId);

    void publishCICreated(CI ci, String userId);

    void publishCIUpdated(CI ci, String userId);

    void publishServiceCreated(Service service, String userId);

    void publishServiceUpdated(Service service, String userId);

    void publishVersionCreated(String entityId, String entityType, String userId);
}
