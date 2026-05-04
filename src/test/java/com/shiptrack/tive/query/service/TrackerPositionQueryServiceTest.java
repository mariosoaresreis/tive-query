package com.shiptrack.tive.query.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptrack.tive.query.config.TiveQueryProperties;
import com.shiptrack.tive.query.model.TrackerPositionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackerPositionQueryServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private TrackerPositionQueryService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        TiveQueryProperties props = new TiveQueryProperties();
        props.setRedisPositionPrefix("tive:tracker:position:");
        service = new TrackerPositionQueryService(redisTemplate, props, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void shouldReturnPositionWhenKeyExists() throws Exception {
        TrackerPositionState state = TrackerPositionState.builder()
                .trackerId("VD0001")
                .latitude(-23.55)
                .longitude(-46.63)
                .entryTimeEpoch(1_700_000_000_000L)
                .build();

        String json = objectMapper.writeValueAsString(state);
        when(valueOps.get("tive:tracker:position:VD0001")).thenReturn(json);

        Optional<TrackerPositionState> result = service.getCurrentPosition("VD0001");

        assertThat(result).isPresent();
        assertThat(result.get().getTrackerId()).isEqualTo("VD0001");
        assertThat(result.get().getLatitude()).isEqualTo(-23.55);
    }

    @Test
    void shouldReturnEmptyWhenKeyDoesNotExist() {
        when(valueOps.get("tive:tracker:position:UNKNOWN")).thenReturn(null);

        Optional<TrackerPositionState> result = service.getCurrentPosition("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyOnDeserializationFailure() {
        when(valueOps.get("tive:tracker:position:BAD")).thenReturn("{ not valid json }}}");

        Optional<TrackerPositionState> result = service.getCurrentPosition("BAD");

        assertThat(result).isEmpty();
    }
}

