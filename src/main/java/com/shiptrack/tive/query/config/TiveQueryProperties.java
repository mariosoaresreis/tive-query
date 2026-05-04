package com.shiptrack.tive.query.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed binding for query-service-specific configuration.
 *
 * All values are injectable from environment variables to support
 * GKE ConfigMap / Secret-based configuration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "tive.query")
public class TiveQueryProperties {

    /**
     * API key expected in every inbound request via the X-Api-Key header.
     * Required — startup will fail if blank.
     */
    private String apiKey = "changeme";

    /**
     * Redis key prefix used by tive-webhook-receiver for current positions.
     * Must match the prefix in TrackerPositionStateService of the receiver.
     */
    private String redisPositionPrefix = "tive:tracker:position:";

    /**
     * Default page size for paginated endpoints.
     */
    private int defaultPageSize = 20;

    /**
     * Maximum page size allowed per request.
     */
    private int maxPageSize = 200;
}

