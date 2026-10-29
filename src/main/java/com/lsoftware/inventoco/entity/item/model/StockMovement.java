package com.lsoftware.inventoco.entity.item.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A change in stock. quantity is signed and in the base unit (negative for outflows). */
public record StockMovement(
        UUID id,
        UUID ownerId,
        UUID itemId,
        MovementType type,
        BigDecimal quantity,
        Instant occurredAt,
        String note,
        UUID purchaseId,
        Instant createdAt
) {}
