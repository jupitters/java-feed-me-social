package com.jupitters.feed_me.repository;

import com.jupitters.feed_me.model.ProcessedFeedEvents;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedFeedEventRepository extends JpaRepository<ProcessedFeedEvents, String> {
}
