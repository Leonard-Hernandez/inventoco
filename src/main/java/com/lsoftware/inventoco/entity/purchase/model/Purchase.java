package com.lsoftware.inventoco.entity.purchase.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Purchase(
        UUID id,
        UUID ownerId,
        PurchaseStatus status,
        String currency,
        int horizonDays,
        Instant createdAt,
        Instant completedAt,
        List<PurchaseLine> lines
) {

    public Purchase completed(Instant at) {
        return new Purchase(id, ownerId, PurchaseStatus.COMPLETED, currency, horizonDays, createdAt, at, lines);
    }
}
