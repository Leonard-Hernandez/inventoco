package com.lsoftware.inventoco.entity.price.model;

import com.lsoftware.inventoco.entity.unit.model.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * A price seen or paid: amount for a package (e.g. 12 900 COP for 2.5 kg) at a supplier.
 * unitPrice is per base unit (per g, ml or unit) so packages of any size compare directly.
 * ownerId and itemId are null for system prices (seeded store chains).
 */
public record PriceObservation(
        UUID id,
        UUID ownerId,
        UUID itemId,
        UUID supplierId,
        UUID catalogProductId,
        BigDecimal amount,
        String currency,
        /** In the base unit. */
        BigDecimal packageQuantity,
        /** How the package was described (2.5 kg), to show it the same way. */
        Unit packageUnit,
        BigDecimal unitPrice,
        String country,
        Visibility visibility,
        Instant observedAt,
        UUID purchaseId,
        Instant createdAt
) {

    public static final int UNIT_PRICE_SCALE = 10;

    public static BigDecimal unitPrice(BigDecimal amount, BigDecimal baseQuantity) {
        return amount.divide(baseQuantity, UNIT_PRICE_SCALE, RoundingMode.HALF_UP);
    }
}
