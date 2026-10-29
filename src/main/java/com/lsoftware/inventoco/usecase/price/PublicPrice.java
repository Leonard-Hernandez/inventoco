package com.lsoftware.inventoco.usecase.price;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A public price as other users see it: where, how much and when. It deliberately has no field
 * that identifies who recorded it (no owner, item or purchase).
 */
public record PublicPrice(
        String supplierName,
        String city,
        String country,
        BigDecimal amount,
        String currency,
        BigDecimal packageQuantity,
        String packageUnit,
        BigDecimal unitPrice,
        Instant observedAt,
        /** Seeded from a store chain instead of recorded by a user. */
        boolean storeListing
) {}
