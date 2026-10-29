package com.jupitters.feed_me.repository;

import com.jupitters.feed_me.model.FeedEventFailure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedEventFailureRepository extends JpaRepository<FeedEventFailure, String> {
}
