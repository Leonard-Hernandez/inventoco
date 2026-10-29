package com.lsoftware.inventoco.infrastructure.price.dto;

import com.lsoftware.inventoco.entity.price.model.Visibility;
import com.lsoftware.inventoco.usecase.price.PriceUseCase.PriceCommand;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * amount is what the whole package costs, in the user's currency. packageUnit defaults to the
 * item's unit; visibility to the user's default.
 */
public record PriceRequest(@NotNull UUID supplierId, @NotNull BigDecimal amount, @NotNull BigDecimal packageQuantity,
                           String packageUnit, Instant observedAt, Visibility visibility) {

    public PriceCommand command() {
        return new PriceCommand(supplierId, amount, packageQuantity, packageUnit, observedAt, visibility);
    }
}
