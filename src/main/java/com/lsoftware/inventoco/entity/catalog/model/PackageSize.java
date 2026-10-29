package com.lsoftware.inventoco.entity.catalog.model;

import com.lsoftware.inventoco.entity.unit.model.Dimension;
import com.lsoftware.inventoco.entity.unit.model.Unit;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The package size written in a store's product name: "Arroz Diana 500 G", "Aceite 3.000 Ml",
 * "Arepa 5 Und Masmaí 400 Grs", "Doria Pague 250 G Lleve 275 G". Weight or volume wins over a
 * count of units, and the last one written wins ("lleve 275 G" is what is in the bag).
 */
public record PackageSize(BigDecimal quantity, Unit unit) {

    private static final Pattern SIZE = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(kgs?|kilos?|grs?|gramos|g|mg|ml|cc|lts?|litros?|l|lbs?|libras?|oz|unds?|unidades|un|u)\\b");

    private static final Map<String, Unit> UNITS = Map.ofEntries(
            Map.entry("kg", Unit.KG), Map.entry("kgs", Unit.KG), Map.entry("kilo", Unit.KG), Map.entry("kilos", Unit.KG),
            Map.entry("g", Unit.G), Map.entry("gr", Unit.G), Map.entry("grs", Unit.G), Map.entry("gramos", Unit.G),
            Map.entry("mg", Unit.MG), Map.entry("ml", Unit.ML), Map.entry("cc", Unit.ML),
            Map.entry("l", Unit.L), Map.entry("lt", Unit.L), Map.entry("lts", Unit.L), Map.entry("litro", Unit.L), Map.entry("litros", Unit.L),
            Map.entry("lb", Unit.LB), Map.entry("lbs", Unit.LB), Map.entry("libra", Unit.LB), Map.entry("libras", Unit.LB),
            Map.entry("oz", Unit.OZ),
            Map.entry("und", Unit.UNIT), Map.entry("unds", Unit.UNIT), Map.entry("unidades", Unit.UNIT), Map.entry("un", Unit.UNIT), Map.entry("u", Unit.UNIT));

    public static Optional<PackageSize> parse(String name) {
        if (name == null) return Optional.empty();
        String text = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        Matcher m = SIZE.matcher(text);
        PackageSize measured = null;
        PackageSize counted = null;
        while (m.find()) {
            BigDecimal quantity = number(m.group(1));
            Unit unit = UNITS.get(m.group(2));
            if (quantity == null || quantity.signum() <= 0 || unit == null) continue;
            PackageSize size = new PackageSize(quantity, unit);
            if (unit.dimension() == Dimension.COUNT) counted = size; else measured = size;
        }
        return Optional.ofNullable(measured != null ? measured : counted);
    }

    /** "3.000" is three thousand (a thousands dot), "1.7" and "20,4" are decimals. */
    static BigDecimal number(String raw) {
        String s = raw.matches("\\d{1,3}\\.\\d{3}") ? raw.replace(".", "") : raw.replace(',', '.');
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
