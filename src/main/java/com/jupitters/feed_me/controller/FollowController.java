package com.jupitters.feed_me.controller;

import com.jupitters.feed_me.dto.FollowingResponse;
import com.jupitters.feed_me.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {
    private final FeedService feedService;

    @GetMapping
    public FollowingResponse getFollowing(@RequestParam String followerId) {
        return feedService.getFollowing(followerId);
    }
}
