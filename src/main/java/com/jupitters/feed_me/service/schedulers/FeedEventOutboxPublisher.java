package com.jupitters.feed_me.service.schedulers;

import com.jupitters.feed_me.repository.OutboxEventRepository;
import com.jupitters.feed_me.service.FeedMetricsService;
import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "feed.async.enabled", havingValue = "true", matchIfMissing = true)
@Getter
public class FeedEventOutboxPublisher {
    private final StringRedisTemplate redisTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final String streamKey;
    private final int publishBatchSize;
    private final int maxAttempts;
    private final FeedMetricsService feedMetricsService;


    public FeedEventOutboxPublisher(StringRedisTemplate redisTemplate,
                                    OutboxEventRepository outboxEventRepository, String streamKey, int publishBatchSize, int maxAttempts, FeedMetricsService feedMetricsService) {
        this.redisTemplate = redisTemplate;
        this.outboxEventRepository = outboxEventRepository;
        this.streamKey = streamKey;
        this.publishBatchSize = publishBatchSize;
        this.maxAttempts = maxAttempts;
        this.feedMetricsService = feedMetricsService;
    }
}
