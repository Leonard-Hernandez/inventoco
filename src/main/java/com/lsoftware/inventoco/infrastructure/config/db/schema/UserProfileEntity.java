package com.lsoftware.inventoco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileEntity {

    @Id
    private UUID id;

    @Column(name = "authco_sub", nullable = false, unique = true)
    private String authcoSub;

    private String email;

    @Column(length = 160)
    private String name;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "time_zone", nullable = false, length = 60)
    private String timeZone;

    @Column(name = "share_prices_by_default", nullable = false)
    private boolean sharePricesByDefault;

    @Column(nullable = false)
    private boolean onboarded;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
