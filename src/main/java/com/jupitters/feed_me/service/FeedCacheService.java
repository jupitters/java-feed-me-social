package com.jupitters.feed_me.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedCacheService {
    public static final int DEFAULT_PAGE_SIZE = 5;
}
