package com.jupitters.feed_me.dto;

public record FollowResponse(
        String followerId,
        String targetUserId,
        boolean following,
        int totalFollowing
) {
}
