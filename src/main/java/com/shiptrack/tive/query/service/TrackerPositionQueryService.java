package com.shiptrack.tive.query.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptrack.tive.query.config.TiveQueryProperties;
import com.shiptrack.tive.query.model.TrackerPositionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Reads the current (latest) position of a tracker from Redis.
 *
 * Data is written by tive-webhook-receiver's TrackerPositionStateService
 * under the key pattern: {@code tive:tracker:position:{trackerId}}.
 * No writes to Redis are ever performed here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackerPositionQueryService {

    private final RedisTemplate<String, String> redisTemplate;
    private final TiveQueryProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Returns the latest known position for the given tracker ID,
     * or empty if the tracker has no position in Redis.
     */
    public Optional<TrackerPositionState> getCurrentPosition(String trackerId) {
        String key = properties.getRedisPositionPrefix() + trackerId;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                log.debug("No position found in Redis for tracker={}", trackerId);
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, TrackerPositionState.class));
        } catch (Exception ex) {
            log.error("Failed to deserialize position state for tracker={}. key={}", trackerId, key, ex);
            return Optional.empty();
        }
    }
}

