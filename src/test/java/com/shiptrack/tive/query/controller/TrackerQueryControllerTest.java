package com.shiptrack.tive.query.controller;

import com.shiptrack.tive.query.model.TrackerPositionState;
import com.shiptrack.tive.query.model.dto.PagedResponse;
import com.shiptrack.tive.query.model.dto.TrackerSummary;
import com.shiptrack.tive.query.service.AlertQueryService;
import com.shiptrack.tive.query.service.TrackerPositionQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrackerQueryController.class)
@Import({com.shiptrack.tive.query.security.SecurityConfig.class,
        com.shiptrack.tive.query.security.ApiKeyAuthenticationFilter.class,
        com.shiptrack.tive.query.config.TiveQueryProperties.class})
@TestPropertySource(properties = {"tive.query.api-key=test-key"})
class TrackerQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackerPositionQueryService positionService;

    @MockBean
    private AlertQueryService alertService;

    // ── /position ───────────────────────────────────────────────────────────

    @Test
    void shouldReturn200WithPositionWhenFound() throws Exception {
        TrackerPositionState state = TrackerPositionState.builder()
                .trackerId("VD0001")
                .latitude(-23.55)
                .longitude(-46.63)
                .entryTimeEpoch(1_700_000_000_000L)
                .build();
        when(positionService.getCurrentPosition("VD0001")).thenReturn(Optional.of(state));

        mockMvc.perform(get("/api/v1/trackers/VD0001/position")
                        .header("X-Api-Key", "test-key")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackerId").value("VD0001"))
                .andExpect(jsonPath("$.latitude").value(-23.55));
    }

    @Test
    void shouldReturn404WhenPositionNotFound() throws Exception {
        when(positionService.getCurrentPosition("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/trackers/UNKNOWN/position")
                        .header("X-Api-Key", "test-key"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401WhenApiKeyMissing() throws Exception {
        mockMvc.perform(get("/api/v1/trackers/VD0001/position"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenApiKeyInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/trackers/VD0001/position")
                        .header("X-Api-Key", "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    // ── /trackers list ──────────────────────────────────────────────────────

    @Test
    void shouldReturnTrackerList() throws Exception {
        TrackerSummary summary = TrackerSummary.builder()
                .trackerId("VD0001")
                .alertCount(3)
                .build();
        when(alertService.listTrackers()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/trackers")
                        .header("X-Api-Key", "test-key")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trackerId").value("VD0001"))
                .andExpect(jsonPath("$[0].alertCount").value(3));
    }

    // ── /trackers/{trackerId}/alerts ─────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnPagedAlertsForTracker() throws Exception {
        PagedResponse<Object> page = PagedResponse.builder()
                .items(List.of())
                .page(0)
                .size(20)
                .totalItems(0)
                .totalPages(0)
                .last(true)
                .build();
        when(alertService.getAlertsForTracker(eq("VD0001"), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn((PagedResponse) page);

        mockMvc.perform(get("/api/v1/trackers/VD0001/alerts")
                        .header("X-Api-Key", "test-key")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalItems").value(0));
    }
}

