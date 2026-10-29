package com.lsoftware.inventoco.infrastructure.purchase.dto;

import com.lsoftware.inventoco.entity.price.model.Visibility;
import com.lsoftware.inventoco.usecase.shopping.ShoppingListUseCase.BoughtLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What was bought. Per line: quantity in unit (the item's when null; 0 = skipped) and, with a
 * supplier, the total paid (also recorded as a price).
 */
public record BoughtRequest(@NotEmpty List<@Valid Line> lines, Instant occurredAt) {

    public record Line(@NotNull UUID itemId, @NotNull BigDecimal quantity, String unit, UUID supplierId,
                       BigDecimal amount, Visibility visibility) {}

    public List<BoughtLine> bought() {
        return lines.stream().map(l -> new BoughtLine(l.itemId(), l.quantity(), l.unit(), l.supplierId(), l.amount(),
                l.visibility())).toList();
    }
}
