package com.lsoftware.inventoco.entity.unit.model;

import com.lsoftware.inventoco.entity.exception.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Locale;

/**
 * Units the user can type. Each one is a factor of its dimension's base unit (unit, g, ml, mm, cm²), so
 * 1.5 kg is stored as 1500 and shown again in whatever unit the item prefers.
 */
public enum Unit {
    UNIT(Dimension.COUNT, "1", "unit", "u", "un", "und", "units"),
    DOZEN(Dimension.COUNT, "12", "dozen", "doz", "docena"),
    MG(Dimension.MASS, "0.001", "mg"),
    G(Dimension.MASS, "1", "g", "gr"),
    KG(Dimension.MASS, "1000", "kg", "kilo"),
    LB(Dimension.MASS, "453.59237", "lb", "lbs", "libra"),
    OZ(Dimension.MASS, "28.349523125", "oz"),
    ML(Dimension.VOLUME, "1", "ml", "cc"),
    CL(Dimension.VOLUME, "10", "cl"),
    L(Dimension.VOLUME, "1000", "l", "lt", "litro"),
    GAL(Dimension.VOLUME, "3785.411784", "gal", "galon"),
    FL_OZ(Dimension.VOLUME, "29.5735295625", "fl_oz", "floz", "fl oz"),
    MM(Dimension.LENGTH, "1", "mm"),
    CM(Dimension.LENGTH, "10", "cm"),
    M(Dimension.LENGTH, "1000", "m", "mt", "mts", "metro", "metros"),
    IN(Dimension.LENGTH, "25.4", "in", "pulgada", "pulgadas"),
    FT(Dimension.LENGTH, "304.8", "ft", "pie", "pies"),
    YD(Dimension.LENGTH, "914.4", "yd", "yarda", "yardas"),
    CM2(Dimension.AREA, "1", "cm2", "cm²"),
    M2(Dimension.AREA, "10000", "m2", "m²", "metro cuadrado", "metros cuadrados"),
    FT2(Dimension.AREA, "929.0304", "ft2", "ft²");

    /** Scale of stored quantities (numeric(19,6)). */
    public static final int SCALE = 6;

    private final Dimension dimension;
    private final BigDecimal factor;
    private final String symbol;
    private final String[] aliases;

    Unit(Dimension dimension, String factor, String symbol, String... aliases) {
        this.dimension = dimension;
        this.factor = new BigDecimal(factor);
        this.symbol = symbol;
        this.aliases = aliases;
    }

    public Dimension dimension() {
        return dimension;
    }

    public String symbol() {
        return symbol;
    }

    public BigDecimal factor() {
        return factor;
    }

    public BigDecimal toBase(BigDecimal quantity) {
        return quantity.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal fromBase(BigDecimal baseQuantity) {
        return plain(baseQuantity.divide(factor, SCALE, RoundingMode.HALF_UP));
    }

    /** Without trailing zeros and never in scientific notation (1000, not 1E+3). */
    public static BigDecimal plain(BigDecimal value) {
        BigDecimal v = value.stripTrailingZeros();
        return v.scale() < 0 ? v.setScale(0) : v;
    }

    /** The unit a user typed (symbol, enum name or alias, any case), e.g. "kg", "L", "fl oz". */
    public static Unit parse(String text) {
        if (text == null || text.isBlank()) throw new DomainException("INVALID_UNIT", "Missing unit");
        String t = text.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(u -> u.symbol.equals(t) || u.name().toLowerCase(Locale.ROOT).equals(t)
                        || Arrays.asList(u.aliases).contains(t))
                .findFirst()
                .orElseThrow(() -> new DomainException("INVALID_UNIT", "Unknown unit: " + text));
    }

    /** Parses a unit and checks it measures the given dimension (no grams of a liquid). */
    public static Unit parse(String text, Dimension expected) {
        Unit unit = parse(text);
        if (unit.dimension != expected) {
            throw new DomainException("INCOMPATIBLE_UNIT",
                    unit.symbol + " measures " + unit.dimension.name().toLowerCase(Locale.ROOT)
                            + ", this item is measured in " + expected.name().toLowerCase(Locale.ROOT));
        }
        return unit;
    }
}
