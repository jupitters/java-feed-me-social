package com.jupitters.feed_me.repository;

import com.jupitters.feed_me.model.FollowRelation;
import com.jupitters.feed_me.model.FollowRelationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRelationRepository extends JpaRepository<FollowRelation, FollowRelationId> {
    List<FollowRelation> findByFollower_Id(String followerId);
    List<FollowRelation> findByTargetUser_Id(String targetUserId);
    long countByFollower_Id(String followerId);
}
