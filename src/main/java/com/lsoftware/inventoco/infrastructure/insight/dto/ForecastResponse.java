package com.lsoftware.inventoco.infrastructure.insight.dto;

import com.lsoftware.inventoco.entity.item.model.Item;
import com.lsoftware.inventoco.entity.unit.model.Unit;
import com.lsoftware.inventoco.usecase.insight.ForecastUseCase.ItemForecast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * When an item runs out. dailyUse is in the item's unit per day; daysLeft and depletesAt are
 * null when no consumption was recorded.
 */
public record ForecastResponse(UUID itemId, String name, String unit, BigDecimal stock, BigDecimal minStock,
                               boolean lowStock, BigDecimal dailyUse, BigDecimal daysLeft, Instant depletesAt,
                               String confidence, int samples, boolean buySoon) {

    public static ForecastResponse from(ItemForecast f) {
        Item i = f.item();
        Unit u = i.displayUnit();
        return new ForecastResponse(i.id(), i.name(), u.symbol(), u.fromBase(i.currentStock()), u.fromBase(i.minStock()),
                i.lowStock(), Unit.plain(f.forecast().dailyRate().divide(u.factor(), 3, RoundingMode.HALF_UP)),
                f.forecast().daysLeft(), f.forecast().depletesAt(), f.forecast().confidence().name(),
                f.forecast().samples(), f.buySoon());
    }
}
