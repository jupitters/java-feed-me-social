package com.jupitters.feed_me.service;

import com.jupitters.feed_me.dto.TimelinePageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeedCacheService {
    public static final int DEFAULT_PAGE_SIZE = 5;

    private static final Duration HOME_FEED_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    private final FeedMetricsService feedMetricsService;

    private final ObjectMapper objectMapper;

    public Optional<TimelinePageResponse> getHomeFeed(String userId) {
        try{
            String payload = redisTemplate.opsForValue().get(homeFeedKey(userId));
            if(payload == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(payload, TimelinePageResponse.class));
        } catch(Exception e) {
            return Optional.empty();
        }
    }

    private String homeFeedKey(String userId) {
        return "feed:home:" + userId;
    }

    public void cacheHomeFeed(TimelinePageResponse timelinePageResponse) {
        writeHomeFeed(timelinePageResponse);
        feedMetricsService.recordCacheMutation("write_first_page");
    }

    private void writeHomeFeed(TimelinePageResponse timelinePageResponse) {
        try {
            String payload = objectMapper.writeValueAsString(timelinePageResponse);
            redisTemplate.opsForValue().set(
                    homeFeedKey(timelinePageResponse.timelineOwnerId()), payload, HOME_FEED_TTL
            );


        } catch (Exception e) {
            //ignore
        }
    }
}
