package com.jupitters.feed_me.dto;

public record UserProfileResponse(
        String id,
        String handle,
        String name,
        String bio,
        boolean isHot
) {
}
