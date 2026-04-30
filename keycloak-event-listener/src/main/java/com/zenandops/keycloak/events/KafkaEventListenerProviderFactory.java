package com.zenandops.keycloak.events;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory that creates {@link KafkaEventListenerProvider} instances.
 * <p>
 * Initializes a shared Kafka producer on startup, reading the bootstrap servers
 * from the {@code KAFKA_BOOTSTRAP_SERVERS} environment variable. The producer is
 * shared across all provider instances and closed when Keycloak shuts down.
 */
public class KafkaEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger LOG = Logger.getLogger(KafkaEventListenerProviderFactory.class.getName());
    private static final String PROVIDER_ID = "zenandops-kafka-event-listener";

    private KafkaProducer<String, String> producer;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new KafkaEventListenerProvider(producer);
    }

    @Override
    public void init(Config.Scope config) {
        String bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            LOG.warning("KAFKA_BOOTSTRAP_SERVERS environment variable is not set. "
                    + "Auth events will not be published to Kafka.");
            return;
        }

        try {
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.ACKS_CONFIG, "1");
            props.put(ProducerConfig.RETRIES_CONFIG, 0);
            props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
            props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
            props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);

            producer = new KafkaProducer<>(props);
            LOG.info("Kafka producer initialized for auth events (bootstrap: " + bootstrapServers + ")");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to initialize Kafka producer. Auth events will not be published.", e);
            producer = null;
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization needed
    }

    @Override
    public void close() {
        if (producer != null) {
            try {
                producer.close();
                LOG.info("Kafka producer closed.");
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error closing Kafka producer.", e);
            }
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
