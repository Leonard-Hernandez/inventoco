package com.lsoftware.inventoco.infrastructure.shared;

import com.lsoftware.inventoco.entity.unit.model.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Turns stored base quantities and per-base-unit prices into what the user reads. */
public final class Quantities {

    private Quantities() {}

    public static BigDecimal in(Unit unit, BigDecimal baseQuantity) {
        return baseQuantity == null ? null : unit.fromBase(baseQuantity);
    }

    /** A price per base unit (per g) expressed per unit (per kg), rounded to 4 decimals. */
    public static BigDecimal pricePer(Unit unit, BigDecimal basePrice) {
        return basePrice == null ? null
                : Unit.plain(basePrice.multiply(unit.factor()).setScale(4, RoundingMode.HALF_UP));
    }
}
