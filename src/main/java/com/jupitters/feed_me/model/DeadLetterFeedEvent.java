package com.jupitters.feed_me.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_feed_events")
@Getter
@NoArgsConstructor
public class DeadLetterFeedEvent {
    @Id
    private String id;

    private String eventId;
    private String streamRecordId;
    private String payloadJson;
    private String failureReason;
    private int attempts;
    private Instant movedToDlqAt;

    public DeadLetterFeedEvent(String eventId, String streamRecordId, String payloadJson, String failureReason, int attempts) {
        this.id = UUID.randomUUID().toString();
        this.eventId = eventId;
        this.streamRecordId = streamRecordId;
        this.payloadJson = payloadJson;
        this.failureReason = failureReason;
        this.attempts = attempts;
    }

    @PrePersist
    void onCreate() {
        this.movedToDlqAt = Instant.now();
    }
}
