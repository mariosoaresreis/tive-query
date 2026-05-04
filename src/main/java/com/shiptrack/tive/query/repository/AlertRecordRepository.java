package com.shiptrack.tive.query.repository;

import com.shiptrack.tive.query.model.AlertRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Read-only repository for alert history.
 *
 * All query methods are executed inside a read-only transaction
 * (enforced at the service layer). No derived mutation methods are ever called.
 */
public interface AlertRecordRepository extends JpaRepository<AlertRecord, String> {

    // ── Per-tracker queries ─────────────────────────────────────────────────

    Page<AlertRecord> findByTrackerId(String trackerId, Pageable pageable);

    Page<AlertRecord> findByTrackerIdAndReceivedAtBetween(
            String trackerId, Instant from, Instant to, Pageable pageable);

    Page<AlertRecord> findByTrackerIdAndAlertType(
            String trackerId, String alertType, Pageable pageable);

    Page<AlertRecord> findByTrackerIdAndAlertTypeAndReceivedAtBetween(
            String trackerId, String alertType, Instant from, Instant to, Pageable pageable);

    long countByTrackerId(String trackerId);

    // ── Global alert feed ───────────────────────────────────────────────────

    Page<AlertRecord> findByReceivedAtBetween(Instant from, Instant to, Pageable pageable);

    Page<AlertRecord> findByAlertType(String alertType, Pageable pageable);

    Page<AlertRecord> findByAlertTypeAndReceivedAtBetween(
            String alertType, Instant from, Instant to, Pageable pageable);

    // ── Tracker listing helpers ─────────────────────────────────────────────

    /**
     * Returns the distinct set of tracker IDs that have at least one stored alert.
     * Used by the tracker list endpoint to enumerate known trackers.
     */
    @Query("SELECT DISTINCT a.trackerId FROM AlertRecord a ORDER BY a.trackerId ASC")
    List<String> findDistinctTrackerIds();

    /**
     * Returns the most recent receivedAt instant for every distinct tracker.
     * Used to populate lastAlertAt in the tracker summary.
     */
    @Query("SELECT a.trackerId, MAX(a.receivedAt), a.alertType FROM AlertRecord a " +
           "GROUP BY a.trackerId, a.alertType")
    List<Object[]> findLastAlertPerTracker();

    /**
     * Returns the count of alerts per tracker ID.
     */
    @Query("SELECT a.trackerId, COUNT(a.id) FROM AlertRecord a GROUP BY a.trackerId")
    List<Object[]> findAlertCountsPerTracker();
}

