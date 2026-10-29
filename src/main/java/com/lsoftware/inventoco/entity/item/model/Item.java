package com.lsoftware.inventoco.entity.item.model;

import com.lsoftware.inventoco.entity.unit.model.Dimension;
import com.lsoftware.inventoco.entity.unit.model.Unit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Something a user keeps in stock. minStock and currentStock are in the base unit of the
 * dimension; displayUnit is only how the user likes to see them.
 */
public record Item(
        UUID id,
        UUID ownerId,
        String name,
        String category,
        Dimension dimension,
        Unit displayUnit,
        BigDecimal minStock,
        BigDecimal currentStock,
        String barcode,
        UUID catalogProductId,
        boolean archived,
        Instant createdAt,
        Instant updatedAt
) {

    public boolean lowStock() {
        return minStock.signum() > 0 && currentStock.compareTo(minStock) <= 0;
    }

    public Item withDetails(String name, String category, Unit displayUnit, BigDecimal minStock, String barcode,
                            UUID catalogProductId, Instant now) {
        return new Item(id, ownerId, name, category, dimension, displayUnit, minStock, currentStock, barcode,
                catalogProductId, archived, createdAt, now);
    }

    public Item archivedAt(Instant now) {
        return new Item(id, ownerId, name, category, dimension, displayUnit, minStock, currentStock, barcode,
                catalogProductId, true, createdAt, now);
    }
}
