package com.jupitters.feed_me.service.impl;

import com.jupitters.feed_me.dto.FeedItemResponse;
import com.jupitters.feed_me.dto.TimelinePageResponse;
import com.jupitters.feed_me.model.FollowRelation;
import com.jupitters.feed_me.model.UserProfile;
import com.jupitters.feed_me.repository.FollowRelationRepository;
import com.jupitters.feed_me.repository.UserProfileRepository;
import com.jupitters.feed_me.service.FeedCacheService;
import com.jupitters.feed_me.service.FeedCursorCodec;
import com.jupitters.feed_me.service.FeedMetricsService;
import com.jupitters.feed_me.service.FeedService;

import jakarta.transaction.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final FeedMetricsService feedMetricsService;
    private final UserProfileRepository userProfileRepository;
    private final FollowRelationRepository followRelationRepository;

    @Override
    public TimelinePageResponse getHomeFeed(String userId, String cursor, int limit) {
        long startedAtNanos = feedMetricsService.startTimer();
        int pageSize = normalizeLimit(limit);
        feedMetricsService.recordHomeFeedRequestedPageSize(limit, pageSize);

        try {
            UserProfile viewer = getUser(userId);
            FeedCursorCodec.FeedCursor pageCursor = FeedCursorCodec.parse(cursor);

        } catch (ResponseStatusException e) {
            feedMetricsService.recordServiceError("get_home_feed", e.getStatusCode().toString());
            throw e;
        }

        return null;
    }

    private VisibleAuthors getVisibleAuthors(UserProfile viewer) {
        List<FollowRelation> followRelations = followRelationRepository.findByFollower_Id(viewer.getId());

        Set<String> allAuthorIds = new LinkedHashSet<>();
        Set<String> hotAuthorIds = new LinkedHashSet<>();
        Set<String> nonHotAuthorIds = new LinkedHashSet<>();

        allAuthorIds.add(viewer.getId());

        if(viewer.isHotUser()) {
            hotAuthorIds.add(viewer.getId());
        } else {
            nonHotAuthorIds.add(viewer.getId());
        }

        for(FollowRelation relation : followRelations) {
            UserProfile author = relation.getTargetUser();
            allAuthorIds.add(author.getId());
            if(viewer.isHotUser()) {
                hotAuthorIds.add(viewer.getId());
            } else {
                nonHotAuthorIds.add(viewer.getId());
            }
        }

        return new VisibleAuthors(allAuthorIds, hotAuthorIds, nonHotAuthorIds);
    }

    private UserProfile getUser(String userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return FeedCacheService.DEFAULT_PAGE_SIZE;
        }
        return Math.min(limit, 20);
    }

    private record VisibleAuthors(Set<String> allAuthorIds, Set<String> hotAuthorIds, Set<String> nonHotAuthorIds) {
    }

    private record NormalFeedSliceResult(FeedSlice slice, String cacheOutcome) {}

    private record FeedSlice(List<FeedItemResponse> items, boolean hasMore) {}
}
