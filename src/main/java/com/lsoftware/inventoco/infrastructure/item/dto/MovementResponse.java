package com.lsoftware.inventoco.infrastructure.item.dto;

import com.lsoftware.inventoco.entity.item.model.StockMovement;
import com.lsoftware.inventoco.entity.unit.model.Unit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** quantity is signed (negative for outflows) and in the item's unit. */
public record MovementResponse(UUID id, String type, BigDecimal quantity, String unit, Instant occurredAt,
                               String note, UUID purchaseId) {

    public static MovementResponse from(StockMovement m, Unit unit) {
        return new MovementResponse(m.id(), m.type().name(), unit.fromBase(m.quantity()), unit.symbol(),
                m.occurredAt(), m.note(), m.purchaseId());
    }
}
