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
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final FeedMetricsService feedMetricsService;
    private final UserProfileRepository userProfileRepository;
    private final FollowRelationRepository followRelationRepository;
    private final FeedCacheService feedCacheService;

    @Override
    public TimelinePageResponse getHomeFeed(String userId, String cursor, int limit) {
        long startedAtNanos = feedMetricsService.startTimer();
        int pageSize = normalizeLimit(limit);
        feedMetricsService.recordHomeFeedRequestedPageSize(limit, pageSize);

        try {
            UserProfile viewer = getUser(userId);
            FeedCursorCodec.FeedCursor pageCursor = FeedCursorCodec.parse(cursor);
            VisibleAuthors visibleAuthors = getVisibleAuthors(viewer);

            NormalFeedSliceResult normalFeedSliceResult = getNormalHomeFeedSlice(viewer.getId(), visibleAuthors.nonHotAuthorIds(), pageCursor, pageSize);
            FeedSlice hotSlice = getHotHomeFeedSlice(viewer.getId(), visibleAuthors, pageCursor, pageSize);

        } catch (ResponseStatusException e) {
            feedMetricsService.recordServiceError("get_home_feed", e.getStatusCode().toString());
            throw e;
        }

        return null;
    }

    private NormalFeedSliceResult getNormalHomeFeedSlice(String userId, Set<String> nonHotAuthorIds, FeedCursorCodec.FeedCursor pageCursor, int pageSize) {
        if(nonHotAuthorIds.isEmpty()) {
            FeedSlice slice = new FeedSlice(List.of(), false);
            return new NormalFeedSliceResult(slice, "empty_non_hot");
        }

        Optional<FeedSlice> cachedSlice = getCachedNormalHomeFeedSlice(userId, pageCursor, pageSize);
    }

    private Optional<FeedSlice> getCachedNormalHomeFeedSlice(String userId, FeedCursorCodec.FeedCursor cursor, int pageSize) {
        if(cursor != null) {
            feedMetricsService.recordHomeFeedCacheLookup("bypass_cursor");
            return Optional.empty();
        }
        if(pageSize > FeedCacheService.DEFAULT_PAGE_SIZE) {
            feedMetricsService.recordHomeFeedCacheLookup("bypass_page_size");
            return Optional.empty();
        }

        Optional<TimelinePageResponse> cachedPage = feedCacheService.getHomeFeed(userId);

        if(cachedPage.isEmpty()) {
            feedMetricsService.recordHomeFeedCacheLookup("miss");
            return Optional.empty();
        }

        Optional<FeedSlice> adapted = adaptCachedFirstPage(cachedPage.get(), pageSize);
        feedMetricsService.recordHomeFeedCacheLookup(adapted.isPresent() ? "hit" : "imcomplete");
        return adapted;
    }

    private Optional<FeedSlice> adaptCachedFirstPage(TimelinePageResponse cachedPage, int pageSize) {
        if(cachedPage.items().size() < pageSize && cachedPage.nextCursor() != null) {
            return Optional.empty();
        }
        List<FeedItemResponse> pageItems = cachedPage.items().stream().limit(pageSize).toList();
        FeedSlice slice = new FeedSlice(pageItems, cachedPage.totalItems() > pageItems.size());

        return Optional.of(slice);
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
