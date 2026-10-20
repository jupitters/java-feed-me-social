package com.jupitters.feed_me.service.impl;

import com.jupitters.feed_me.dto.FeedItemResponse;
import com.jupitters.feed_me.dto.TimelineMode;
import com.jupitters.feed_me.dto.TimelinePageResponse;
import com.jupitters.feed_me.dto.UserProfileResponse;
import com.jupitters.feed_me.model.FollowRelation;
import com.jupitters.feed_me.model.Post;
import com.jupitters.feed_me.model.UserProfile;
import com.jupitters.feed_me.repository.FollowRelationRepository;
import com.jupitters.feed_me.repository.PostRepository;
import com.jupitters.feed_me.repository.UserProfileRepository;
import com.jupitters.feed_me.service.FeedCacheService;
import com.jupitters.feed_me.service.FeedCursorCodec;
import com.jupitters.feed_me.service.FeedMetricsService;
import com.jupitters.feed_me.service.FeedService;

import jakarta.transaction.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private static final Comparator<FeedItemResponse> FEED_ORDER =
            Comparator.comparing(FeedItemResponse::createdAt)
                    .thenComparing(FeedItemResponse::postId)
                    .reversed();

    private final FeedMetricsService feedMetricsService;
    private final UserProfileRepository userProfileRepository;
    private final FollowRelationRepository followRelationRepository;
    private final FeedCacheService feedCacheService;
    private final PostRepository postRepository;

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
            FeedSlice hotSlice = getHotHomeFeedSlice(viewer.getId(), visibleAuthors.hotAuthorIds, pageCursor, pageSize);

            int totalItems = Math.toIntExact(postRepository.countByAuthor_IdIn(visibleAuthors.allAuthorIds));

            TimelinePageResponse response = mergeHomeFeedSlices(userId, totalItems, pageSize, normalFeedSliceResult.slice, hotSlice);

            feedMetricsService.recordHomeFeedRequest(startedAtNanos, normalFeedSliceResult.cacheOutcome,
                    determineMergeMode(normalFeedSliceResult.slice, hotSlice),
                    response.nextCursor() != null,
                    response.items().size());

            return response;
        } catch (ResponseStatusException e) {
            feedMetricsService.recordServiceError("get_home_feed", e.getStatusCode().toString());
            throw e;
        }
    }

    @Override
    public TimelinePageResponse getUserFeed(String userId, String cursor, int limit) {
        long startedAtNanos = feedMetricsService.startTimer();
        int pageSize = normalizeLimit(limit);

        feedMetricsService.recordUserFeedRequestedPageSize(limit, pageSize);

        try{
            getUser(userId);

            FeedCursorCodec.FeedCursor pageCursor = FeedCursorCodec.parse(cursor);
            List<Post> posts = fetchUserFeedPosts(userId, pageCursor, pageSize + 1);
            int totalItems = Math.toIntExact(postRepository.countByAuthor_Id(userId));
            TimelinePageResponse response = buildTimelinePage(userId, TimelineMode.USER, posts, totalItems, pageSize);
            
            feedMetricsService.recordUserFeedRequest(startedAtNanos, response.nextCursor() != null, response.items().size());

            return response;
        }catch(ResponseStatusException e) {
            feedMetricsService.recordServiceError(
                    "get_user_feed",
                    e.getStatusCode().toString()
            );
            throw e;
        }
    }

    @Override
    public List<UserProfileResponse> getUsers() {
        return userProfileRepository.findAll(Sort.by("id")).stream()
                .map(this::toUserProfileResponse)
                .toList();
    }

    private UserProfileResponse toUserProfileResponse(UserProfile user) {
        return new UserProfileResponse(
                user.getId(),
                user.getHandle(),
                user.getName(),
                user.getBio(),
                user.isHotUser()
        );
    }

    private TimelinePageResponse buildTimelinePage(String timelineOwnerId, TimelineMode mode, List<Post> posts, int totalItems, int pageSize) {
        boolean hasMore = posts.size() > pageSize;
        List<Post> pagePosts = hasMore ? posts.subList(0, pageSize) : posts;

        List<FeedItemResponse> pageItems = pagePosts.stream()
                .map(post -> toFeedItem(post, timelineOwnerId, Set.of(timelineOwnerId)))
                .toList();

        String nextCursor = hasMore && !pageItems.isEmpty()
                ? FeedCursorCodec.encode(pageItems.get(pageItems.size() - 1))
                : null;

        return new TimelinePageResponse(timelineOwnerId, mode, totalItems, pageItems, nextCursor);
    }

    private List<Post> fetchUserFeedPosts(String userId, FeedCursorCodec.FeedCursor pageCursor, int fetchSize) {
        PageRequest pageRequest = PageRequest.of(0, fetchSize);
        if(pageCursor == null) {
            return postRepository.findByAuthor_IdOrderByCreatedAtDescIdDesc(userId, pageRequest);
        }

        return postRepository.findUserFeedPageAfterCursor(userId, pageCursor.createdAt(), pageCursor.postId(), pageRequest);
    }

    private TimelinePageResponse mergeHomeFeedSlices(String userId, int totalItems, int pageSize, FeedSlice normalSlice, FeedSlice hotSlice) {
        List<FeedItemResponse> mergedItems = new ArrayList<>(pageSize + 1);

        int normalIndex = 0;
        int hotIndex = 0;
        int normalItemsUsed = 0;
        int hotItemsUsed = 0;

        while (mergedItems.size() < pageSize + 1 &&
                (normalIndex < normalSlice.items().size() || hotIndex < hotSlice.items().size())) {
            FeedItemResponse nextNormal = normalIndex < normalSlice.items().size() ? normalSlice.items().get(normalIndex) : null;
            FeedItemResponse nextHot = hotIndex < hotSlice.items().size() ? hotSlice.items().get(hotIndex) : null;

            if(nextHot == null || (nextNormal != null && FEED_ORDER.compare(nextNormal, nextHot) <= 0)) {
                mergedItems.add(nextNormal);
                normalIndex++;
                normalItemsUsed++;
            } else {
                mergedItems.add(nextHot);
                hotIndex++;
                hotItemsUsed++;
            }
        }

        boolean hasMore = mergedItems.size() > pageSize
                || normalIndex < normalSlice.items().size()
                || hotIndex < hotSlice.items().size()
                || normalSlice.hasMore()
                || hotSlice.hasMore();

        List<FeedItemResponse> pageItems = mergedItems.size() > pageSize ? mergedItems.subList(0, pageSize) : mergedItems;

        String nextCursor = hasMore && !pageItems.isEmpty() ? FeedCursorCodec.encode(pageItems.get(pageItems.size() - 1)) : null;

        feedMetricsService.recordHomeFeedMerge(determineMergeMode(normalSlice, hotSlice), normalItemsUsed, hotItemsUsed);

        return new TimelinePageResponse(
                userId,
                TimelineMode.HOME,
                totalItems,
                pageItems,
                nextCursor
        );
    }

    private String determineMergeMode(FeedSlice normalSlice, FeedSlice hotSlice) {
        boolean hasNormal = !normalSlice.items().isEmpty();
        boolean hasHot = !hotSlice.items.isEmpty();

        if(hasNormal && hasHot) {
            return "mixed";
        }

        if(hasNormal) {
            return "normal_only";
        }

        if(hasHot) {
            return "hot_only";
        }

        return "empty";
    }

    private FeedSlice getHotHomeFeedSlice(String userId, Set<String> hotAuthorIds, FeedCursorCodec.FeedCursor pageCursor, int pageSize) {
        if(hotAuthorIds.isEmpty()) {
            return new FeedSlice(List.of(), false);
        }

        List<Post> posts = fetchHomeFeedPosts(hotAuthorIds, pageCursor, pageSize + 1);

        return buildFeedSlice(posts, pageSize, userId, hotAuthorIds);
    }

    private NormalFeedSliceResult getNormalHomeFeedSlice(String userId, Set<String> nonHotAuthorIds, FeedCursorCodec.FeedCursor pageCursor, int pageSize) {
        if(nonHotAuthorIds.isEmpty()) {
            FeedSlice slice = new FeedSlice(List.of(), false);
            return new NormalFeedSliceResult(slice, "empty_non_hot");
        }

        Optional<FeedSlice> cachedSlice = getCachedNormalHomeFeedSlice(userId, pageCursor, pageSize);

        if(cachedSlice.isPresent()) {
            return new NormalFeedSliceResult(cachedSlice.get(), "hit");
        }

        List<Post> posts = fetchHomeFeedPosts(nonHotAuthorIds, pageCursor, pageSize + 1);

        FeedSlice slice = buildFeedSlice(posts, pageSize, userId, nonHotAuthorIds);

        if(pageCursor == null && pageSize == FeedCacheService.DEFAULT_PAGE_SIZE) {
            TimelinePageResponse tpResponse = new TimelinePageResponse(
                    userId,
                    TimelineMode.HOME,
                    Math.toIntExact(postRepository.countByAuthor_IdIn(nonHotAuthorIds)),
                    slice.items,
                    slice.hasMore() && !slice.items.isEmpty()
                    ? FeedCursorCodec.encode(
                            slice.items().get(slice.items().size() - 1)
                    ) : null
            );

            feedCacheService.cacheHomeFeed(tpResponse);
        }
        return new NormalFeedSliceResult(slice, "miss");
    }

    private FeedSlice buildFeedSlice(List<Post> posts, int pageSize, String userId, Set<String> visibleAuthorIds) {
        boolean hasMore = posts.size() > pageSize;
        List<Post> pagePosts = hasMore ? posts.subList(0, pageSize) : posts;

        List<FeedItemResponse> pageItems = pagePosts.stream()
                .map(post -> toFeedItem(post, userId, visibleAuthorIds))
                .toList();

        return new FeedSlice(pageItems, hasMore);
    }

    private FeedItemResponse toFeedItem(Post post, String viewerId, Set<String> visibleAuthorIds) {
        UserProfile author = post.getAuthor();

        double freshnessWindow = 600.0;
        double recencyScore = 1.0 - Duration.between(post.getCreatedAt(), Instant.now()).toMinutes() / freshnessWindow;

        double hotUserPenalty = author.isHotUser() ? 0.15 : 0.0;

        double affinityBoost = visibleAuthorIds.contains(author.getId()) && viewerId.equals(author.getId()) ? 0.0 : 0.2;

        double rankingScore = Math.round((recencyScore + affinityBoost - hotUserPenalty) * 100.0) / 100.0;

        String deliveryStrategy = author.isHotUser() ? "hybrid-pull" : "fan-out-write";

        String rankingReason = author.isHotUser() ? "Hot-user content is blended with a pull-based to reduce write amplifications" : "Recent content on followed accounts is promoted for freshness and affinity";

        return new FeedItemResponse(
                post.getId(),
                author.getId(),
                author.getHandle(),
                author.getName(),
                post.getContent(),
                post.getCreatedAt(),
                rankingScore,
                deliveryStrategy,
                rankingReason
        );
    }

    private List<Post> fetchHomeFeedPosts(Set<String> authorIds, FeedCursorCodec.FeedCursor pageCursor, int fetchSize) {
        PageRequest pageRequest = PageRequest.of(0, fetchSize);
        if (pageCursor == null) {
            return postRepository.findByAuthor_IdInOrderByCreatedAtDescIdDesc(authorIds, pageRequest);
        }

        return postRepository.findHomeFeedPageAfterCursor(authorIds, pageCursor.createdAt(), pageCursor.postId(), pageRequest);
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
