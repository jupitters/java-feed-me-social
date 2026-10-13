package com.jupitters.feed_me.repository;

import com.jupitters.feed_me.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
}
