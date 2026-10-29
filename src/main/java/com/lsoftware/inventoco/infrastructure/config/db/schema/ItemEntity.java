package com.lsoftware.inventoco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 80)
    private String category;

    @Column(nullable = false, length = 10)
    private String dimension;

    @Column(name = "display_unit", nullable = false, length = 10)
    private String displayUnit;

    @Column(name = "min_stock", nullable = false, precision = 19, scale = 6)
    private BigDecimal minStock;

    /** Never in an entity UPDATE: only JpaItemRepository.addStock changes it (atomically). */
    @Column(name = "current_stock", nullable = false, updatable = false, precision = 19, scale = 6)
    private BigDecimal currentStock;

    @Column(length = 14)
    private String barcode;

    @Column(name = "catalog_product_id")
    private UUID catalogProductId;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
