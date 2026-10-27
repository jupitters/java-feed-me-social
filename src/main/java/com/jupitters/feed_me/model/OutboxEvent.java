package com.jupitters.feed_me.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

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
    private String hotUser;

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
    
    
}
