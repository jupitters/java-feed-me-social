package com.jupitters.feed_me.service;

import com.jupitters.feed_me.dto.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public interface FeedService {
    TimelinePageResponse getHomeFeed(String userId, String cursor, int limit);

    TimelinePageResponse getUserFeed(String userId, String cursor, int limit);

    List<UserProfileResponse> getUsers();

    FollowingResponse getFollowing(String followerId);

    FollowResponse follow(String followerId, String userId);

    FollowResponse unfollow(String followerId, String userId);

    PostResponse createPost(String authorId, String content, String idempotencyKey);

    PostResponse getPost(String postId);
}
