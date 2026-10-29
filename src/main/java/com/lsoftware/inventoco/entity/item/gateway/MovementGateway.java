package com.lsoftware.inventoco.entity.item.gateway;

import com.lsoftware.inventoco.entity.item.model.StockMovement;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MovementGateway {
    StockMovement save(StockMovement movement);
    /** Newest first. */
    List<StockMovement> listByItem(UUID ownerId, UUID itemId, int limit);
    /** Consumption and waste of every item of the owner since an instant, oldest first. */
    List<StockMovement> outflowsSince(UUID ownerId, Instant since);
}
