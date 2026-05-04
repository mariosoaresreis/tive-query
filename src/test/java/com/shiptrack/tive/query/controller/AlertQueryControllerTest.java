package com.shiptrack.tive.query.controller;

import com.shiptrack.tive.query.model.dto.PagedResponse;
import com.shiptrack.tive.query.service.AlertQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertQueryController.class)
@Import({com.shiptrack.tive.query.security.SecurityConfig.class,
        com.shiptrack.tive.query.security.ApiKeyAuthenticationFilter.class,
        com.shiptrack.tive.query.config.TiveQueryProperties.class})
@TestPropertySource(properties = {"tive.query.api-key=test-key"})
class AlertQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertQueryService alertService;

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnGlobalAlertFeed() throws Exception {
        PagedResponse<Object> page = PagedResponse.builder()
                .items(List.of())
                .page(0).size(20).totalItems(0).totalPages(0).last(true)
                .build();
        when(alertService.getAlerts(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn((PagedResponse) page);

        mockMvc.perform(get("/api/v1/alerts")
                        .header("X-Api-Key", "test-key")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void shouldReturn401WithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAcceptTypeFilter() throws Exception {
        PagedResponse<Object> page = PagedResponse.builder()
                .items(List.of()).page(0).size(20).totalItems(0).totalPages(0).last(true).build();
        when(alertService.getAlerts(any(), any(), eq("SHOCK"), anyInt(), anyInt()))
                .thenReturn((PagedResponse) page);

        mockMvc.perform(get("/api/v1/alerts?type=SHOCK")
                        .header("X-Api-Key", "test-key")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}

