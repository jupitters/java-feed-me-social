package com.jupitters.feed_me.service.schedulers;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "feed.async.enabled", havingValue = "true", matchIfMissing = true)
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

    private final StrategyRegistration redisTemplate;
}
