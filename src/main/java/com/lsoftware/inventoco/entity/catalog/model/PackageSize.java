package com.lsoftware.inventoco.entity.catalog.model;

import com.lsoftware.inventoco.entity.unit.model.Dimension;
import com.lsoftware.inventoco.entity.unit.model.Unit;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The package size written in a store's product name: "Arroz Diana 500 G", "Aceite 3.000 Ml",
 * "Arepa 5 Und Masmaí 400 Grs", "Doria Pague 250 G Lleve 275 G", "Tela lino 1,5 m". A weight or volume wins
 * over a count, and the last one written wins ("lleve 275 G" is what is in the bag). A count wins over a length
 * or area, which on disposables and bags is the size of each one ("Platos 23 cm x 20 Und"), an area over a
 * length, and of several lengths the longest is the roll ("Papel Aluminio 7 m x 30 cm"). A bare "x 30" closing
 * the name is a count, but only when nothing else was found.
 */
public record PackageSize(BigDecimal quantity, Unit unit) {

    private static final Pattern SIZE = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(kgs?|kilos?|grs?|gramos|g|mg|ml|cc|lts?|litros?|l|lbs?|libras?|oz|unds?|unidades|un|u"
                    + "|m2|m²|mts?2|cm2|cm²|metros cuadrados|metro cuadrado|mts?|metros?|mm|cm|m|yardas?|yd)(?![a-z0-9²³])");

    /** "Pañal Etapa 3 M x 30": a closing "x 30" with no unit is a count. */
    private static final Pattern TRAILING_COUNT = Pattern.compile("\\bx\\s*(\\d+)\\s*$");

    /** A number that is not what is in the package: a fabric's width ("ancho 1,5 m") or a diaper stage. */
    private static final Pattern NOT_A_SIZE = Pattern.compile("\\b(?:ancho|width|etapa)\\s*(?:de\\s*)?:?\\s*$");

    /** Store spellings {@link Unit#find} does not know. */
    private static final Map<String, Unit> STORE_UNITS = Map.ofEntries(
            Map.entry("kgs", Unit.KG), Map.entry("kilos", Unit.KG), Map.entry("grs", Unit.G), Map.entry("gramos", Unit.G),
            Map.entry("lts", Unit.L), Map.entry("litros", Unit.L), Map.entry("libras", Unit.LB),
            Map.entry("unds", Unit.UNIT), Map.entry("unidades", Unit.UNIT), Map.entry("mt2", Unit.M2), Map.entry("mts2", Unit.M2));

    public static Optional<PackageSize> parse(String name) {
        if (name == null) return Optional.empty();
        String text = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        Matcher m = SIZE.matcher(text);
        PackageSize weighed = null;
        PackageSize counted = null;
        PackageSize area = null;
        PackageSize length = null;
        while (m.find()) {
            if (NOT_A_SIZE.matcher(text.substring(0, m.start())).find()) continue;
            BigDecimal quantity = number(m.group(1));
            Unit unit = unit(m.group(2));
            if (quantity == null || quantity.signum() <= 0 || unit == null) continue;
            PackageSize size = new PackageSize(quantity, unit);
            switch (unit.dimension()) {
                case COUNT -> counted = size;
                case AREA -> area = size;
                case LENGTH -> {
                    if (length == null || size.base().compareTo(length.base()) > 0) length = size;
                }
                default -> weighed = size;
            }
        }
        return Stream.of(weighed, counted, area, length, trailingCount(text)).filter(Objects::nonNull).findFirst();
    }

    private BigDecimal base() {
        return unit.toBase(quantity);
    }

    private static Unit unit(String written) {
        Unit unit = STORE_UNITS.get(written);
        return unit != null ? unit : Unit.find(written).orElse(null);
    }

    private static PackageSize trailingCount(String text) {
        Matcher m = TRAILING_COUNT.matcher(text);
        if (!m.find()) return null;
        BigDecimal quantity = number(m.group(1));
        return quantity == null || quantity.signum() <= 0 ? null : new PackageSize(quantity, Unit.UNIT);
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
