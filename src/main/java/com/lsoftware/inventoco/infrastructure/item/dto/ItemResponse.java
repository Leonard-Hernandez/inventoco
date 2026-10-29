package com.lsoftware.inventoco.infrastructure.item.dto;

import com.lsoftware.inventoco.entity.item.model.Item;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.lsoftware.inventoco.infrastructure.shared.Quantities.in;

/** Quantities in the item's unit. */
public record ItemResponse(UUID id, String name, String category, String dimension, String unit, BigDecimal stock,
                           BigDecimal minStock, boolean lowStock, String barcode, UUID catalogProductId,
                           Instant updatedAt) {

    public static ItemResponse from(Item i) {
        return new ItemResponse(i.id(), i.name(), i.category(), i.dimension().name(), i.displayUnit().symbol(),
                in(i.displayUnit(), i.currentStock()), in(i.displayUnit(), i.minStock()), i.lowStock(), i.barcode(),
                i.catalogProductId(), i.updatedAt());
    }
}
