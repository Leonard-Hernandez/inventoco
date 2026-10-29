package com.lsoftware.inventoco.infrastructure.purchase.dto;

import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.purchase.model.Purchase;
import com.lsoftware.inventoco.entity.purchase.model.PurchaseLine;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import com.lsoftware.inventoco.infrastructure.shared.Quantities;
import com.lsoftware.inventoco.usecase.shopping.ShoppingListUseCase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A shopping list (DRAFT) or a past purchase. Line quantities and prices are in the item's unit;
 * estimatedTotal uses the best known price of each line, estimatedSavings compares it with the usual one.
 */
public record PurchaseResponse(UUID id, String status, String currency, int horizonDays, Instant createdAt,
                               Instant completedAt, List<Line> lines, BigDecimal estimatedTotal,
                               BigDecimal estimatedSavings) {

    public record Line(UUID itemId, String itemName, String unit, String reason, BigDecimal suggestedQuantity,
                       UUID supplierId, String supplierName, BigDecimal bestUnitPrice, BigDecimal usualUnitPrice,
                       BigDecimal estimatedCost, BigDecimal estimatedSavings) {}

    public static PurchaseResponse from(Purchase p, Map<UUID, Item> items) {
        List<Line> lines = p.lines().stream().map(l -> line(l, items.get(l.itemId()))).toList();
        BigDecimal total = p.lines().stream().map(ShoppingListUseCase::estimatedCost).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal savings = p.lines().stream().map(ShoppingListUseCase::estimatedSavings)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PurchaseResponse(p.id(), p.status().name(), p.currency(), p.horizonDays(), p.createdAt(),
                p.completedAt(), lines, total, savings);
    }

    private static Line line(PurchaseLine l, Item item) {
        Unit unit = item == null ? Unit.UNIT : item.displayUnit();
        return new Line(l.itemId(), item == null ? "?" : item.name(), unit.symbol(), l.reason().name(),
                unit.fromBase(l.suggestedQuantity()), l.supplierId(), l.supplierName(),
                Quantities.pricePer(unit, l.bestUnitPrice()), Quantities.pricePer(unit, l.usualUnitPrice()),
                ShoppingListUseCase.estimatedCost(l), ShoppingListUseCase.estimatedSavings(l));
    }
}
