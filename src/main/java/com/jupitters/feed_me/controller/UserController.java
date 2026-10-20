package com.jupitters.feed_me.controller;

import com.jupitters.feed_me.dto.UserProfileResponse;
import com.jupitters.feed_me.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final FeedService feedService;

    @GetMapping
    public List<UserProfileResponse> getUsers() {
        return feedService.getUsers();
    }
}
