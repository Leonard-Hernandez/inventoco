package com.lsoftware.inventoco.entity.purchase.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One suggested item of a shopping list. Quantities are in the base unit; prices per base unit.
 * supplierId is the cheapest supplier the user can pick, supplierName the cheapest overall.
 */
public record PurchaseLine(
        UUID id,
        UUID itemId,
        int position,
        LineReason reason,
        BigDecimal suggestedQuantity,
        UUID supplierId,
        String supplierName,
        BigDecimal bestUnitPrice,
        BigDecimal usualUnitPrice
) {}
