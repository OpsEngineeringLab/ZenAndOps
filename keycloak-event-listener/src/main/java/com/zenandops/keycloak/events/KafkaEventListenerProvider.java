package com.zenandops.keycloak.events;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keycloak Event Listener that intercepts LOGIN, LOGOUT, and REFRESH_TOKEN events
 * and publishes them to the Kafka {@code auth-events} topic.
 * <p>
 * Events are serialized as JSON matching the existing AuthEvent schema:
 * <pre>
 * {
 *   "eventId": "uuid-string",
 *   "eventType": "LOGIN | LOGOFF | TOKEN_REFRESH",
 *   "userId": "keycloak-user-id",
 *   "userLogin": "username",
 *   "timestamp": "2025-01-15T10:30:00Z"
 * }
 * </pre>
 * <p>
 * Kafka unavailability is handled gracefully: failures are logged as warnings
 * and never block the authentication flow.
 */
public class KafkaEventListenerProvider implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(KafkaEventListenerProvider.class.getName());
    private static final String TOPIC = "auth-events";
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    /**
     * Maps Keycloak event types to the ZenAndOps auth event type names.
     * LOGIN → LOGIN, LOGOUT → LOGOFF, REFRESH_TOKEN → TOKEN_REFRESH
     */
    private static final Map<EventType, String> EVENT_TYPE_MAP = Map.of(
            EventType.LOGIN, "LOGIN",
            EventType.LOGOUT, "LOGOFF",
            EventType.REFRESH_TOKEN, "TOKEN_REFRESH"
    );

    private final KafkaProducer<String, String> producer;

    public KafkaEventListenerProvider(KafkaProducer<String, String> producer) {
        this.producer = producer;
    }

    @Override
    public void onEvent(Event event) {
        String mappedEventType = EVENT_TYPE_MAP.get(event.getType());
        if (mappedEventType == null) {
            // Not an event we care about — ignore silently
            return;
        }

        if (producer == null) {
            LOG.warning("Kafka producer is not available. Skipping auth event: " + mappedEventType);
            return;
        }

        String eventId = UUID.randomUUID().toString();
        String userId = event.getUserId();
        String userLogin = extractUserLogin(event);
        String timestamp = ISO_FORMATTER.format(Instant.now());

        String json = buildJson(eventId, mappedEventType, userId, userLogin, timestamp);

        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, userId, json);
            // Fire-and-forget: send asynchronously, log errors in callback
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    LOG.log(Level.WARNING,
                            "Failed to publish auth event to Kafka: type=" + mappedEventType
                                    + ", userId=" + userId, exception);
                } else {
                    LOG.info("Published auth event: type=" + mappedEventType
                            + ", userId=" + userId
                            + ", topic=" + metadata.topic()
                            + ", partition=" + metadata.partition()
                            + ", offset=" + metadata.offset());
                }
            });
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "Error sending auth event to Kafka: type=" + mappedEventType
                            + ", userId=" + userId, e);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // No-op: admin events are not published
    }

    @Override
    public void close() {
        // No-op: the producer lifecycle is managed by the factory
    }

    /**
     * Extracts the username from the event details.
     * Falls back to userId if the username is not available.
     */
    private String extractUserLogin(Event event) {
        Map<String, String> details = event.getDetails();
        if (details != null) {
            String username = details.get("username");
            if (username != null && !username.isBlank()) {
                return username;
            }
        }
        // Fallback to userId if username is not in event details
        return event.getUserId();
    }

    /**
     * Builds a JSON string manually to avoid adding a JSON library dependency.
     * The Keycloak SPI JAR should be as lightweight as possible.
     */
    private String buildJson(String eventId, String eventType, String userId,
                             String userLogin, String timestamp) {
        return "{" +
                "\"eventId\":\"" + escapeJson(eventId) + "\"," +
                "\"eventType\":\"" + escapeJson(eventType) + "\"," +
                "\"userId\":\"" + escapeJson(userId) + "\"," +
                "\"userLogin\":\"" + escapeJson(userLogin) + "\"," +
                "\"timestamp\":\"" + escapeJson(timestamp) + "\"" +
                "}";
    }

    /**
     * Escapes special characters in a JSON string value.
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
