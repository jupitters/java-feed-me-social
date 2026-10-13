package com.jupitters.feed_me.service.impl;

import com.jupitters.feed_me.dto.TimelinePageResponse;
import com.jupitters.feed_me.service.FeedService;
import org.springframework.stereotype.Service;

@Service
public class FeedServiceImpl implements FeedService {
    @Override
    public TimelinePageResponse getHomeFeed(String userId, String cursor, int limit) {
        return null;
    }
}
