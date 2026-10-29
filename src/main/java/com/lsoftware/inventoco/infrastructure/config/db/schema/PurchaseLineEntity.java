package com.lsoftware.inventoco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchase_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseLineEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private PurchaseEntity purchase;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 12)
    private String reason;

    @Column(name = "suggested_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal suggestedQuantity;

    @Column(name = "supplier_id")
    private UUID supplierId;

    @Column(name = "supplier_name", length = 160)
    private String supplierName;

    @Column(name = "best_unit_price", precision = 24, scale = 10)
    private BigDecimal bestUnitPrice;

    @Column(name = "usual_unit_price", precision = 24, scale = 10)
    private BigDecimal usualUnitPrice;
}
