package com.jupitters.feed_me.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor
public class OutboxEvent {
    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventType eventType;

    @Column(nullable = false)
    private String postId;

    @Column(nullable = false)
    private String authorId;

    @Column(nullable = false)
    private boolean hotUser;

    @Column(nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    private OutboxEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column(nullable = false)
    private Instant updatedAt;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    public OutboxEvent(Post post) {
        this.id = UUID.randomUUID().toString();
        this.eventType = OutboxEventType.POST_CREATED;
        this.postId = post.getId();
        this.authorId = post.getAuthor().getId();
        this.hotUser = post.getAuthor().isHotUser();
        this.occurredAt = post.getCreatedAt();
        this.status = OutboxEventStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if(nextAttemptAt == null) {
            this.nextAttemptAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public markPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
    }

    public void scheduleRetry(Instant nextAttemptAt, int maxAttempts) {
        this.attemptCount++;
        this.nextAttemptAt = nextAttemptAt;
        if(this.attemptCount >= maxAttempts) {
            this.status = OutboxEventStatus.FAILED;
        }
    }
}
