package com.jupitters.feed_me.dto;

import java.util.List;

public record FollowingResponse (
        String followerId,
        List<String> targetUserIds,
        int totalFollowing
) {
}
