package com.lsoftware.inventoco.infrastructure.item.dto;

import com.lsoftware.inventoco.entity.item.model.MovementType;
import com.lsoftware.inventoco.usecase.item.RecordMovementUseCase.MovementCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/** unit defaults to the item's; for ADJUSTMENT quantity is the counted stock. */
public record MovementRequest(@NotNull MovementType type, @NotNull BigDecimal quantity, String unit,
                              Instant occurredAt, @Size(max = 500) String note) {

    public MovementCommand command() {
        return new MovementCommand(type, quantity, unit, occurredAt, note);
    }
}
