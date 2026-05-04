package com.shiptrack.tive.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Tive Query Service — CQRS read side.
 *
 * Responsibilities:
 * - Serve current tracker positions from Redis (written by tive-webhook-receiver).
 * - Serve paginated/filtered alert history from PostgreSQL (written by tive-webhook-receiver).
 * - No writes to any data store; strictly read-only.
 *
 * Companion to tive-webhook-receiver. Both services share the same
 * GCP project but are deployed in separate Kubernetes namespaces.
 */
@SpringBootApplication
@EnableConfigurationProperties
public class TiveQueryApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiveQueryApplication.class, args);
    }
}

