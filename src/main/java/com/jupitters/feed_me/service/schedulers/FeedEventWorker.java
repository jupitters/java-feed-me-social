package com.jupitters.feed_me.service.schedulers;

import com.jupitters.feed_me.repository.DeadLetterFeedEventRepository;
import com.jupitters.feed_me.repository.FeedEventFailureRepository;
import com.jupitters.feed_me.repository.ProcessedFeedEventRepository;
import com.jupitters.feed_me.service.FeedMetricsService;
import com.jupitters.feed_me.service.FeedService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "feed.async.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Getter
public class FeedEventWorker {
    @PostConstruct
    public void initializeConsumerGroup() {
        try {
            StreamInfo.XInfoGroup groups = red
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final StringRedisTemplate redisTemplate;
    private final ProcessedFeedEventRepository processedFeedEventRepository;
    private final DeadLetterFeedEventRepository deadLetterFeedEventRepository;
    private final FeedEventFailureRepository feedEventFailureRepository;

    private final FeedService feedService;
    private final FeedMetricsService feedMetricsService;

    @Value("${feed.async.stream.key}")
    private String streamKey;
}
