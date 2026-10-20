package com.jupitters.feed_me.controller;

import com.jupitters.feed_me.dto.FollowResponse;
import com.jupitters.feed_me.dto.FollowingResponse;
import com.jupitters.feed_me.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {
    private final FeedService feedService;

    @GetMapping
    public FollowingResponse getFollowing(@RequestParam String followerId) {
        return feedService.getFollowing(followerId);
    }

    @PostMapping
    public FollowResponse follow(@RequestParam String followerId, @PathVariable String userId) {
        return feedService.follow(followerId, userId);
    }

    @DeleteMapping
    public FollowResponse unfollow(@RequestParam String followerId, @PathVariable String userId) {
        return feedService.unfollow(followerId, userId);
    }
}
