package com.jupitters.feed_me.service;

import com.jupitters.feed_me.dto.FollowingResponse;
import com.jupitters.feed_me.dto.TimelinePageResponse;
import com.jupitters.feed_me.dto.UserProfileResponse;

import java.util.List;

public interface FeedService {
    TimelinePageResponse getHomeFeed(String userId, String cursor, int limit);

    TimelinePageResponse getUserFeed(String userId, String cursor, int limit);

    List<UserProfileResponse> getUsers();

    FollowingResponse getFollowing(String followerId);
}
