package com.lsoftware.inventoco.usecase.price;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A price an item can be bought at. supplierId is set only when the user can pick that
 * supplier (theirs or a store chain); a public price from another user's supplier has a name only.
 */
public record Offer(UUID supplierId, String supplierName, BigDecimal unitPrice, Instant observedAt, Source source) {

    public enum Source { OWN, PUBLIC }
}
