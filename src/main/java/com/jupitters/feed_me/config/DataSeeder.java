package com.jupitters.feed_me.config;

import com.jupitters.feed_me.model.FollowRelation;
import com.jupitters.feed_me.model.Post;
import com.jupitters.feed_me.model.UserProfile;
import com.jupitters.feed_me.repository.FollowRelationRepository;
import com.jupitters.feed_me.repository.PostRepository;
import com.jupitters.feed_me.repository.UserProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seedFeedMeData(UserProfileRepository userProfileRepository,
                                     FollowRelationRepository followRelationRepository,
                                     PostRepository postRepository) {
        return ignored -> {
            if(userProfileRepository.count() > 0L) {
                return;
            }

            UserProfile ana = new UserProfile("u1", "ana", "Ana Dev", "Developer", false);
            UserProfile bruno = new UserProfile("u2", "bruno", "Bruno Debe", "Hot user creator", true);
            UserProfile carlos = new UserProfile("u3", "carlos", "Carlos Marketing", "Marks", false);
            UserProfile daniela = new UserProfile("u4", "daniela", "Daniela Reaga", "Im very good at my job", true);

            List<UserProfile> users = List.of(ana, bruno, carlos, daniela);

            userProfileRepository.saveAll(users);

            followRelationRepository.saveAll(List.of(
                    new FollowRelation(ana, bruno),
                    new FollowRelation(bruno, ana),
                    new FollowRelation(bruno, carlos),
                    new FollowRelation(ana, carlos),
                    new FollowRelation(daniela, ana),
                    new FollowRelation(daniela, bruno),
                    new FollowRelation(daniela, carlos)
            ));

            postRepository.saveAll(Stream.of(
                    new Post(UUID.randomUUID().toString(), bruno, "Hi i'm bruno and im hoooooooooot!"),
                    new Post(UUID.randomUUID().toString(), ana, "Hi i'm ana and im not hot!"),
                    new Post(UUID.randomUUID().toString(), daniela, "Hi i'm dani and im also hot!"),
                    new Post(UUID.randomUUID().toString(), carlos, "Hi i'm tired and im also carlos!"),
                    new Post(UUID.randomUUID().toString(), carlos, "I will be hot")
            ).toList());
        };
    }
}
