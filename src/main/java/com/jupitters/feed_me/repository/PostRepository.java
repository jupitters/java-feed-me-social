package com.jupitters.feed_me.repository;

import com.jupitters.feed_me.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, String> {
    @EntityGraph(attributePaths = "author")
    List<Post> findByAuthor_IdInOrderByCreatedAtDescIdDesc(Collection<String> authorIds, Pageable pageable);
}
