package com.jupitters.feed_me.service;

import com.jupitters.feed_me.dto.TimelinePageResponse;

public interface FeedService {
    TimelinePageResponse getHomeFeed(String userId, String cursor, int limit);
}
