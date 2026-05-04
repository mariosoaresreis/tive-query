package com.shiptrack.tive.query.service;

import com.shiptrack.tive.query.config.TiveQueryProperties;
import com.shiptrack.tive.query.model.AlertRecord;
import com.shiptrack.tive.query.model.dto.PagedResponse;
import com.shiptrack.tive.query.repository.AlertRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertQueryServiceTest {

    @Mock
    private AlertRecordRepository repository;

    @Mock
    private TrackerPositionQueryService positionService;

    private AlertQueryService service;

    @BeforeEach
    void setUp() {
        TiveQueryProperties props = new TiveQueryProperties();
        props.setDefaultPageSize(20);
        props.setMaxPageSize(200);
        service = new AlertQueryService(repository, props, positionService);
    }

    @Test
    void shouldReturnPagedAlertsForTracker() {
        AlertRecord record = new AlertRecord();
        when(repository.findByTrackerId(eq("VD0001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        PagedResponse<AlertRecord> response =
                service.getAlertsForTracker("VD0001", null, null, null, 0, 10);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getTotalItems()).isEqualTo(1);
    }

    @Test
    void shouldApplyAlertTypeFilterForTracker() {
        when(repository.findByTrackerIdAndAlertType(eq("VD0001"), eq("TEMPERATURE"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAlertsForTracker("VD0001", null, null, "TEMPERATURE", 0, 10);

        verify(repository).findByTrackerIdAndAlertType(eq("VD0001"), eq("TEMPERATURE"), any(Pageable.class));
    }

    @Test
    void shouldApplyTimeRangeFilterForTracker() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to   = Instant.parse("2024-01-02T00:00:00Z");

        when(repository.findByTrackerIdAndReceivedAtBetween(eq("VD0001"), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAlertsForTracker("VD0001", from, to, null, 0, 10);

        verify(repository).findByTrackerIdAndReceivedAtBetween(eq("VD0001"), eq(from), eq(to), any());
    }

    @Test
    void shouldCapPageSizeAtMaxPageSize() {
        when(repository.findByTrackerId(eq("VD0001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Request size 9999 — should be capped at maxPageSize (200)
        service.getAlertsForTracker("VD0001", null, null, null, 0, 9999);

        verify(repository).findByTrackerId(eq("VD0001"),
                argThat(p -> p.getPageSize() == 200));
    }

    @Test
    void shouldReturnTrackerSummariesFromDb() {
        when(repository.findDistinctTrackerIds()).thenReturn(List.of("VD0001"));
        when(repository.findLastAlertPerTracker()).thenReturn(List.of());
        when(repository.findAlertCountsPerTracker()).thenReturn(
                List.<Object[]>of(new Object[]{"VD0001", 5L}));
        when(positionService.getCurrentPosition("VD0001")).thenReturn(Optional.empty());

        var summaries = service.listTrackers();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getTrackerId()).isEqualTo("VD0001");
        assertThat(summaries.get(0).getAlertCount()).isEqualTo(5L);
        assertThat(summaries.get(0).getLatitude()).isNull();
    }
}


