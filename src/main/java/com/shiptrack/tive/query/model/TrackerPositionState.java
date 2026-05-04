package com.shiptrack.tive.query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Materialized latest position for a tracker, as stored in Redis by tive-webhook-receiver.
 *
 * This is a read model — tive-query never writes this object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackerPositionState {

    /** Tracker identifier — matches the Tive EntityName field. */
    private String trackerId;

    private Double latitude;
    private Double longitude;

    /** Event timestamp in milliseconds epoch (used for staleness checks). */
    private Long entryTimeEpoch;

    /** UTC string timestamp of the event. */
    private String entryTimeUtc;

    /** Wall-clock epoch when the projection was last updated (server time). */
    private Long projectedAtEpoch;
}

