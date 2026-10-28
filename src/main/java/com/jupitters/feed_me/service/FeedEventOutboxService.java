package com.jupitters.feed_me.service;

import com.jupitters.feed_me.model.OutboxEvent;
import com.jupitters.feed_me.model.Post;
import com.jupitters.feed_me.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedEventOutboxService {
    private final OutboxEventRepository outboxEventRepository;

    public void enqueuePostCreated(Post post) {
        outboxEventRepository.save(new OutboxEvent(post));
    }
}
