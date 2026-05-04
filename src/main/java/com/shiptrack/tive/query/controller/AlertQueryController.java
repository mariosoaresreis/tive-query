package com.shiptrack.tive.query.controller;

import com.shiptrack.tive.query.model.AlertRecord;
import com.shiptrack.tive.query.model.dto.PagedResponse;
import com.shiptrack.tive.query.service.AlertQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * REST endpoint for the global alert feed.
 *
 * Base path: /api/v1/alerts
 *
 * GET /api/v1/alerts
 *     Paginated feed of all alerts across all trackers, with optional
 *     time range and alert type filters. Results are sorted newest-first.
 *
 * Query parameters:
 * - from  (ISO-8601 instant, optional) — inclusive start
 * - to    (ISO-8601 instant, optional) — exclusive end (defaults to now)
 * - type  (string, optional)           — alert type filter
 * - page  (int, default 0)             — zero-based page index
 * - size  (int, default 20)            — page size (max 200)
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertQueryController {

    private final AlertQueryService alertService;

    @GetMapping
    public ResponseEntity<PagedResponse<AlertRecord>> getAlerts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(alertService.getAlerts(from, to, type, page, size));
    }
}

