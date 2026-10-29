package com.lsoftware.inventoco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogProductEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 200)
    private String normalizedName;

    @Column(length = 120)
    private String brand;

    @Column(length = 14, unique = true)
    private String gtin;

    @Column(nullable = false, length = 10)
    private String dimension;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
