package com.jupitters.feed_me.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "post_creation_requests")
@Getter
@Setter
@NoArgsConstructor
public class PostCreationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String idempotencyKey;
    private String requestHash;
}
