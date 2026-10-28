package com.jupitters.feed_me.repository;

import com.jupitters.feed_me.model.OutboxEvent;
import com.jupitters.feed_me.model.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
        OutboxEventStatus status,
        Instant nextAttemprAt,
        Pageable pageable
    );
}
