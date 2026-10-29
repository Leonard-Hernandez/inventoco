package com.lsoftware.inventoco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_observation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceObservationEntity {

    @Id
    private UUID id;

    /** Null for system prices (seeded store chains). */
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "catalog_product_id")
    private UUID catalogProductId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "package_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal packageQuantity;

    @Column(name = "package_unit", nullable = false, length = 10)
    private String packageUnit;

    @Column(name = "unit_price", nullable = false, precision = 24, scale = 10)
    private BigDecimal unitPrice;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false, length = 8)
    private String visibility;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "purchase_id")
    private UUID purchaseId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
