package com.shiptrack.tive.query.controller;

import com.shiptrack.tive.query.model.AlertRecord;
import com.shiptrack.tive.query.model.TrackerPositionState;
import com.shiptrack.tive.query.model.dto.PagedResponse;
import com.shiptrack.tive.query.model.dto.TrackerSummary;
import com.shiptrack.tive.query.service.AlertQueryService;
import com.shiptrack.tive.query.service.TrackerPositionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * REST endpoints for querying tracker data.
 *
 * Base path: /api/v1/trackers
 *
 * GET /api/v1/trackers
 *     List all known trackers (sourced from alert history) enriched with
 *     current position from Redis.
 *
 * GET /api/v1/trackers/{trackerId}/position
 *     Current position of a single tracker from the Redis projection.
 *
 * GET /api/v1/trackers/{trackerId}/alerts
 *     Paginated alert history for a single tracker with optional filters.
 */
@RestController
@RequestMapping("/api/v1/trackers")
@RequiredArgsConstructor
public class TrackerQueryController {

    private final TrackerPositionQueryService positionService;
    private final AlertQueryService alertService;

    /**
     * Lists all trackers that have at least one alert in the database,
     * enriched with their latest position from Redis.
     */
    @GetMapping
    public ResponseEntity<List<TrackerSummary>> listTrackers() {
        return ResponseEntity.ok(alertService.listTrackers());
    }

    /**
     * Returns the current position of a tracker as materialized in Redis.
     *
     * @return 200 with position, or 404 if the tracker has no known position.
     */
    @GetMapping("/{trackerId}/position")
    public ResponseEntity<TrackerPositionState> getPosition(@PathVariable String trackerId) {
        return positionService.getCurrentPosition(trackerId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Returns paginated alert history for a tracker.
     *
     * Query parameters:
     * - from  (ISO-8601 instant, optional) — inclusive start
     * - to    (ISO-8601 instant, optional) — exclusive end
     * - type  (string, optional)           — alert type filter
     * - page  (int, default 0)             — zero-based page index
     * - size  (int, default 20)            — page size
     */
    @GetMapping("/{trackerId}/alerts")
    public ResponseEntity<PagedResponse<AlertRecord>> getTrackerAlerts(
            @PathVariable String trackerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                alertService.getAlertsForTracker(trackerId, from, to, type, page, size));
    }
}

