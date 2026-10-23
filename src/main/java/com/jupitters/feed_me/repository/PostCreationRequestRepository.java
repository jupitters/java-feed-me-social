package com.jupitters.feed_me.repository;

import com.jupitters.feed_me.model.PostCreationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostCreationRequestRepository extends JpaRepository<PostCreationRequest, Long> {
    Optional<PostCreationRequest> findByUserIdAndIdempotencyKey(String authorId, String idempotencyKey);
}
