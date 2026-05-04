package com.shiptrack.tive.query.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Summary view of a tracker — returned by the tracker list endpoint.
 *
 * Current position is included when known (i.e., when the tracker has
 * emitted at least one position event consumed by tive-webhook-receiver).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackerSummary {

    /** Tracker identifier (Tive EntityName). */
    private String trackerId;

    /** Timestamp of the most recent alert, or null if no alerts exist. */
    private Instant lastAlertAt;

    /** Most recent alert type, or null if no alerts exist. */
    private String lastAlertType;

    /** Total number of alerts stored for this tracker. */
    private long alertCount;

    /** Current latitude from the Redis position projection, or null. */
    private Double latitude;

    /** Current longitude from the Redis position projection, or null. */
    private Double longitude;

    /** Epoch millis of the latest position event, or null. */
    private Long positionEpoch;
}

