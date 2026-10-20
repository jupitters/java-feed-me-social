package com.jupitters.feed_me.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank String idempotencyKey,
        @NotBlank String authorId,
        @NotBlank @Size(max = 200) String content
) {
}
