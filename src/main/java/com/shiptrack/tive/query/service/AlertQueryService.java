package com.shiptrack.tive.query.service;

import com.shiptrack.tive.query.config.TiveQueryProperties;
import com.shiptrack.tive.query.model.AlertRecord;
import com.shiptrack.tive.query.model.dto.PagedResponse;
import com.shiptrack.tive.query.model.dto.TrackerSummary;
import com.shiptrack.tive.query.repository.AlertRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Queries alert history from PostgreSQL.
 *
 * All methods are read-only transactions; no data is ever modified.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertQueryService {

    private final AlertRecordRepository repository;
    private final TiveQueryProperties properties;
    private final TrackerPositionQueryService positionService;

    // ── Per-tracker alert queries ───────────────────────────────────────────

    /**
     * Returns a page of alerts for a given tracker, with optional time range and
     * alert type filters. Results are sorted newest-first.
     *
     * @param trackerId  mandatory tracker identifier
     * @param from       inclusive start of the time window (null = no lower bound)
     * @param to         exclusive end of the time window (null = now)
     * @param alertType  optional alert type filter
     * @param page       zero-based page index
     * @param size       page size (capped at maxPageSize)
     */
    public PagedResponse<AlertRecord> getAlertsForTracker(
            String trackerId,
            Instant from,
            Instant to,
            String alertType,
            int page,
            int size) {

        size = clampSize(size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));

        Page<AlertRecord> result;

        boolean hasType = alertType != null && !alertType.isBlank();
        boolean hasFrom = from != null;
        boolean hasTo = to != null;
        Instant effectiveTo = hasTo ? to : Instant.now();

        if (hasType && (hasFrom || hasTo)) {
            result = repository.findByTrackerIdAndAlertTypeAndReceivedAtBetween(
                    trackerId, alertType, hasFrom ? from : Instant.EPOCH, effectiveTo, pageable);
        } else if (hasType) {
            result = repository.findByTrackerIdAndAlertType(trackerId, alertType, pageable);
        } else if (hasFrom || hasTo) {
            result = repository.findByTrackerIdAndReceivedAtBetween(
                    trackerId, hasFrom ? from : Instant.EPOCH, effectiveTo, pageable);
        } else {
            result = repository.findByTrackerId(trackerId, pageable);
        }

        return toPagedResponse(result, page, size);
    }

    // ── Global alert feed ───────────────────────────────────────────────────

    /**
     * Returns a global page of alerts across all trackers, with optional
     * time range and alert type filters. Results are sorted newest-first.
     */
    public PagedResponse<AlertRecord> getAlerts(
            Instant from,
            Instant to,
            String alertType,
            int page,
            int size) {

        size = clampSize(size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));

        boolean hasType = alertType != null && !alertType.isBlank();
        boolean hasFrom = from != null;
        boolean hasTo = to != null;
        Instant effectiveTo = hasTo ? to : Instant.now();

        Page<AlertRecord> result;
        if (hasType && (hasFrom || hasTo)) {
            result = repository.findByAlertTypeAndReceivedAtBetween(
                    alertType, hasFrom ? from : Instant.EPOCH, effectiveTo, pageable);
        } else if (hasType) {
            result = repository.findByAlertType(alertType, pageable);
        } else if (hasFrom || hasTo) {
            result = repository.findByReceivedAtBetween(
                    hasFrom ? from : Instant.EPOCH, effectiveTo, pageable);
        } else {
            result = repository.findAll(pageable);
        }

        return toPagedResponse(result, page, size);
    }

    // ── Tracker listing ─────────────────────────────────────────────────────

    /**
     * Returns a summary for every tracker that has at least one alert in the database,
     * enriched with the current position from Redis.
     */
    public List<TrackerSummary> listTrackers() {
        List<String> trackerIds = repository.findDistinctTrackerIds();

        // Build last-alert lookup: trackerId → [receivedAt, alertType]
        Map<String, Instant> lastAlertAt = new HashMap<>();
        Map<String, String> lastAlertType = new HashMap<>();
        for (Object[] row : repository.findLastAlertPerTracker()) {
            String tid = (String) row[0];
            Instant ts = (Instant) row[1];
            String type = (String) row[2];
            lastAlertAt.merge(tid, ts, (a, b) -> a.isAfter(b) ? a : b);
            if (!lastAlertAt.containsKey(tid) || lastAlertAt.get(tid).equals(ts)) {
                lastAlertType.put(tid, type);
            }
        }

        // Build count lookup: trackerId → count
        Map<String, Long> counts = repository.findAlertCountsPerTracker()
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        return trackerIds.stream()
                .map(tid -> {
                    Optional<com.shiptrack.tive.query.model.TrackerPositionState> pos =
                            positionService.getCurrentPosition(tid);

                    return TrackerSummary.builder()
                            .trackerId(tid)
                            .lastAlertAt(lastAlertAt.get(tid))
                            .lastAlertType(lastAlertType.get(tid))
                            .alertCount(counts.getOrDefault(tid, 0L))
                            .latitude(pos.map(com.shiptrack.tive.query.model.TrackerPositionState::getLatitude).orElse(null))
                            .longitude(pos.map(com.shiptrack.tive.query.model.TrackerPositionState::getLongitude).orElse(null))
                            .positionEpoch(pos.map(com.shiptrack.tive.query.model.TrackerPositionState::getEntryTimeEpoch).orElse(null))
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private int clampSize(int requested) {
        if (requested <= 0) return properties.getDefaultPageSize();
        return Math.min(requested, properties.getMaxPageSize());
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page, int pageNum, int size) {
        return PagedResponse.<T>builder()
                .items(page.getContent())
                .page(pageNum)
                .size(size)
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}

