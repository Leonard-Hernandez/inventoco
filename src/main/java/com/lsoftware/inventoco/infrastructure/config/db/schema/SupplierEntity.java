package com.lsoftware.inventoco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "supplier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierEntity {

    @Id
    private UUID id;

    /** Null for system suppliers (store chains). */
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(length = 120)
    private String city;

    @Column(length = 500)
    private String url;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
